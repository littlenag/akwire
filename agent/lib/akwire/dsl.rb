# Wraps up a zinc module

require 'logging'

module Zinc

  class Measurement
    include Logging

    def initialize(defn, value)
      @def = defn
      @value = Float(value)
    end

    def to_s
      "#{@def.mod.prop(:name)}/#{@def.prop(:name)} => #{@value}"
    end
  end

  class Report
    include Logging

    def initialize(defn, value)
      @def = defn
      @value = value.to_s
    end

    def to_s
      "#{@def.mod.prop(:name)}/#{@def.prop(:name)} => #{@value}"
    end
  end

#  class Event end

  class PatternDsl
    include Logging

    def initialize(name, mod)
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
    include Logging

    def initialize(name, mod)
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

  class ModuleDsl

    include Logging

    def initialize
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

  class Tmr
    include Logging

    def initialize(file)
      logger.debug("Loading module: #{file}")
      @module = ModuleDsl.new
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
      @dsl.prop(:name)
    end

    def to_s
      name
    end
  end
end
