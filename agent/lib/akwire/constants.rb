module Akwire
  unless defined?(Akwire::VERSION)
    VERSION = '0.0.1'

    SETTINGS_CATEGORIES = [:collectors]

    LOG_LEVELS = [:debug, :info, :warn, :error, :fatal]

    SEVERITIES = %w[ok warning critical unknown]

    STOP_SIGNALS = %w[INT TERM]
  end
end
