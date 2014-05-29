module Akwire
  module Boolean; end
  class TrueClass; include Boolean; end
  class FalseClass; include Boolean; end

  class Observation
    def initialize(defn)
      @logger = Logger.get
      @def = defn
    end

    def collector
      @def.mod.prop(:name)
    end

    def key
      @def.prop(:name)
    end

    def defn
      @def
    end

  end

  class Measurement < Observation
    def initialize(defn, value)
      super defn
      @value = Float(value)
    end

    def to_s
      "#{defn.mod.prop(:name)}/#{defn.prop(:name)} => #{@value}"
    end

    def to_hash
      {
        :type => "m",
#        :collector => collector,
        :key => key,
        :value => @value
      }
    end
  end

  class Report < Observation
    def initialize(defn, value)
      super defn
      @value = value.to_s
    end

    def to_s
      "#{@def.mod.prop(:name)}/#{@def.prop(:name)} => #{@value}"
    end
  end

  class Status < Observation
    def initialize(defn, value)
      super defn
      @value = value.to_s
    end

    def to_s
      "#{@def.mod.prop(:name)}/#{@def.prop(:name)} => #{@value}"
    end
  end

  class NagiosWrapper
    def initialize(defn, value)
      super defn
      @value = value.to_s
    end

    def to_s
      "#{@def.mod.prop(:name)}/#{@def.prop(:name)} => #{@value}"
    end
  end

  class PatternDsl
    def initialize(name, mod)
      @logger = Logger.get
      @module = mod
      @props = {}
      @props[:name] = name
    end

    # dsl methods

    def enum(*vals)
      @props[:callback] = block
    end

    def description(val)
      @props[:description] = val
    end

    # accessor

    def prop(p)
      @props[p]
    end

    def props
      @props
    end

    def mod
      @module
    end
  end

  class AsyncDataCollectorsDsl
    # event, log, alert
    # aperiodic data types: event, log, alert
  end

  class Option
    def initialize(params)
#option :collect_loopback,
#       :description => "When true will collect stats for the loopback device",
#       :type => Boolean,
#       :default => false
      @params = params
    end
  end

  class MeasurementDsl
    def initialize(name, mod)
      @logger = Logger.get
      @module = mod
      @props = {}
      @props[:name] = name
      @props[:description] = "no description provided"
      @props[:units] = "unit"
      @props[:type] = :absolute
    end
    
    def describe__
      {
        :name => @props[:name],
        :description => @props[:description],
        :units => @props[:units],
        :type => @props[:type],
      }
    end

    def collect__
Measurement.new(m_def, m_def.prop(:callback).call())
    end

    # dsl methods

    def collect(&block)
      @props[:callback] = block
    end

    def description(val)
      @props[:description] = val
    end

    def units(val, included_in_key=false)
      @props[:units] = val
      @props[:units_included_in_key] = included_in_key
    end

    def type(val)
      raise "Bad type: #{val}" unless [:gauge, :rate, :absolute, :timer, :delta, :counter].include?(val)
      @props[:type] = val
    end

    def observe(m,opts={})
    end

    def option(opt)
      # get the configured value of some option
      raise "Missing option: #{opt} from settings #{mod.settings}" if mod.settings[opt.to_s].nil?
      mod.settings[opt.to_s]
    end

    # accessor

    def prop(p)
      @props[p]
    end

    def props
      @props
    end

    def mod
      @module
    end
  end

  class CollectorDsl
    def initialize(defaults={})
      @logger = Logger.get

      @props = {}
      @props[:options] = {}
      @props[:patterns] = {}

      @props[:measurements] = {}
      @props[:reports] = {}
      @props[:checks] = {}

      @props[:name] = nil
      @props[:version] = nil
      @props[:authors] = []
      @props[:description] = nil
      @props[:platform] = nil
      @props[:arch] = nil

      @settings = {}
    end

    def describe__
      def describe_measurements
        i = []
        @props[:measurements].each { |name,m|
          i << m.describe__
        }
        i
      end

      def describe_options
        i = {}
      end

      def describe_patterns
        i = {}
      end

      {
        :name => @props[:name],
        :version => @props[:version],
        :description => @props[:description],
        :measurements => describe_measurements,
        :options => describe_options,
        :patterns => describe_patterns,
      }
    end

    # Properties set by the author of the collector
    def props
      @props
    end

    # Configuration settings specified by the user
    def settings
      @settings
    end

    def name(v)
      @props[:name] = v
    end

    def version(v)
      @props[:version] = v
    end

    def author(name, email=nil)
      @props[:authors] = [name, email]
    end

    def description(v)
      @props[:description] = v
    end

    def option(name, opts={})
      raise "Option name must be a Symbol" unless name.is_a?(Symbol)
      raise "Option name must be unique" unless @props[:options].nil?
      @props[:options][name] = Option.new(opts)
    end

    def measurement(name, opts = {}, &block)
      m = MeasurementDsl.new(name,self)
      m.instance_eval(&block)
      @props[:measurements][name] = m
    end

    # status or health check
    def report(name, opts = {}, &block)
      r = ReportDsl.new(name,self)
      r.instance_eval(&block)
      @props[:reports][name] = r
    end

    # status or health check
    def check(name, opts = {}, &block)
      c = CheckDsl.new(name,self)
      c.instance_eval(&block)
      @props[:checks][name] = c
    end

    def pattern(name, matcher=nil, &block)
      p = PatternDsl.new(name,self)
      if block
        p.instance_eval(&block)
      end
      @props[:patterns][name] = p
    end

    # option :data_points,
    #        :description => "Number of data points to include in average check (smooths out spikes)",
    #        :default => 1
    def option(name, opts)
      unless name.is_a?(Symbol)
        raise "Option name #{name} must be a Symbol"
      end
      @props[:options][name] = opts
    end
  end

  class Collector
    def initialize(file)
      @logger = Logger.get
      @logger.debug("collector script: #{file}")
      @file = file
      @name, @version = get_meta(file)
      @authors = []
      @description = nil
      @platform = nil
      @os = nil
      @instances = {}
      @singleton = false
    end

    def get_meta(file)
      fd = File::new(file)
      name = nil
      version = nil

      while (true)
        if (name and version)
          return name, version
        end

        line = fd.gets
        raise "Malformed collector, did not find name and version" if line.nil?

        # Skip comments
        next if (/^[[:blank:]]*#.*$/.match(line))

        # Look for the 'name' property
        name_match = /^[[:blank:]]*name[[:blank:]]+['"](\w*)['"].*$/.match(line)
        if (name_match and name.nil?)
          name = name_match[1]
          next
        elsif (name_match and name)
          raise "Duplicate name property in collector!"
        end

        # Look for the 'version' property
        version_match = /^[[:blank:]]*version[[:blank:]]+['"]([\w\.]*)['"].*$/.match(line)
        if (version_match and version.nil?)
          version = version_match[1]
          next
        elsif (version_match and version)
          raise "Duplicate version property in collector!"
        end
      end
    end

    def configure_instance(instance_name, settings)
      instance = CollectorDsl.new
      instance.instance_eval(File::read(@file), @file)
      raise "Duplicate instance with name #{instance_name}" unless @instances[instance_name].nil?

      apply_settings(instance,instance_name,settings)

      @instances[instance_name] = instance

      @description = instance.props[:description] if @description.nil?
      @authors = instance.props[:authors] if @authors.nil?

      if (instance_name.nil?)
        @logger.info("configured collector",
                     {
                       :collector => @name,
                       :singleton => true
                     })
      else
        @logger.info("configured collector", 
                     {
                       :collector => @name,
                       :instance => instance_name
                     })
      end
    end

    def instances
      @instances.keys
    end

    def describe
      instance = CollectorDsl.new
      instance.instance_eval(File::read(@file), @file)
      instance.describe__
    end

    def apply_settings(instance,instance_name,settings)
      settings = {} if settings.nil?
      instance.settings[:instance_name] = instance_name
      instance.settings[:mode] = settings[:mode] || :passive
      instance.settings[:interval] = settings[:interval] || 5
    end

    def configured?
      @instances.size > 0
    end

    def collect
      obs = []
      @instances.each_pair { |instance_name, instance|
        @logger.debug("collecting measurements",
                      {
                        :collector => @name,
                        :instance => instance_name
                      })
        instance.props[:measurements].each_pair { |m_name,m_def|
          obs << m_def.collect
        }
      }
      return obs
    end

    # Meta-data provided by the plugin

    def name
      @name
    end

    def version
      @version
    end

    def authors
      @authors
    end

    def description
      @description
    end

    # Life-cycle hooks
    def stop_all(&block)
      block.call if block_given?
    end

    # Configured values

    def mode_is_active?
      @mode == :active
    end

    def mode_is_passive?
      @mode == :passive
    end

    def mode
      @mode
    end

    def configured_interval
      1
    end

    def needs_config?
      false
    end

    def to_s
      name
    end
  end
end
