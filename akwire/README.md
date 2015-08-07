Akwire Monitoring Framework
===========================

**AngularJS - Scala - Play - Scaldi - Salat**

A full monitoring stack for the modern infrastructure, lets review the components:

* **AngularJS** - client side javascript framework for creating complex MVC applications in Javascript,
fronted with Twitter bootstrap CSS framework, because well, im not a web designer.
  * Take a look at what the google cool kids are upto here : [AngularJS](http://angularjs.org/)

* **PlayFramework** - currently using 2.2.1 with the scala API
  *  [PlayFramework Docs](http://www.playframework.com/documentation/2.2.x/Home)


Dev Commands
============

curl -HContent-type:application/json -XPOST -d @test/json/observations/m1.json http://localhost:9000/ingest/observations

Design
======

Every monitoring system needs to consider the following data flow:

ICAP
 - instrument - make data available to be collected (e.g. jmx/jolokia, coda hale's metrics)
 - collect    - collect the data (optional, ala collectd)
 - analyze    - analyze the data (optional, ala graphite)
 - persist    - persist the data (optional, ala riemann)

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

Akwire is split into two separate logical pieces:
 - the alerting engine
 - the notification engine

The job of the alerting engine (AE) is to determine when something is
wrong and needs attention. The job of the notification engine (NE) is
to get the attention of the most appropriate person.

Alerting Engines are overly represented in monitoring software and is
what most pieces of monitoring software are. An AE does its job by
producing a stream of Alerts, scoped to th particular object in need
of attention.

Rules that belong to Teams are either submitted to:
 - to the Enterprise Alert Bus, to be filtered by Services (push)
 - left to the team, to be displayed on their Team Incident Dashboard,
   and handled by their default notification policy

Services have a set of filtering rules that subscribe/pull relevant Incidents
 => for each Incident pull, it is submitted to that Services
    notification Policy and displayed on the Enterprise Incident
    Dashboard, which is then acted on by Tier 1 and 2
 => assumes that Rules doesn't always know the exent of their impact

Rules belonging to Users are not submittable to the Enterprise.
 => Team stuff should usually be open to the enterprise
 => User stuff is mostly noise
 
Notification Policies
=====================

----

filter action [target ...] | filter {filter action target} other { action target }

someway to perform rollups

filters:
 - during/when
 - sev
 - tag
 - host/device
 - observer
 - key

services filters:
 - team/owner
 - environment

action keywords:
 - assign (available for services? teams to?)
 - notify (pass incident to the target's policy)
 - page (invoke generic highest-level notification)
 - call
 - text
 - email
 - alert
 > want 'text' and 'call' as reserved words probably, use backticks for escaping names e.g. user `key`

targets:
 - team
 - user(name of user)
 - name of policy
 - alert/notify as generic keyword in case you pass a action type to a policy
 - rollup : generic target? team!email, team!sms
 - want an on-call schedule?

date time ranges and patterns
 - day of week
 - hours in day
 > want to check for gaps in schedule
 > have to default to team timezone!
 > short-hand for daily rotation, weekly rotation, layers

probably want short=hand ways of daying something is a schedule, or a
policy, that way can have viz present as a list of rule or as a
calendar

if a target can't be found always notify the default policy or target

want everything to be "complete", that is no gaps in rules, rules
followed one-by-one, use otherwise statement for intentional gaps,
error if no gaps but use otherwise anyway

probably want some way of defining escalation and reminders, as well

'wait', 'repeat', 'after' as escalation keywords?

may want some way to test if a notice has already been sent

logical OR, AND filters together

'elsewise' and 'otherwise' ? default match keywords

drop/ignore, ticket, and log as builtin targets?

send to the team email, vs send email to all members of a team, vs send email to an individual member

integrations may need to hook into ack/resolve messages coming back
from users, one way sync from akwire to external tool because
basically saying that all state is managed in the external utility

what about flapping? rate limiting?

Automagic expansions:
Weekdays -> Monday to Friday
Weekends -> Saturday to Sunday

----

buy number from twilio ((323) 672-4363)
if you call the number, you get
 - automatic recording of any current incidents for you (team first, then user)
 - dial 0
   - prompt to get to contact directory, transfer to those numbers
     (good when you don't know everyone's number)
     (could do the same from an app)

----

examples:

----

policy default:

email team then    # no filter means always match, then keyword allows chaining of policies, each policy runs to completion
sev(1) {
  during(Monday to Friday) phone user(Mark)
  during(Weekends) phone user(Jason)
}
sev(2) sms team


----

policy default:

email team then    # no filter means always match, then keyword allows chaining of policies, each policy runs to completion
sev(1) {
  notify policy(`on call`)
}
sev(2) {
  sms team
  email team
}
sev(3) next to retry / stop to give up / halt to quit entirely

----

email team    # no filter means always match, then keyword allows chaining of policies, each policy runs to completion
sev {
  1 => notify policy(`on call`)
  2 => { sms team
         email team
       }
  _ => drop
}

----

schedule 'on-call':

during(Monday to Friday) phone user(Mark)
during(Weekends) phone user(Jason)

#alert policy(`on call`)

#during(8am to 9pm) notify [team!phone, team!sms, user(`Mike`)!phone, rollup(2h)]

should be able to have a 
 - list of action + target pairs
 - an action + list of targets

-------------------------------------------

filter statements should have two styles:
 - case/match
 - if_expr

probably start with if_expr

-------------------------------------------

filter action target

'notify' keyword, to invoke the target's policy

'page' keyword to choose most urgent method

policies vs schedules

pagerduty ALWAYS passes the incident/alert to the target's policy

we'll have policies at the, levels of:
 - service
 - team
 - user

need notions of the 'this' team; 'this' user; self target
 - 'me' for user
 - 'crew' for team?
 - 'enterprise'	for service?

write rules about incidents, to remind user if open too long

[ email user("it@lqdt.com"), email user("ai@lqdt.com"), notify schedule("ai/net on-call")];
delay 1h;
[ email user("asad@lqdt.com"),  user("carlos@lqdt.com"), invoke_schedule("ai/net on-call")];
delay 30m;

---

notify [ user("it@lqdt.com"), user("ai@lqdt.com"), schedule("ai/net on-call")]
delay 1h
notify [ user("asad@lqdt.com"), user("carlos@lqdt.com"), schedule("ai/net on-call")]
delay 30m
repeat 5 times

repeat until ...
 - acked

rollup/batch
 - could be a property of a channel, with the script just handling routing

channels in the akwire sense are global; we might need the notion of a conduit, which would
let messages on a particular channel be guided along a particular path
 => e.g. choosing one email server over another
 => sns region might be property of an SNS conduit

---

in-scope vars:
 - incident
 - me
 - user(id)
 - team
 - schedule(id)
 - different policy/script


functions:
 - wait(duration)
 - wait_until(time_pattern)
 - notify(entity)
 - page(entity)
 - send(channel:C,target:C#T)   // channel of type C, target of type C#T

receivers:
 - entities
 - channels+targets


if (incident.severity == IL_5) {
  wait(1h);
  notify(me);
  notify([me, user(2), schedule(14)]);  // done in parallel
} else {

}

---

jump to label instruction, list of labels

then process the program to remove the labels

---

policy ID =>

schedule ID =>

---

how to define rotations? overlays?

has tag, host, id

how to define reminders and rollups
 - reminders would deal with incidents entirely so it makes sense for incidents to be the primitive of choice
 
set on-trigger = 
set on-resolve, on-ack, on-suppress

need to be able to query for incidents to implement reminders

--------------------------------------------

Splunk/PRTG criteria

is Greater than
is Less than
is Equal to
is Not equal to
Drops by
Rises by

should have a way if detecting if an incident becomes stale,
for example with splunk there is no reason not to just resend the alert every 5 minutes, but in akwire it would detect that the triggers have stopped coming in every 5 minutes and then mark the incident as “stale”
 - more predictive rather than just resolving them if they’ve been open too long

--------------------------------------------

things you can use monitoring for:
 - Fault detection
 - anomaly detection
 - performance analysis
 - trending

things you can monitor:
 - servers
 - network
 - application logic
 - business logic

--------------------------------------------

Need to get true ITIL terms defined
 - severity or impact, urgency
 - event, alarm, alert, incident

--------------------------------------------

monitoring systems should have status dashboards and records of incidents, they are not a ticketing system, they should integrate with a ticketing system

--------------------------------------------

probably want to have a normalized severity model, with option types or path 
types for accessing the fields that would be present in other systems alerts

incident.severity for normalized severity

incident.source match {
 case source("nagios") => incident.severity
 
may either need path-dependent types or an Option type

--------------------------------------------

what if this were to be a typed lisp? then i could rewrite in clojure 

the clojure alerting rules need to look a lot more like jut rules

source | filters | @sink

what would happen if this were just re-written to a clojure program?
 - user will want to have logic like
 - for incidents like host = "h1" then notify me
 
i would want a quiet hours feature

 if between 12am and 4am then
 
 end

--------------------------------------------

this
(a < b) && c

over this
a < b && c
