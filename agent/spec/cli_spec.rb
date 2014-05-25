require File.dirname(__FILE__) + '/../lib/akwire/cli.rb'
require File.dirname(__FILE__) + '/../lib/akwire/constants.rb'
require File.dirname(__FILE__) + '/helpers.rb'

describe 'Akwire::CLI' do
  include Helpers

  it 'does not provide default configuration options' do
    Akwire::CLI.read.should eq(Hash.new)
  end

  it 'can parse command line arguments' do
    options = Akwire::CLI.read([
      '-c', 'spec/config.json',
      '-d', 'spec/conf.d',
      '-e', 'spec/extensions',
      '-v',
      '-l', '/tmp/akwire_spec.log',
      '-p', '/tmp/akwire_spec.pid',
      '-b'
    ])
    expected = {
      :config_file => 'spec/config.json',
      :config_dirs => ['spec/conf.d'],
      :extension_dir => 'spec/extensions',
      :log_level => :debug,
      :log_file => '/tmp/akwire_spec.log',
      :pid_file => '/tmp/akwire_spec.pid',
      :daemonize => true
    }
    options.should eq(expected)
  end

  it 'can set the appropriate log level' do
    options = Akwire::CLI.read([
      '-v',
      '-L', 'warn'
    ])
    expected = {
      :log_level => :warn
    }
    options.should eq(expected)
  end

  it 'exits when an invalid log level is provided' do
    with_stdout_redirect do
      lambda { Akwire::CLI.read(['-L', 'invalid']) }.should raise_error SystemExit
    end
  end
end
