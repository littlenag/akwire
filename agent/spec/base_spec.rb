require File.dirname(__FILE__) + '/../lib/akwire/base.rb'
require File.dirname(__FILE__) + '/helpers.rb'

describe 'Akwire::Base' do
  include Helpers

  before do
    @base = Akwire::Base.new(options)
  end

  it 'can setup the logger' do
    logger = @base.logger
    logger.should respond_to(:info, :warn, :error, :fatal)
  end

  it 'can load settings from configuration files' do
    ENV['AKWIRE_CONFIG_FILES'] = nil
    @settings = @base.settings
    settings = @settings
    settings.should respond_to(:validate, :[])
    settings[:collectors][:core][:mode].should eq(:active)
    ENV['AKWIRE_CONFIG_FILES'].should include(File.expand_path(options[:config_file]))
  end

  it 'can load collectors' do
    collectors = @base.collectors
    collectors.should respond_to(:[])
    collectors["basic"].should be_an_instance_of(Akwire::Collector)
  end

  it 'can setup the current process' do
    @base.setup_process
    EM::threadpool_size.should eq(20)
  end
end
