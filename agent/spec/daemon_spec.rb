require File.dirname(__FILE__) + '/../lib/akwire/daemon.rb'
require File.dirname(__FILE__) + '/helpers.rb'

gem "debugger"

describe 'Akwire::Daemon' do
  include Helpers

  before do
    @daemon = Akwire::Daemon.new(options)
  end

  after do
    # Just to make sure we get all the logs
    @daemon.flush_logs
  end

  it 'can connect to rabbitmq' do
    async_wrapper do
      @daemon.setup_rabbitmq
      async_done
    end
  end

  it 'can broadcast a hello' do
    async_wrapper do
      agent_tx_queue do |queue|
        @daemon.setup_rabbitmq
        @daemon.broadcast_hello
        queue.subscribe do |payload|
          payload.should_not be_nil
          hello = Oj.load(payload)
          hello[:agent_id].should eq('12345-12345')
          hello[:version].should eq(1)
          async_done
        end
      end
    end
  end

  # the basic flow of sessions are:
  #   agent broadcasts "hello" when it starts
  #   hub upon seeing an unregistered agent sends a "hello-agent"
  #   agent responds with "hello-manager", stops broadcasting "hello"

  it 'can create a session with the hub' do
    async_wrapper do
      agent_tx_queue do |queue|
        @daemon.setup_rabbitmq
        @daemon.setup_session

        timer(1) do
          command_from_hub({"command" => "hello-agent"})
        end

        queue.subscribe do |payload|
          payload.should_not be_nil
          msg = Oj.load(payload)

          if msg[:hello]
            msg[:agent_id].should eq('12345-12345')
            msg[:version].should eq(1)
            msg[:hello].should eq(true)
          else
            msg[:agent_id].should eq('12345-12345')
            msg[:version].should eq(1)
            msg[:response].should eq('hello-manager')
            async_done
          end
        end
      end
    end
  end

  it 'can load collectors' do
    c = @daemon.list_collectors.payload
    c[:collectors].should_not be_nil
    c[:collectors].should include(:basic)
    c[:collectors].should include(:advanced)
  end

  it 'can load instances from collectors' do
    c = @daemon.list_instances.payload
    c[:instances].should_not be_nil
    c[:instances].should include(:basic)
    c[:instances][:basic].should include(nil)
    c[:instances][:advanced].should include(:test1)
  end

  it 'can publish collector metadata (basic)' do
    c = @daemon.describe_collector({:collector => "basic"}).payload
    puts c
    puts @daemon.collect_all_measurements({:collector => "basic"}).payload
  end

  it 'can publish collector metadata (advanced)' do
    c = @daemon.describe_collector({:collector => "advanced"}).payload
    puts c
  end

#  it 'can send a check result' do
#    async_wrapper do
#      result_queue do |queue|
#        @daemon.setup_rabbitmq
#        check = result_template[:check]
#        @daemon.publish_result(check)
#        queue.subscribe do |payload|
#          result = Oj.load(payload)
#          result[:client].should eq('i-424242')
#          result[:check][:name].should eq('foobar')
#          async_done
#        end
#      end
#    end
#  end

#  it 'can execute a check command' do
#    async_wrapper do
#      result_queue do |queue|
#        @daemon.setup_rabbitmq
#        @daemon.execute_check_command(check_template)
#        queue.subscribe do |payload|
#          result = Oj.load(payload)
#          result[:client].should eq('i-424242')
#          result[:check][:output].should eq("WARNING\n")
#          result[:check].should have_key(:executed)
#          async_done
#        end
#      end
#    end
#  end

#  it 'can receive a check request and execute the check' do
#    async_wrapper do
#      result_queue do |queue|
#        @daemon.setup_rabbitmq
#        @daemon.setup_subscriptions
#        timer(1) do
#          amq.fanout('test').publish(Oj.dump(check_template))
#        end
#        queue.subscribe do |payload|
#          result = Oj.load(payload)
#          result[:client].should eq('i-424242')
#          result[:check][:output].should eq("WARNING\n")
#          result[:check][:status].should eq(1)
#          async_done
#        end
#      end
#    end
#  end
end
