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
      @collectors = base.collectors
      base.setup_process
      @collectors.configure_from_settings(@settings[:collectors].to_hash)

      @waiting_for_token = true
      @token = nil
      @token_requester = nil

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

    def request_token
      payload = {
        :version => 1,
        :timestamp => Time.now.to_i,
        :agent_id => @settings[:daemon][:id],
        :command => "issue-session-token"
      }

      @logger.debug('requesting session token', {
        :payload => payload
      })

      begin
        @amq.direct('registration').publish(Oj.dump(payload))
      rescue AMQ::Client::ConnectionClosedError => error
        @logger.error('failed to request session token', {
          :payload => payload,
          :error => error.to_s
        })
      end
    end

    def process_command(command)
      @logger.info('received command', {
                     :command => command
                   })
      
    end

    # Establish a session with the manager
    def setup_session
      @logger.debug('binding to m2a exchange')
      
      @command_queue = @amq.queue(@settings[:daemon][:id], :auto_delete => true) do |queue|
        
        @logger.debug('binding agent queue to (management -> agent) exchange', {
                        :queue_name => queue.name
                      })

        # No other agents should receive commands directed to this agent
        queue.bind(@amq.fanout("m2a"), :routing_key => @settings[:daemon][:id])

        # Drop stale messages
        queue.purge

        token_requestor = EM::PeriodicTimer.new(30) do
          if @rabbitmq.connected?
            request_token
          end
        end

        handle_token = EM.spawn do
          case response[:result] 
          when "registration-accepted" then
            
            @logger.info('session established with manager', {
                           :response => response
                         })
            
            # this point we might validate the token and the manager's
            # identity, or if this is our very first run we might
            # auto-accept the identity and save it in a file under var
            
            # to save:
            #  - pid, own identity, manager's identity
            # use ssh public keys for identities?
            # ssl certs?
            
#        @timers << EM::PeriodicTimer.new(60) do
#          if @rabbitmq.connected?
#            check_heartbeat
#          end
#        end

            @token = response[:token]
            
          when "registration-denied" then
            @logger.error('manager denied registration attempt', {
                            :manager => response[:manager_id]
                          })
            stop
          else
            @logger.info('malformed message', {
                           :manager => response
                         })
          end

        end

        handle_command = EM.spawn do 
          process_command(command)
        end

        queue.subscribe do |headers,payload|
          command = Oj.load(payload)
          if @token.nil?
            handle_token(command)
          else
            handle_command(command)
          end
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
        @check_request_queue.unsubscribe
      else
        @check_request_queue.before_recovery do
          @check_request_queue.unsubscribe
        end
      end
    end
    
    def complete_checks_in_progress(&block)
      @logger.info('completing checks in progress', {
        :checks_in_progress => @checks_in_progress
      })
      retry_until_true do
        if @checks_in_progress.empty?
          block.call
          true
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
      @timers.each do |timer|
        timer.cancel
      end
      unsubscribe
      complete_checks_in_progress do
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
