#!/usr/bin/env ruby

if File.symlink?(__FILE__)
  ZINC_DIR = File.dirname(File.dirname(File.readlink(__FILE__)))
elsif File.dirname(__FILE__) == "."
  ZINC_DIR = File.dirname(File.expand_path("."))
else
  ZINC_DIR = File.expand_path(File.dirname(File.dirname(__FILE__)))
end

$: << (ZINC_DIR + "/lib")

require 'mon'
require 'logging'
require 'getoptlong'

module Zinc
  class MonRunner
    include Logging

    def initialize(name, args)
      @name = File.basename(name)
      @args = args
      @verbose = false
      @publish = false
      @publish_url = "http://localhost:9090"
    end

    def help_text
      puts <<HELP_MSG
USAGE
  #{@name} [OPTIONS] ACTION
  #{@name} [OPTIONS] SCRIPT

OPTIONS
  -h,--help                Print this help statement and exit
  -v,--verbose             Verbose logging to stdout
  -p[URL],--publish[=URL]  Publish collected metrics, a custom URL may be provided
  -nX, --repeat=X          Collect X times, 1 second default interval
  -iI, --interval=I        Wait I seconds between collect cycles

ZINC ACTIONS              !! May only be run as 'root' or 'mon' !!
  run MON                  Run the installed MON, collect once, print the results
  list MON
  search PATTERN
  install MON
  uninstall MON
  enable MON
  disable MON
  
SCRIPT ACTIONS
  validate SCRIPT          Run and validate
  inspect SCRIPT           Print metadata

EXAMPLES
  Run the custom script custom.mon:
    $ mon custom.mon

  Run the installed MON 'core'
    $ mon run core

  Run custom.mon 12 times, once each five seconds:
    $ mon -i 5 -n 12 custom.mon
HELP_MSG
    end

    def parse_args
      opts = GetoptLong.new(
        [ '--help', '-h', GetoptLong::NO_ARGUMENT ],
        [ '--verbose', '-v', GetoptLong::NO_ARGUMENT ],
        [ '--publish', '-p', GetoptLong::OPTIONAL_ARGUMENT ],
        [ '--repeat', '-n', GetoptLong::REQUIRED_ARGUMENT ],
        [ '--interval', '-i', GetoptLong::REQUIRED_ARGUMENT ]
      )

      opts.each do |opt, arg|
        case opt
        when '--help'
          help_text
          exit(0)
        when '--verbose' then @verbose = true
        when '--publish' then 
          @publish = true 
          if not arg.nil? 
            @publish_url=arg 
          end
        when '--repeat' then @repeat = Integer(arg)
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

    # ZINC ACTIONS

    def run_mon(mon)
      logger.info("Installed MON: #{mon}")
      m = Zinc::Mon.new(ZINC_DIR + "/modules/" + mon + "/main.mon")
      obs = m.collect
      puts obs
    end

    # SCRIPT ACTIONS

    def run_script(script)
      logger.info("MON: #{script}")
      m = Zinc::Mon.new(script)
      obs = m.collect
      puts obs
    end

    def validate(script)
      Zinc::Mon.new(script)
    end

    def inspect(script)
      Zinc::Mon.new(script)
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
        help_text
        exit -2
      end

      action=@args.shift
      logger.info("ACTION: #{action}")
      case action
      when "run" then run_mon(@args.shift)
      when "inspect" then inspect(@args.shift)
      when "validate" then validate(@args.shift)
      else
        # if nothing else interpret as a script
        script = action
        run_script(script)
      end
    end
  end
end

if __FILE__ == $PROGRAM_NAME
  Zinc::MonRunner.new($PROGRAM_NAME, ARGV).main
end
