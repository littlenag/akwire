gem 'em-worker', '0.0.2'

require 'timeout'
require 'em/worker'

module Akwire
  class IO
    class << self
      def popen(command, mode='r', timeout=nil, &block)
        block ||= Proc.new {}
        begin
          if RUBY_VERSION < '1.9.3'
            child = ::IO.popen(command + ' 2>&1', mode)
            block.call(child)
            wait_on_process(child, false)
          else
            options = {
              :err => [:child, :out]
            }
            case RUBY_PLATFORM
            when /(ms|cyg|bcc)win|mingw|win32/
              shell = ['cmd', '/c']
              options[:new_pgroup] = true
            else
              shell = ['sh', '-c']
              options[:pgroup] = true
            end
            child = ::IO.popen(shell + [command, options], mode)
            if timeout
              Timeout.timeout(timeout) do
                block.call(child)
                wait_on_process(child)
              end
            else
              block.call(child)
              wait_on_process(child)
            end
          end
        rescue Timeout::Error
          kill_process_group(child.pid)
          wait_on_process_group(child.pid)
          ['Execution timed out', 2]
        rescue => error
          kill_process_group(child.pid)
          wait_on_process_group(child.pid)
          raise error
        end
      end

      def async_popen(command, data=nil, timeout=nil, &block)
        execute = Proc.new do
          begin
            popen(command, 'r+', timeout) do |child|
              unless data.nil?
                child.write(data.to_s)
              end
              child.close_write
            end
          rescue => error
            [error.to_s, 2]
          end
        end
        complete = Proc.new do |output, status|
          block.call(output, status) if block
        end
        @async_popen_worker ||= EM::Worker.new
        @async_popen_worker.enqueue(execute, complete)
      end

      private

      def kill_process_group(group_id)
        begin
          ::Process.kill(9, -group_id)
        rescue Errno::ESRCH, Errno::EPERM
        end
      end

      def wait_on_process_group(group_id)
        begin
          loop do
            ::Process.wait2(-group_id)
          end
        rescue Errno::ECHILD
        end
      end

      def wait_on_process(process, wait_on_group=true)
        output = process.read
        _, status = ::Process.wait2(process.pid)
        if wait_on_group
          wait_on_process_group(process.pid)
        end
        [output, status.exitstatus || 2]
      end
    end
  end
end
