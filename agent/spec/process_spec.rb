require File.dirname(__FILE__) + '/../lib/akwire/base.rb'
require File.dirname(__FILE__) + '/helpers.rb'

describe 'Akwire::Process' do
  include Helpers

  before do
    @process = Akwire::Process.new
  end

  it 'can create a pid file' do
    @process.write_pid('/tmp/akwire.pid')
    File.open('/tmp/akwire.pid', 'r').read.should eq(::Process.pid.to_s + "\n")
  end

  it 'can exit if it cannot create a pid file' do
    with_stdout_redirect do
      lambda { @process.write_pid('/akwire.pid') }.should raise_error(SystemExit)
    end
  end
end
