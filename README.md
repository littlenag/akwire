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

plugins should have an optional UI component that can be dropped into the server dashboard
 - list their configuration options
 - provide an intelligent UI with better JavaScript

plugins are the heart and soul of the system in that they collect every form of data:
 - HTTP
 - TCP/SYN
 - ICMP
 - File
 - Socket
 - JMX
 - SSH
 - WMI

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
