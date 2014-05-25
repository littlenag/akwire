module Akwire
  class Collectors
    def initialize
      @logger = Logger.get
      @collectors = Hash.new
      @collector_dir = nil
      @collector_gems = false
    end

    def [](key)
      @collectors[key.to_sym]
    end

    def keys
      @collectors.keys
    end

    def instances
      i = []
      @collectors.keys.each { |name, collector|
        i << collector.instances
      }
      i
    end

    def collector_exists?(name)
      @collectors.has_key?(name.to_sym)
    end

    def load_directory(dir)
      @collector_dir = dir
      Dir[@collector_dir + "/**/main.mon"].each do |file|
        c = Collector.new(file)
        @collectors[c.name.to_sym] = c
        @logger.info('collector loaded', {:name => c.name})
      end
    end

    def load_gems(gems)
      @collector_gems = gems
    end

    # collectors are always configured, but their configuration may be {}
    # if configured they must be assigned a name and a config object
    # though if they have defaults for everything then they can be 'default' loaded
    def load_instances(collector_settings={})
      collector_settings.each do |collector_name,plugin_settings|
        unless collector_exists?(collector_name)
          @logger.warn('no collector found for configuration', {
                         :collector => collector_name,
                         :settings => plugin_settings
                       })
          next
        end

        if plugin_settings[:mode].nil?
          # Treat as instances
          plugin_settings.each do |instance_name, instance_settings|
            self[collector_name].configure_instance(instance_name, instance_settings)
          end
        else
          # Treat as singleton
          self[collector_name].configure_instance(nil, plugin_settings)
        end

      end

      # default load singleton and defaultable collectors, but only if autoload is set
      if collector_settings[:autoload]
        all_collectors.each do |collector|
          unless collector.configured?
            collector.configure_instance(nil,nil)
          end
        end
      end

      # remove the ones lacking a config
      all_collectors.keep_if do |collector| 
        if collector.configured?
          true
        else
          @logger.warn("dropping unconfigured collector: #{collector}")
          false
        end
      end
      
    end

    def collect_observations
      all_collectors.each do |collector|
        obs = collector.collect
        yield collector, obs
      end
    end

    def stop_all(&when_done)
      EM::Iterator.new(all_collectors)
        .each(
              foreach = proc { |collector,iter|
                collector.stop_all {
                  @logger.warn("stopped collector: #{collector}")
                  iter.next
                }
              }, 
              after = proc { |results|
                @logger.warn("stopped all collectors")
                when_done.call if block_given?
              })
    end
    
    private

    def all_collectors
      @collectors.values
    end

    def loaded(name, description)
      @logger.info('loaded collector', {
        :name => name,
        :description => description
      })
    end
  end

end
