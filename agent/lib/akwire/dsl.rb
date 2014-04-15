module Akwire
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

  end

  class Measurement < Observation
    def initialize(defn, value)
      super defn
      @def = defn
      @value = Float(value)
    end

    def to_s
      "#{@def.mod.prop(:name)}/#{@def.prop(:name)} => #{@value}"
    end

    def to_hash
      {
        :type => "m",
        :collector => collector,
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

# aperiodic data types: event, log, alert

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

  class MeasurementDsl
    def initialize(name, mod)
      @logger = Logger.get
      @module = mod
      @props = {}
      @props[:name] = name
    end

    # dsl methods

    def collect(&block)
      @props[:callback] = block
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
    def initialize
      @logger = Logger.get
      @props = {}
      @props[:measurements] = {}
      @props[:version] = ""
      @props[:author] = ""
      @props[:description] = ""
      @props[:interval] = 5
    end

    def [](key)
      @props[key]
    end

    def props
      @props
    end

    def name(v)
      @props[:name] = v
    end

    def version(v)
      @props[:version] = v
    end

    def description(v)
      @props[:description] = v
    end

    def measurement(name, &block)
      m = MeasurementDsl.new(name,self)
      m.instance_eval(&block)
      @props[:measurements][name] = m
    end
  end

  class Collector
    def initialize(file)
      @logger = Logger.get
      @logger.debug("Loading collector: #{file}")
      @wrapper = CollectorDsl.new
      @wrapper.instance_eval(File::read(file), file)
    end

    def collect
      obs = []
      @wrapper.props[:measurements].each_pair { |name,measDef|
        obs << Measurement.new(measDef, measDef.prop(:callback).call())
      }
      return obs
    end

    # Meta-data provided by the plugin

    def name
      @wrapper.prop(:name)
    end

    def description
      @wrapper[:description]
    end

    # Configured values

    def active?
      false
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
