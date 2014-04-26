module Akwire
  class Collectors
    def initialize
      @logger = Logger.get
      @collectors = Hash.new
      @collector_dir = nil
      @collector_gems = false
    end

    def [](key)
      @collectors[key]
    end

    def collector_exists?(name)
      @collectors.has_key?(name)
    end

    def load_directory(dir)
      @collector_dir = dir
      Dir[@collector_dir + "/**/main.mon"].each do |file|
        c = Collector.new(file)
        @collectors[c.name] = c
        @logger.info('collector loaded', {:name => c.name})
      end
    end

    def load_gems(gems)
      @collector_gems = gems
    end

    # collectors are either configured or not,
    # if configured they get a name and a config object
    # though if they have defaults for everything then they can be 'default' loaded
    def load_instances(settings={})
      settings.each do |key,settings|
        collector_name, instance_name = key.to_s.split("!")
        if (collector_exists?(collector_name))
          self[collector_name].configure_instance(instance_name, settings)
        else
          @logger.warn('no collector found for configuration', {
                         :key => key,
                         :collector => collector_name,
                         :settings => settings
                       })
        end
      end

      # do we try to default load singleton or defaultable collectors?
      all_collectors.each do |collector|
        unless collector.configured?
          collector.configure_instance(nil,nil)
        end
      end

      # are any left unconfigured? remove them? do we care?
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
