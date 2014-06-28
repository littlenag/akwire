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

    def to_hash
      raise "must override"
    end

  end

  class Measurement < Observation
    def initialize(defn, id, value)
      super defn
      @id = id
      @value = Float(value)
    end

    def to_s
      "#{@id} => #{@value}"
    end

    def to_hash
      {
        :type => "m",
#        :collector => collector,
        :id => @id,
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
    def initialize(base_name, matcher, mod)
      @logger = Logger.get
      @module = mod
      @props = {}
      @props[:id] = base_name
      @props[:matcher] = matcher
      # inject a new method into the class that captures this pattern object so that
      # it can be referred to by other definitions
      the_pattern = self
      mod.define_singleton_method(base_name, lambda{the_pattern})
    end

    def id
      @props[:id]
    end

    def validate__(v)
      if not v.is_a?(String)
        false
      elsif @props[:matcher].nil?
        true
      elsif @props[:matcher].is_a?(Regexp) and @props[:matcher].match(v)
        true
      elsif @props[:matcher].is_a?(Array) and @props[:matcher].include?(v)
        true 
      else
        false
      end
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

  class OptionDsl
    def initialize(params)
#option :collect_loopback,
#       :description => "When true will collect stats for the loopback device",
#       :type => Boolean,
#       :default => false
      @params = params
    end
  end

    def initialize(name, mod)
      @logger = Logger.get
      @module = mod
      @props = {}
      @props[:name] = name
      @props[:description] = "no description provided"
      @props[:default] = nil
      @props[:type] = Object

      # this could get a whole lot more powerful:
      # support auto-complete methods
      # enumerators/paging for possible values
      # arbitary json
    end

    def props
      @props
    end

    def defaulted__?
      not @props[:default].nil?
    end

    def validate__(v)
      case @props[:type]
      when String then
        true
      when Object then 
        true
      when Boolean then
        v.is_a?(TrueClass) or v.is_a?(FalseClass)
      when Integer then
        begin
          puts "fo"
          Integer(v)
          return true
        rescue
          puts "foff"
          return false
        end
      when Float then
        begin
          Float(v)
          true
        rescue
          false
        end
      end
    end

    # dsl methods

    def default(val)
      raise "Default value #{val} does not match type #{@props[:type]}" unless validate__(val)
      @props[:default] = val
    end

    def description(val)
      @props[:description] = val
    end

    def type(val)
      raise "Bad type: #{val}" unless [String, Float, Integer, Boolean].include?(val)
      @props[:type] = val
    end

    def units(val, included_in_key=false)
      @props[:units] = val
      @props[:units_included_in_key] = included_in_key
    end
  end

  class MeasurementDsl
    def initialize(base_name, patterns, mod)
      @logger = Logger.get
      @module = mod
      @props = {}
      @props[:name] = base_name
      @props[:patterns] = patterns
      @props[:description] = "no description provided"
      @props[:units] = "unit"
      @props[:type] = :absolute
    end

    def id
      @props[:name]
    end

    def id__(base_name, opts)
      raise "Name must be a symbol" unless base_name.is_a?(Symbol)
      id = {:name => base_name}
      opts.each { |k,v|
        id.merge!(k=>v.id)
      }
      id
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
      def m_id(opts)
        {:name => @props[:name]}.merge(opts)
      end

      def invoke
        @observed_values = []
        @props[:callback].call()
        @observed_values
      end

      @logger.debug("collecting measurements", :defn => @props[:name])

      obs = []

      invoke.each do |o|
        m = Measurement.new(self, m_id(o[1]), o[0])
        @logger.debug("observation", :obs => m.to_hash)
        obs << m
      end
      
      return obs
    end

    def validate_pattern_params__(params)
      params.each { |k,v|
        raise "Pattern name #{k} must be a symbol" unless k.is_a?(Symbol)
      }

      pattern_defs = @props[:patterns]
      pattern_defs.each { |name, defn|
        raise "Missing values for pattern param: #{name}" if params[name].nil?
      }

      params.each { |k,v|
        raise "Value #{v} for pattern #{k} not a valid type (e.g. String)" unless v.is_a?(String)
        raise "Value #{v} for pattern #{k} is not valid for pattern" unless pattern_defs[k].validate__(v)
      }
      params
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

    def observe(v,pattern_params={})
      @observed_values << [v, validate_pattern_params__(pattern_params)]
    end

    def get_option(opt)
      raise "Missing option: #{opt} from settings #{mod.settings}" if mod.settings[:options][opt].nil?
      mod.settings[:options][opt]
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

    def validate_pattern_refs__(pattern_refs)
      pattern_refs.each { |k,v|
        raise "Pattern name #{k} must be a symbol" unless k.is_a?(Symbol)
        raise "Pattern target #{v} must reference a valid pattern definition" unless v.is_a?(PatternDsl)
      }
      pattern_refs
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
      @props[:options][name] = Option.new(name, opts)
    end

    def measurement(name, pattern_refs = {}, &block)
      raise "Measurement name #{name} must be a Symbol" unless name.is_a?(Symbol)
      m = MeasurementDsl.new(name,validate_pattern_refs__(pattern_refs),self)
      m.instance_eval(&block)
      @props[:measurements][m.id] = m
    end

    # status or health check
    def report(name, opts = {}, &block)
      raise "Report name #{name} must be a Symbol" unless name.is_a?(Symbol)
      r = ReportDsl.new(name,self)
      r.instance_eval(&block)
      @props[:reports][r.id] = r
    end

    # status or health check
    def check(name, opts = {}, &block)
      raise "Check name #{name} must be a Symbol" unless name.is_a?(Symbol)
      c = CheckDsl.new(name,self)
      c.instance_eval(&block)
      @props[:checks][c.id] = c
    end

    def pattern(name, matcher=nil, &block)
      raise "Pattern name #{name} must be a Symbol" unless name.is_a?(Symbol)
      p = PatternDsl.new(name,matcher,self)
      if block
        p.instance_eval(&block)
      end
      @props[:patterns][p.id] = p
    end

    def option(name, &block)
      raise "Option name #{name} must be a Symbol" unless name.is_a?(Symbol)
      o = OptionDsl.new(name,self)
      if block
        o.instance_eval(&block)
      end
      @props[:options][name] = o
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
                       :singleton => true,
                       :settings => settings
                     })
      else
        @logger.info("configured collector", 
                     {
                       :collector => @name,
                       :instance => instance_name,
                       :settings => settings
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

    def apply_settings(instance,instance_name,settings={})
      settings[:instance_name] = instance_name
      settings[:mode] = settings[:mode] || :passive
      settings[:interval] = settings[:interval] || 5
      settings[:options] = settings[:options] || {}

      options = instance.props[:options]
      options.each { |n,o|
        # Each option had best appear or be defaulted
        if settings[:options][n].nil?
          if o.defaulted__?
            # Is there a default value?
            @logger.info("Applying default option value", :instance => instance_name, :value => o.props[:default], :option => n)
            settings[:options][n] = o.props[:default]
          else
            # Nope, error out here
            raise "Missing required value for option #{n}"
          end
        end
      }

      instance.settings.merge!(settings)
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
          obs << m_def.collect__
                           :instance => instance_name
                         })
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
