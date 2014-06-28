require File.join(File.dirname(__FILE__), 'base')

module Akwire
  class Daemon
    include Utilities

    def self.run(options={})
      daemon = self.new(options)
      EM::run do
        daemon.start
        daemon.trap_signals
      end
    end

    def flush_logs
      @logger.flush_logs
    end

    def initialize(options={})
      @options = options
      @base = Base.new(options)
      @logger = @base.logger(options)
      @settings = @base.settings
      @collectors = @base.collectors
      @base.setup_process

      @collectors.load_instances(@settings[:collectors].to_hash)

      @session_worker = nil
      @lastping = nil

      @timers = Array.new
      @checks_in_progress = Array.new
    end

    def setup_rabbitmq
      @logger.info('connecting to rabbitmq', {
        :settings => @settings[:rabbitmq]
      })
      @rabbitmq = RabbitMQ.connect(@settings[:rabbitmq])
      @rabbitmq.on_error do |error|
        @logger.fatal('rabbitmq connection error', {
          :error => error.to_s,
          :error_o => error
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

    ##################

    class Ok
      def self.[](response, opts={})
        raise "Response cannot be empty" if response.nil?
        Ok.new(opts.merge({:response => response}))
      end

      def initialize(payload)
        @payload = payload
      end

      def payload
        @payload
      end
    end

    class Error
      def self.[](msg, opts={})
        raise "Error message cannot be empty" if reponse.nil?
        Error.new(opts.merge({:response => "error", :error => msg}))
      end

      def initialize(payload)
        @payload = payload
      end

      def payload
        @payload
      end
    end

    # Manager is asking for a 'pong' message back as part of a keepalive strategy
    def pong
      @lastping = Time.now
      Ok["pong"]
    end

    # Manager is asking for a list of loaded collectors
    def list_collectors
      Ok["collectors", :collectors => @collectors.keys]
    end

    def list_instances
      Ok["instances", :instances => @collectors.instances]
    end

    def describe_collector(msg)
      Ok["collector-description", 
         :collector => msg[:collector], 
         :meta => @collectors[msg[:collector]].describe]
    end

    def collect_all_measurements(msg)
      Ok["collector-measurements", 
         :collector => msg[:collector], 
         :measurements => @collectors[msg[:collector]].collect]
    end

    def hello_agent(msg)
      # this point we should validate the manager's identity, or if
      # this is our very first run we might auto-accept the identity
      # and save it in a file under var
      
      # to save:
      #  - pid, own identity, manager's identity
      # use ssh public keys for identities?
      # ssl certs?
      
      # support a list of local commands to run (like munin)
      
      @logger.info('session established with manager', {:command => msg})
      
      # Cancel our current session worker which is busy broadcasting
        # hello's and replace it with one that check's for new pings
      # from the manager

      @lastping = Time.now
      
      @session_worker.cancel
      
      @session_worker = EM::PeriodicTimer.new(5) do
        if @rabbitmq.connected?
          
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
      end
      
      Ok["hello-manager"]
    end

    def process_command(properties, msg)
      # Process as if is command
      if (msg[:command].nil?)
        @logger.error('malformed command', {
                       :malformed => msg
                     })
      else
        response = case msg[:command]
        when "hello-agent"     then hello_agent(msg)
        when "ping"            then pong
        when "list-collectors" then list_collectors
        end

        if response
          respond(response.payload, properties)
        else
          @logger.info('unrecognized command', {
                         :unrecognized => msg
                       })
          respond({
                    :response => "error",
                    :error => err
                  },properties)
          error(properties,msg,"Unrecognized command: #{msg[:command]}")
        end
      end
    end

    def broadcast_hello
      @logger.info('broadcast hello')
      respond({:hello => true})
    end

    def respond(payload, properties=nil)
      payload[:version] = 1
      payload[:timestamp] = Time.now.to_i
      payload[:agent_id] = @settings[:daemon][:id]

      @logger.debug('sending response', {
        :payload => payload
      })

      begin
        if properties
          @amq.direct('akwire.agent.to.hub').publish(Oj.dump(payload), :correlation_id => properties.correlation_id)
        else
          @amq.direct('akwire.agent.to.hub').publish(Oj.dump(payload))
        end          
      rescue AMQ::Client::ConnectionClosedError => error
        @logger.error('error sending message', {
          :payload => payload,
          :error => error.to_s
        })
      end
    end

    # Establish a session with the manager
    def setup_session
      @logger.info('binding to hub.to.agent exchange')

      # Create to a local queue named for our agent's id
      @command_queue = @amq.queue(@settings[:daemon][:id], :auto_delete => true) do |queue|
        
        @logger.debug('binding agent queue to (hub -> agent) exchange', {
                        :queue_name => queue.name
                      })

        # Bind that queue to the central hub to agent exchange
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
      @logger.debug('harvesting from collectors')
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
        @command_queue.unsubscribe if @command_queue
      else
        if @command_queue
          @command_queue.before_recovery do
            @command_queue.unsubscribe
          end
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
