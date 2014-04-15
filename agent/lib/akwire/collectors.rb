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
    end

    def load_gems(val) 
      @collector_gems = val
    end

    def load_all
      Dir[@collector_dir + "/**/main.mon"].each do |file|
        c = Collector.new(file)
        @collectors[c.name] = c
        @logger.info('loaded collector', {
                       :name => c.name,
                       :description => c.description
                     })
      end
        
    end

    def configure_from_settings(settings={})
      all_collectors.each do |collector|
        if collector.needs_config?
          if settings.has_key?(collector.name)
            collector.configure(settings[collector.name])
          else
            @logger.info('collector requires configuration', {
                           :collector => collector.name
                         })
          end
        end
      end
    end

    def collect_observations
      all_collectors.select {|c| c.active? }.each do |collector|
        obs = collector.collect
        yield collector, obs
      end
    end

    def stop_all(&block)
      collectors = all_collectors
      stopper = Proc.new do |collector|
        if collector.nil?
          block.call
        else
          collector.stop do
            stopper.call(collector.pop)
          end
        end
      end
      stopper.call(collectors.pop)
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
