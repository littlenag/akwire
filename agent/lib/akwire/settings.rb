module Akwire
  class Settings
    include Utilities

    attr_reader :indifferent_access, :loaded_env, :loaded_files

    def initialize
      @logger = Logger.get
      @settings = Hash.new
      SETTINGS_CATEGORIES.each do |category|
        @settings[category] = Hash.new
      end
      @indifferent_access = false
      @loaded_env = false
      @loaded_files = Array.new
    end

    def indifferent_access!
      @settings = with_indifferent_access(@settings)
      @indifferent_access = true
    end

    def to_hash
      unless @indifferent_access
        indifferent_access!
      end
      @settings
    end

    def [](key)
      to_hash[key]
    end

    SETTINGS_CATEGORIES.each do |category|
      define_method(category) do
        @settings[category].map do |name, details|
          details.merge(:name => name.to_s)
        end
      end

      type = category.to_s.chop

      define_method((type + '_exists?').to_sym) do |name|
        @settings[category].has_key?(name.to_sym)
      end

      define_method(('invalid_' + type).to_sym) do |details, reason|
        invalid(reason, {
          type => details
        })
      end
    end

    def load_env
      if ENV['RABBITMQ_URL']
        @settings[:rabbitmq] = ENV['RABBITMQ_URL']
        @logger.warn('using rabbitmq url environment variable', {
          :rabbitmq_url => ENV['RABBITMQ_URL']
        })
      end
      ENV['REDIS_URL'] ||= ENV['REDISTOGO_URL']
      if ENV['REDIS_URL']
        @settings[:redis] = ENV['REDIS_URL']
        @logger.warn('using redis url environment variable', {
          :redis_url => ENV['REDIS_URL']
        })
      end
      ENV['API_PORT'] ||= ENV['PORT']
      if ENV['API_PORT']
        @settings[:api] ||= Hash.new
        @settings[:api][:port] = ENV['API_PORT']
        @logger.warn('using api port environment variable', {
          :api_port => ENV['API_PORT']
        })
      end
      @indifferent_access = false
      @loaded_env = true
    end

    def load_file(file)
      @logger.debug('loading config file', {
        :config_file => file
      })
      if File.file?(file) && File.readable?(file)
        begin
          contents = File.open(file, 'r').read
          config = Oj.load(contents, :mode => :strict)
          merged = deep_merge(@settings, config)
          unless @loaded_files.empty?
            changes = deep_diff(@settings, merged)
            @logger.warn('config file applied changes', {
              :config_file => file,
              :changes => redact_sensitive(changes)
            })
          end
          @settings = merged
          @indifferent_access = false
          @loaded_files << file
        rescue Oj::ParseError => error
          @logger.error('config file must be valid json', {
            :config_file => file,
            :error => error.to_s
          })
          @logger.warn('ignoring config file', {
            :config_file => file
          })
        end
      else
        @logger.error('config file does not exist or is not readable', {
          :config_file => file
        })
        @logger.warn('ignoring config file', {
          :config_file => file
        })
      end
    end

    def load_directory(directory)
      path = directory.gsub(/\\(?=\S)/, '/')
      Dir.glob(File.join(path, '**/*.json')).each do |file|
        load_file(file)
      end
    end

    def set_env
      ENV['AKWIRE_CONFIG_FILES'] = @loaded_files.join(':')
    end

    def validate
      @logger.debug('validating settings')
      SETTINGS_CATEGORIES.each do |category|
        unless @settings[category].is_a?(Hash)
          invalid(category.to_s + ' must be a hash')
        end
        send(category).each do |details|
          send(('validate_' + category.to_s.chop).to_sym, details)
        end
      end
      case File.basename($0)
      when 'akwire-daemon'
        validate_daemon
      when 'rspec'
        validate_daemon
      end
      @logger.debug('settings are valid')
    end

    private

    def invalid(reason, data={})
      @logger.fatal('invalid settings', {
        :reason => reason
      }.merge(data))
      @logger.fatal('AKWIRE NOT RUNNING!')
      exit 2
    end

    def validate_daemon
      unless @settings[:daemon].is_a?(Hash)
        invalid('missing daemon configuration')
      end
      unless @settings[:daemon][:id] =~ /^[\w\.-]+$/
        invalid('daemon must have a unique id and it cannot contain spaces or special characters')
      end
      if @settings[:daemon].has_key?(:keepalive)
        unless @settings[:daemon][:keepalive].is_a?(Hash)
          invalid('client keepalive must be a hash')
        end
      end
    end

  end
end
