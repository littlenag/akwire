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
      @collectors.load_settings(@settings.to_hash)
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
      end
      @amq = @rabbitmq.channel
    end

    def publish_keepalive
      payload = {
          :timestamp => Time.now.to_i,
          :agent => "some random id"
      }
      @logger.debug('publishing keepalive', {
        :payload => payload
      })
      begin
        @amq.direct('keepalives').publish(Oj.dump(payload))
      rescue AMQ::Client::ConnectionClosedError => error
        @logger.error('failed to publish keepalive', {
          :payload => payload,
          :error => error.to_s
        })
      end
    end

    def setup_keepalives
      @logger.debug('scheduling keepalives')
      publish_keepalive
      @timers << EM::PeriodicTimer.new(20) do
        if @rabbitmq.connected?
          publish_keepalive
        end
      end
    end

    def setup_subscriptions
      @logger.debug('subscribing to client subscriptions')
      @check_request_queue = @amq.queue('', :auto_delete => true) do |queue|
        @settings[:client][:subscriptions].each do |exchange_name|
          @logger.debug('binding queue to exchange', {
            :queue_name => queue.name,
            :exchange_name => exchange_name
          })
          queue.bind(@amq.fanout(exchange_name))
        end
        queue.subscribe do |payload|
          check = Oj.load(payload)
          @logger.info('received check request', {
            :check => check
          })
          process_check(check)
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
      setup_keepalives
#      setup_subscriptions
#      setup_standalone
#      setup_sockets

# load modules
# register
# pull config
# process incoming requests
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
