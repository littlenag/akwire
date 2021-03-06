require File.dirname(__FILE__) + '/../lib/akwire/utilities.rb'

describe 'Akwire::Utilities' do
  include Akwire::Utilities

  it 'can redact sensitive info from a hash' do
    hash = {
      :one => 1,
      :password => 'foo',
      :nested => {
        :password => 'bar'
      },
      :diff_one => [nil, {:secret => 'baz'}],
      :diff_two => [{:secret => 'baz'}, {:secret => 'qux'}]
    }
    redacted = redact_sensitive(hash)
    redacted[:one].should eq(1)
    redacted[:password].should eq('REDACTED')
    redacted[:nested][:password].should eq('REDACTED')
    redacted[:diff_one][1][:secret].should eq('REDACTED')
    redacted[:diff_two][0][:secret].should eq('REDACTED')
    redacted[:diff_two][1][:secret].should eq('REDACTED')
  end
end
