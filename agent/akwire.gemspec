# -*- encoding: utf-8 -*-
require File.join(File.dirname(__FILE__), 'lib', 'akwire', 'constants')

Gem::Specification.new do |s|
  s.name        = 'akwire'
  s.version     = Akwire::VERSION
  s.platform    = Gem::Platform::RUBY
  s.authors     = ['Mark Kegel']
  s.email       = ['mark.kegel@gmail.com']
  s.homepage    = 'https://github.com/littlenag/akwire'
  s.summary     = 'A data collection framework'
  s.description = 'A data collection framework that aims to be simple, malleable, and scalable.'
  s.license     = 'MIT'
  s.has_rdoc    = false

  s.add_dependency('oj', '2.0.9')
  s.add_dependency('eventmachine', '1.0.3')
  s.add_dependency('amq-protocol', '1.9.2')
  s.add_dependency('amq-client', '1.0.2')
  s.add_dependency('amqp', '1.3.0')
  s.add_dependency('thin', '1.5.0')
  s.add_dependency('em-worker', '0.0.2')

  s.add_development_dependency('rake')
  s.add_development_dependency('rspec')
  s.add_development_dependency('debugger')

  s.files         = Dir.glob('{bin,lib}/**/*') + %w[akwire.gemspec README.md CHANGELOG.md MIT-LICENSE.txt]
  s.executables   = Dir.glob('bin/**/*').map { |file| File.basename(file) }
  s.require_paths = ['lib']
end
