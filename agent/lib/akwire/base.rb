require 'rubygems'

gem 'oj', '2.0.9'
gem 'eventmachine', '1.0.3'

require 'time'
require 'uri'
require 'oj'

require File.join(File.dirname(__FILE__), 'constants')
require File.join(File.dirname(__FILE__), 'utilities')
require File.join(File.dirname(__FILE__), 'cli')
require File.join(File.dirname(__FILE__), 'logstream')
require File.join(File.dirname(__FILE__), 'settings')
require File.join(File.dirname(__FILE__), 'collectors')
require File.join(File.dirname(__FILE__), 'dsl')
require File.join(File.dirname(__FILE__), 'process')
require File.join(File.dirname(__FILE__), 'io')
require File.join(File.dirname(__FILE__), 'rabbitmq')

Oj.default_options = {:mode => :compat, :symbol_keys => true}

module Akwire
  class Base
    def initialize(options={})
      @options = options
    end

    def logger(options={})
      if options[:reset].nil?
        logger = Logger.get(options)
      else
        logger = Logger.setup(options)
      end
      logger.setup_signal_traps
      logger
    end

    def settings
      settings = Settings.new
      settings.load_env
      if @options[:config_file]
        settings.load_file(@options[:config_file])
      end
      if @options[:config_dirs]
        @options[:config_dirs].each do |config_dir|
          settings.load_directory(config_dir)
        end
      end
      settings.validate
      settings.set_env
      settings
    end

    def collectors
      # This will instantiate the Collectors but they will have to wait for 
      # their configurations to create runnable instances
      collectors = Collectors.new
      if @options[:collectors_dir]
        collectors.load_directory(@options[:collectors_dir])
      end

      if @options[:load_gem_collectors]
        collectors.load_gems(@options[:load_gem_collectors])
      end

      # Return only the configured and runnable collectors that have instances
      collectors
    end

    def setup_process
      process = Process.new
      if @options[:daemonize]
        process.daemonize
      end
      if @options[:pid_file]
        process.write_pid(@options[:pid_file])
      end
    end
  end
end
