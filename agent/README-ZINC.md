Zinc
======

Ideas
=====

Features:
 - autogen module reports

Web UI for zinc:
 - enable/disable modules or streams
 - change intervals for each module/stream
 - see docs about a given module
 - define aggregation functions for a stream
 - define alerting rules on the monitored system
 - simple in-memory graphs
 - buffering policy
 - change backends
 - install new modules
 - upload new modules
 - configure a backend
 - change a module between push and pull modes
 - call modules 'coins', 'small change', 'pennies', copper with a zinc core?
 - foo.coin

Zinc
 - should support both push and pull modes for each module
 - push should support multiple formats
 - pull would be RESTful, over SSL with role auth

Data types: 
 - measurement
 - report 
 - event

Still need to be able to speak:
 - SNMP
 - JMX
 - nagios
 - shell over ssh
 - NRPE for remote nagios
 - Zabbix agent for remote zabbix

Backends:
 - zeromq
 - thrift
 - http/s

rule types
 - simple threshold
 - time-based threshold
 - count-based threshold
 - event promotion
 - report diff rule
 - time-based multi-stream
 - count-based mult-stream

stream info to keep around
 - units
 - tags
 - alerting rules whose pattern's match
 - first seen, last seen
 - count of times received
 - count of "outages", and when
 - last 4-5 values
 - estimate of periodicty, changes and when
 - how many times charted

alerts to situations
 - either active or archived
 - if active, then 
   - new
   - resolved
   - acknowledged
   - ignored/suppressed (a user ignores a situations, suppresses a stream)

severity/impact - what is the impact?
criticality/urgency - how soon should it be fixed?
priority - set by the user
 - it may not actually make much sense for raw events to have these later two fields

ITIL
 - event
 - alert
 - incident
 - problem

anomoly detection (see etsy's skyline)

CEP via esper

OWL, semantic web, enrichment

data protocol
 - remote browsing, diff detection
 - forwarding rules
 - heartbeat 
 - SSL
 - remotely update rules, create roles, etc
 - configure consolidation functions for data
 - auto-connect, the one that get's configured first is the client
 - smart-handshake, allow all peers and keep their instance id
 - scaling and HA stories as well

esper

 - expose the full power of esper

 - for ALERTING queries, so long they return Observation objects then
   each row can become a new alert, with the Observations inside each
   row returned as belonging to some alert trigger set

 - could have defaults/templates that include both the alerting and
   the clearing queries

 - other more basic data queries could run separately

 - scale and load balance

 - require rules to have a context

UI
 - users and groups
 - roles and views
 - wiki, embed xwiki if possible
 - cmdb, CI (configuration item)
 - could have a headless version for servers?
 - desktop version embedded with R?

4-tuple
 - cluster/instance
 - host
 - observer
 - metric/key

metrics-as-a-service
 - could support tenant's

puppet/chef integration

still more ideas

sudo:

http://docs.puppetlabs.com/puppet/2.7/reference/modules_publishing.html
> if a script require privileged execution then you would list the exact binaries to call here

> or if you want to run as a different user (say the mysql user) then you might specify that here

require package:

> list packages that must/should be installed in order for the script to function

essenses:
 - hardware : mac book pro
 - software : os x 10.6-9
 - service  : mongodb

> should be able to specialize certain metrics to individual platforms

==

some way to declare configuration parameters, kind of the same way
that a nagios script would, in fact would want to be able to pass
these in from the 'mon' command line.
 - print out help docs
 => would want to be able to get conf from a local file 
 => or via thrift a way to grab from a central server

want some kind of way to group metrics
 - share activation

cache_every_cycle { }

pattern

measurement
 type (Gauge, Counter, Rate, Delta)
 units
 name
 desc
 collect/optional

report

events may have to work differently
