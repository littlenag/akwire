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

curl -HContent-type:application/json -XPOST -d @test/json/observations/m1.json http://localhost:6565/ingest/observations

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

email team    # no filter means always match, then keyword allows chaining of policies, each policy runs to completion
sev(1) notify policy(`on call`)
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
 - 'team' for team?
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

rollup

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

