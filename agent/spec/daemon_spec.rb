require File.dirname(__FILE__) + '/../lib/akwire/daemon.rb'
require File.dirname(__FILE__) + '/helpers.rb'

describe 'Akwire::Daemon' do
  include Helpers

  before do
    @client = Akwire::Daemon.new(options)
  end

  it 'can connect to rabbitmq' do
    async_wrapper do
      @client.setup_rabbitmq
      async_done
    end
  end

  it 'can send a keepalive' do
    async_wrapper do
      keepalive_queue do |queue|
        @client.setup_rabbitmq
        @client.publish_keepalive
        queue.subscribe do |payload|
          keepalive = Oj.load(payload)
          keepalive[:name].should eq('i-424242')
          keepalive[:service][:password].should eq('REDACTED')
          async_done
        end
      end
    end
  end

  it 'can schedule keepalive publishing' do
    async_wrapper do
      keepalive_queue do |queue|
        @client.setup_rabbitmq
        @client.setup_keepalives
        queue.subscribe do |payload|
          keepalive = Oj.load(payload)
          keepalive[:name].should eq('i-424242')
          async_done
        end
      end
    end
  end

  it 'can send a check result' do
    async_wrapper do
      result_queue do |queue|
        @client.setup_rabbitmq
        check = result_template[:check]
        @client.publish_result(check)
        queue.subscribe do |payload|
          result = Oj.load(payload)
          result[:client].should eq('i-424242')
          result[:check][:name].should eq('foobar')
          async_done
        end
      end
    end
  end

  it 'can execute a check command' do
    async_wrapper do
      result_queue do |queue|
        @client.setup_rabbitmq
        @client.execute_check_command(check_template)
        queue.subscribe do |payload|
          result = Oj.load(payload)
          result[:client].should eq('i-424242')
          result[:check][:output].should eq("WARNING\n")
          result[:check].should have_key(:executed)
          async_done
        end
      end
    end
  end

  it 'can receive a check request and execute the check' do
    async_wrapper do
      result_queue do |queue|
        @client.setup_rabbitmq
        @client.setup_subscriptions
        timer(1) do
          amq.fanout('test').publish(Oj.dump(check_template))
        end
        queue.subscribe do |payload|
          result = Oj.load(payload)
          result[:client].should eq('i-424242')
          result[:check][:output].should eq("WARNING\n")
          result[:check][:status].should eq(1)
          async_done
        end
      end
    end
  end
end
