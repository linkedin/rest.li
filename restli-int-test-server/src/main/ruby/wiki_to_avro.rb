#! /usr/bin/ruby

File.open(ARGV[0]).each do |line|
  cols = line.split("|")
  name = cols[1].strip unless cols[1].nil?
  type = cols[2].strip unless cols[2].nil?
  doc = cols[3].strip unless cols[3].nil?
  puts %Q!{"name": "#{name}", "type": "#{type}", doc="#{doc}"},!
end
