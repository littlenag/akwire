![akwire](https://raw.github.com/littlenag/akwire/master/akwire-logo.png)

A data collection system that aims to be simple, malleable, and scalable.

[![Build Status](https://secure.travis-ci.org/littlenag/akwire.png)](https://travis-ci.org/littlenag/akwire)

## Documentation
  Please refer to the [Akwire Docs](http://docs.akwireapp.org/).

## License
  Akwire is released under the [MIT license](https://raw.github.com/littlenag/akwire/master/MIT-LICENSE.txt).

## Server

  The Akwire server does these things:
  - exposes a REST API for external services to pull from
  - web-based management console the deployment from
  - server to manage the deployed agents:
    - sends keep-alives
    - registers/deregisters agents
    - pushes updated configurations
    - brokers data pull requests

Notes
 - want some secure way to handle credentials since many scripts have to login
 - maybe a password wallet of some kind?

=====

containers for observers
 - call these what?

do i use redis as a db or not?

types of observers:

 - stateless  : these require no configuration and just expose data (e.g. disk cpu stats), there is only ever one of these observer's loaded

 - configured : these require configuration (e.g. mongo stat taking a list of DBs), there is only ever one of these observer's loaded

 - instanced  : these require configuration and there may be more than one loaded at a time (e.g. detailed stats for a mongo collection)

one or more observers is managed by a container


=====

Ruby based agent
 - core daemons for the major platforms
  : windows
  : linux (centos 5/6, debian, ubuntu)
  : os x

 - core plugins for the major platforms

Troubleshooting:

How do I force data publish?

By default the agent should wait to register with the server before
publishing data. This ensure it pulls the latest config when it
starts. This is over riding via a command line flag
(--continue-without-registration).

====

these push/feed adapted or renormalized data from the agents/collectors to various local systems
 - graphite
 - splunk
 - syslog

and cloud services
 - copperegg?
 - datadog
 - librato
 - new relic?
 - circonus
 - pager duty?

also have to adapt for pull based systems:
 - nagios
 - prtg
 - zabbix
 - zenoss

====

periodically run code:
 - status checks
 - measurements
 - reports

aperiodically driven code:
 - events
 - logs
 - alert


notions that impact data types, semantics:
 - periodic vs aperiodic
 - structured vs unstructured
   - string vs numeric
   - object vs primitive
 - various notions of severity/impact
 - time sensitivity or priority 
 - interestingness to badness continuum

events usually high priority, logs low priority
events structured, logs usually unstructured (and cannot force structure on logs, just won't work)
events can be alarms/alerts, logs can have severity but are less actionable

want to support a hybrid active/passive plugin system
 - active are scheduled
 - passive are exposed via API


====

route notifications by:
 - severity/impact
 - priority/urgency
 - host/host-group
 - service/service-group
 - team/owner
 - tag
 - location

====

plugins should have an optional UI component that can be dropped into the server dashboard
 - list their configuration options
 - provide an intelligent UI with better JavaScript

plugins are the heart and soul of the system in that they collect every form of data:
 - HTTP(S)
 - TCP/SYN
 - ICMP
 - SNMP
 - File
 - Socket
 - JMX
 - SSH
 - WMI
 - NRPE
 - external: http put/post, smtp, snmp trap

Along with specialized plugins for gathering metrics from:

Platform:
 - mysql
 - mongodb
 - mule
 - activemq

OS:
 - Linux
 - Windows
 - OS X
 - BSD
 - Solaris

Daemons:
 - mail
 - pam
 - syslog
 - ntp
 - postfix

Windows Services:
 - exchange
 - AD
 - sharepoint

Networking:
 - Routers
 - Switches
 - Load Balancers
 - Voice

Security
 - Cameras
 - Lights
 - Badge Swipes

Storage:
 - NetApp

Virt:
 - vmware, vCenter

Environmental:
 - HVAC
 - PDU
 - Temp/Humidity


================

more notes

metric engine : {
  db/persistence : {
    charts, dashboards (i.e. visualizations)
    reports, trending
    broad analytics
  }
  
  alert engine : {
    incidents (persistant status dashboard)
    notifications (for waking folks up)
  }
}

this may have been the flow for metrics and events

alerts
incidents
notifications
—
want a management story for all three

Notification Routing
------
Team: 
level: App, hw, vm, net
Location

================

Change from embedding clojure/reimann to a more gattling like system
 - static scala files that we load the classes from
 - these classes can be instantiated to create rules
 - add a new -> new type of rule
 - deliver several rule types with the code, but allow new ones to be loaded
 - use akka streams instead of reimann
 - scala should use a dsl to expose parameters that might need configuration
 - might even want to create a simple dsl for expressing the stream logic


================

