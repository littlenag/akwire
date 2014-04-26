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
        @logger.info('loaded collector', {
                       :name => c.name,
                       :description => c.description
                     })
      end
    end

    def load_gems(gems)
      @collector_gems = gems
    end

    # collectors are either configured or not,
    # if configured they get a name and a config object
    # though if they have defaults for everything then they can be 'default' loaded
    def configure_from_settings(settings={})
      @instances = {}
      all_collectors.each do |collector|
        if collector.needs_config?
          if settings.has_key?(collector.name)
            collector.configure(settings[collector.name])
          else
            @logger.info('collector requires configuration', {
                           :collector => collector.name
                         })
          end
        else
          @instance[collector.name] = collectors
        end
      end
    end

    def collect_observations
      all_instances.select {|c| c.active? }.each do |instance|
        obs = instance.collect
        yield instance, obs
      end
    end

    def stop_all(&when_done)
      EM::Iterator.new(all_instances)
        .each(
              foreach = proc { |instance,iter|
                instance.stop {
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

    def all_instances
      @instances.values
    end

    def loaded(name, description)
      @logger.info('loaded collector', {
        :name => name,
        :description => description
      })
    end
  end

end
