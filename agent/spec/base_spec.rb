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
    ENV['SENSU_CONFIG_FILES'] = nil
    settings = @base.settings
    settings.should respond_to(:validate, :[])
    settings[:checks][:merger][:command].should eq('echo -n merger')
    settings[:checks][:merger][:subscribers].should eq(['test'])
    settings[:checks][:merger][:interval].should eq(60)
    ENV['SENSU_CONFIG_FILES'].should include(File.expand_path(options[:config_file]))
  end

  it 'can load extensions' do
    extensions = @base.extensions
    extensions.should respond_to(:[])
    extensions[:mutators].should be_kind_of(Hash)
    extensions[:handlers].should be_kind_of(Hash)
    extensions[:mutators]['only_check_output'].should be_an_instance_of(Akwire::Extension::OnlyCheckOutput)
    extensions[:mutators]['opentsdb'].should be_an_instance_of(Akwire::Extension::OpenTSDB)
    extensions[:handlers]['debug'].should be_an_instance_of(Akwire::Extension::Debug)
  end

  it 'can setup the current process' do
    @base.setup_process
    EM::threadpool_size.should eq(20)
  end
end
