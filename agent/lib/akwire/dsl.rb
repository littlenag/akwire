module Akwire
  class Measurement
    def initialize(defn, value)
      @logger = Logger.get
      @def = defn
      @value = Float(value)
    end

    def to_s
      "#{@def.mod.prop(:name)}/#{@def.prop(:name)} => #{@value}"
    end
  end

  class Report
    def initialize(defn, value)
      @logger = Logger.get
      @def = defn
      @value = value.to_s
    end

    def to_s
      "#{@def.mod.prop(:name)}/#{@def.prop(:name)} => #{@value}"
    end
  end

  class Status
    def initialize(defn, value)
      @logger = Logger.get
      @def = defn
      @value = value.to_s
    end

    def to_s
      "#{@def.mod.prop(:name)}/#{@def.prop(:name)} => #{@value}"
    end
  end

  class NagiosWrapper
    def initialize(defn, value)
      @logger = Logger.get
      @def = defn
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
    end

    def prop(p)
      @props[p]
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
      @module = CollectorDsl.new
      @module.instance_eval(File::read(file), file)
    end

    def collect
      obs = []
      @module.props[:measurements].each_pair { |name,measDef|
        obs << Measurement.new(measDef, measDef.prop(:callback).call())
      }
      return obs
    end

    def name
      @module.prop(:name)
    end

    def description
      @module.prop(:description)
    end

    def needs_config?
      false
    end

    def to_s
      name
    end
  end
end
