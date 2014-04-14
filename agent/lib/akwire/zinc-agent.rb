#!/usr/bin/env ruby

if File.symlink?(__FILE__)
  ZINC_DIR = File.dirname(File.dirname(File.readlink(__FILE__)))
elsif File.dirname(__FILE__) == "."
  ZINC_DIR = File.dirname(File.expand_path("."))
else
  ZINC_DIR = File.expand_path(File.dirname(File.dirname(__FILE__)))
end

$: << (ZINC_DIR + "/lib")

require 'yaml'
require 'tmr'
require 'logging'
require 'getoptlong'

module Zinc
  class Daemon
    include Logging

    def initialize(name, args)
      @name = File.basename(name)
      @log_level = Logger::WARN
      @interval = 60 # seconds
      @config = ZINC_DIR + "/etc/zinc.conf"
    end

    def help_text
      puts <<HELP_MSG
USAGE
  #{@name} [OPTIONS] <start|stop|restart|status|reload>

OPTIONS
  -h,--help             Print this help statement and exit
  -v,--verbose          Verbose logging to stdout
  -cFILE,--config=FILE  Run using FILE

ACTIONS
  <none>
     Run the daemon in the foreground, useful for debugging

  start
     Start the daemon in the background

  stop
     Stop the daemon

  restart
     Restart the daemon, will run in the background

  status
     Prints a list of loaded TMRs, exits with 0 if running, 1 if not

  reload
     Reload installed TMRs
HELP_MSG
    end

    def parse_args
      opts = GetoptLong.new(
        [ '--help', '-h', GetoptLong::NO_ARGUMENT ],
        [ '--verbose', '-v', GetoptLong::NO_ARGUMENT ],
        [ '--interval', '-i', GetoptLong::REQUIRED_ARGUMENT ]
      )

      opts.each do |opt, arg|
        case opt
        when '--help'
          help_text
          exit(0)
        when '--verbose' then @log_level = Logger::DEBUG
        when '--interval' then @interval = Integer(arg)
        else
          help_text
          exit(-1)
        end
      end
    end

    def parse_config
    end

    def logging
      original_formatter = Logger::Formatter.new
      logger.formatter = proc { |severity, datetime, progname, msg|
        original_formatter.call(severity, datetime, progname, msg.dump)
      }
      logger.info(input)
    end

    # helper functions

    def check_if_running
      return false
    end

    # ACTIONS

    def run
      logger.info("Running")
      while true

      end

    end

    def main
      parse_args
      parse_config

      # configure logging
      # write to both syslog and a regular log file

      # investigate treetop for more parsing options
      logger.debug("ARGV:")
      logger.debug(@args)

      if @args.size == 0
        run
      else
        action=@args.shift
        logger.info("ACTION: #{action}")
        case action
        when "start"   then start
        when "stop"    then stop
        when "restart" then restart
        when "reload"  then reload
        when "status"  then status
        else
          help_text
          exit -1
        end
      end
    end
  end
end

if __FILE__ == $PROGRAM_NAME
  Zinc::Daemon.new($PROGRAM_NAME, ARGV).main
end
