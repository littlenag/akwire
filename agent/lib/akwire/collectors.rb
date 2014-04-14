module Akwire
  class Collectors
    def initialize
      @logger = Logger.get
      @collectors = Hash.new
    end

    def [](key)
      @collectors[key]
    end

    def collector_exists?(name)
      @collectors.has_key?(name)
    end

    def require_directory(directory)
      path = directory.gsub(/\\(?=\S)/, '/')
      Dir.glob(File.join(path, '**/*.rb')).each do |file|
        begin
          require File.expand_path(file)
        rescue ScriptError => error
          @logger.error('failed to require extension', {
            :extension_file => file,
            :error => error
          })
          @logger.warn('ignoring extension', {
            :extension_file => file
          })
        end
      end
    end

    def load_all
    end

    def load_settings(settings={})
      all_extensions.each do |extension|
        extension.settings = settings
      end
    end

    def stop_all(&block)
      extensions = all_extensions
      stopper = Proc.new do |extension|
        if extension.nil?
          block.call
        else
          extension.stop do
            stopper.call(extensions.pop)
          end
        end
      end
      stopper.call(extensions.pop)
    end

    private

    def all_extensions
      @collectors.flatten
    end

    def loaded(name, description)
      @logger.info('loaded collector', {
        :name => name,
        :description => description
      })
    end
  end

  module Module
    class Base
      attr_accessor :logger, :settings

      def initialize
        EM::next_tick do
          post_init
        end
      end

      def name
        'base'
      end

      def description
        'module description (change me)'
      end

      def definition
        {
          :type => 'module',
          :name => name
        }
      end

      def post_init
        true
      end

      def run(data=nil, &block)
        block.call('noop', 0)
      end

      def stop(&block)
        block.call
      end

      def [](key)
        definition[key.to_sym]
      end

      def has_key?(key)
        definition.has_key?(key.to_sym)
      end

      def safe_run(data=nil, &block)
        begin
          data ? run(data.dup, &block) : run(&block)
        rescue => error
          block.call(error.to_s, 2)
        end
      end

      def self.descendants
        ObjectSpace.each_object(Class).select do |klass|
          klass < self
        end
      end
    end

#    Object.const_set("module".capitalize, Class.new(Base))

  end
end
