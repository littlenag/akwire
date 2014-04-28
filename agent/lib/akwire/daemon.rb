require File.join(File.dirname(__FILE__), 'base')

module Akwire
  class Daemon
    include Utilities

    attr_accessor :safe_mode

    def self.run(options={})
      daemon = self.new(options)
      EM::run do
        daemon.start
        daemon.trap_signals
      end
    end

    def initialize(options={})
      base = Base.new(options)
      @logger = base.logger
      @settings = base.settings
      @collectors = base.collectors(@settings)
      base.setup_process

      @session_worker = nil
      @lastping = nil

      @timers = Array.new
      @checks_in_progress = Array.new
      @safe_mode = @settings[:daemon][:safe_mode] || false
    end

    def setup_rabbitmq
      @logger.debug('connecting to rabbitmq', {
        :settings => @settings[:rabbitmq]
      })
      @rabbitmq = RabbitMQ.connect(@settings[:rabbitmq])
      @rabbitmq.on_error do |error|
        @logger.fatal('rabbitmq connection error', {
          :error => error.to_s
        })
        stop
      end

      @rabbitmq.before_reconnect do
        @logger.warn('reconnecting to rabbitmq')
      end

      @rabbitmq.after_reconnect do
        @logger.info('reconnected to rabbitmq')
        # re-register here if managed
      end

      @amq = @rabbitmq.channel
    end

    def broadcast_hello
      payload = {
        :version => 1,
        :timestamp => Time.now.to_i,
        :agent_id => @settings[:daemon][:id],
        :hello => true
      }

      @logger.debug('broadcasting hello', {
        :payload => payload
      })

      begin
        @amq.direct('akwire.agent.to.hub').publish(Oj.dump(payload))
      rescue AMQ::Client::ConnectionClosedError => error
        @logger.error('error sending message', {
          :payload => payload,
          :error => error.to_s
        })
      end
    end

    # Manager is asking for a 'pong' message back as part of a keepalive strategy
    def pong(properties,msg)
      @lastping = Time.now

      payload = {
        :version => 1,
        :timestamp => Time.now.to_i,
        :agent_id => @settings[:daemon][:id],
        :response => "pong"
      }

      @logger.debug('sending heartbeat response', {
        :payload => payload
      })

      begin
        @amq.direct('akwire.agent.to.hub').publish(Oj.dump(payload), :correlation_id => properties.correlation_id)
      rescue AMQ::Client::ConnectionClosedError => error
        @logger.error('error sending message', {
          :payload => payload,
          :error => error.to_s
        })
      end
    end

    # Manager is asking for a 'pong' message back as part of a keepalive strategy
    def error(properties,msg,err)

      payload = {
        :version => 1,
        :timestamp => Time.now.to_i,
        :agent_id => @settings[:daemon][:id],
        :response => "error",
        :error => err
      }

      @logger.debug('sending ERROR response', {
        :payload => payload
      })

      begin
        @amq.direct('akwire.agent.to.hub').publish(Oj.dump(payload), :correlation_id => properties.correlation_id)
      rescue AMQ::Client::ConnectionClosedError => error
        @logger.error('error sending message', {
          :payload => payload,
          :error => error.to_s
        })
      end
    end

    def hello_agent(properties,msg)
      hello_manager = lambda do
        payload = {
          :version => 1,
          :timestamp => Time.now.to_i,
          :agent_id => @settings[:daemon][:id],
          :response => "hello-manager"
        }
        
        @logger.debug('sending hello-manager response', {
                        :payload => payload
                      })

        begin
          @amq.direct('akwire.agent.to.hub').publish(Oj.dump(payload), :correlation_id => properties.correlation_id)
        rescue AMQ::Client::ConnectionClosedError => error
          @logger.error('error sending message', {
                          :payload => payload,
                          :error => error.to_s
                        })
        end
      end
      
      check_lastping = lambda do
        if (Time.now - @lastping) > 30
          @logger.warn('session ended; agent has unexpectedly ceased pinging the agent; reverting to un-managed mode', {
                         :lastping => @lastping
                       })
          
          # Cancel the old worker
          @session_worker.cancel
          
          @session_worker = EM::PeriodicTimer.new(60) do
            if @rabbitmq.connected?
              broadcast_hello
            end
          end
        end
      end

      # this point we should validate the manager's identity, or if
      # this is our very first run we might auto-accept the identity
      # and save it in a file under var
      
      # to save:
      #  - pid, own identity, manager's identity
      # use ssh public keys for identities?
      # ssl certs?
      
      # support a list of local commands to run (like munin)
      
      @logger.info('session established with manager', {
                       :command => msg
                     })
      
      # Cancel our current session worker which is busy broadcasting
        # hello's and replace it with one that check's for new pings
      # from the manager

      @lastping = Time.now
      
      @session_worker.cancel
      
      @session_worker = EM::PeriodicTimer.new(5) do
        if @rabbitmq.connected?
            check_lastping.call
          end
      end
      
      hello_manager.call
    
    end

    def process_command(properties, msg)
      # Process as if is command
      if (msg[:command].nil?)
        @logger.error('malformed command', {
                       :malformed => msg
                     })
      else
        case msg[:command]
        when "hello-agent" then hello_agent(properties,msg)
        when "ping" then pong(properties,msg)
        else
          @logger.info('unrecognized command', {
                         :unrecognized => msg
                       })
          error(properties,msg,"Unrecognized command: #{msg[:command]}")
        end
      end
    end

    # Establish a session with the manager
    def setup_session
      @logger.debug('binding to hub.to.agent exchange')
      
      @command_queue = @amq.queue(@settings[:daemon][:id], :auto_delete => true) do |queue|
        
        @logger.debug('binding agent queue to (hub -> agent) exchange', {
                        :queue_name => queue.name
                      })

        # No other agents should receive commands directed to this agent
        queue.bind(@amq.fanout("akwire.hub.to.agent"), :routing_key => @settings[:daemon][:id])

        broadcast_hello

        # Have the session worker broadcast hello messages until a manager responds
        @session_worker = EM::PeriodicTimer.new(60) do
          if @rabbitmq.connected?
            broadcast_hello
          end
        end

        queue.subscribe do |properties,payload|
          @logger.debug("received message: #{payload}")

          message = Oj.load(payload)
          process_command(properties, message)
        end
      end
    end

    def publish_observations
      @collectors.collect_observations do |collector, obs|
        obs.each do |o|
          begin
            @logger.debug("publishing observation", o.to_hash)
            @amq.direct('observations').publish(Oj.dump(obs.to_s))
          rescue AMQ::Client::ConnectionClosedError => error
            @logger.error('failed to publish keepalive', {
                            :payload => obs,
                            :error => error.to_s
                          })
          end
        end
      end    
    end

    def setup_collectors
      @logger.debug('scheduling collectors')
      publish_observations
      @timers << EM::PeriodicTimer.new(60) do
        if @rabbitmq.connected?
          publish_observations
        end
      end
    end
    
    def unsubscribe
      @logger.warn('unsubscribing from client subscriptions')
      if @rabbitmq.connected?
        @command_queue.unsubscribe
      else
        @command.before_recovery do
          @command.unsubscribe
        end
      end
    end
    
    def complete_checks_in_progress(&when_done)
      @logger.info('completing checks in progress', {
        :checks_in_progress => @checks_in_progress
      })
      retry_until_true do
        if @checks_in_progress.empty?
          when_done.call
          true
        else
          false
        end
      end
    end

    def start
      setup_rabbitmq

# two major modes
      case @settings[:daemon][:mode].to_sym
      when :managed then
        @logger.info("Running in MANAGED mode")

#  - managed:
#    - announce until registered
#    - no data published until registered
#    - commands accepted
#    - configs pulled

# register
# pull config for each collector

# process incoming requests
# stop publishing keepalives once registered?
        
        setup_session
        setup_collectors

      when :independent then
        @logger.info("Running in INDEPENDENT mode")

#  - independent: (collectd-like existence)
#    - no keep alives
#    - no commands accepted
#    - won't register
#    - collectors published immediately

        setup_collectors
      end

    end

    def stop
      @logger.warn('stopping')
      @session_worker.cancel
      @timers.each do |timer|
        timer.cancel
      end
      unsubscribe
      complete_checks_in_progress do
        @logger.warn('stopping collectors')
        @collectors.stop_all do
          @rabbitmq.close
          @logger.warn('stopping reactor')
          EM::stop_event_loop
        end
      end
    end

    def trap_signals
      @signals = Array.new
      STOP_SIGNALS.each do |signal|
        Signal.trap(signal) do
          @signals << signal
        end
      end
      EM::PeriodicTimer.new(1) do
        signal = @signals.shift
        if STOP_SIGNALS.include?(signal)
          @logger.warn('received signal', {
            :signal => signal
          })
          stop
        end
      end
    end
  end
end
