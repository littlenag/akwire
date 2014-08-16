Modern Web Template
===========

**AngularJS - Scala - Play - Guice - PlayReactiveMongo**

A full application stack for a Modern Web application, lets review the components:

* **AngularJS** - client side javascript framework for creating complex MVC applications in Javascript,
fronted with Twitter bootstrap CSS framework, because well, im not a web designer.
  * Take a look at what the google cool kids are upto here : [AngularJS](http://angularjs.org/)

* **Bootstrap** - Bootstrap components written in pure AngularJS
  *  [http://angular-ui.github.io/bootstrap/](http://angular-ui.github.io/bootstrap/)

* **PlayFramework** - currently using 2.2.1 with the scala API
  *  [PlayFramework Docs](http://www.playframework.com/documentation/2.2.x/Home)

* **Guice** integration for Dependency injection,
  * Special thanks to the typesafehub team for their activator : [Play-Guice](http://www.typesafe.com/activator/template/play-guice)

* **PlayReactiveMongo** gives interaction with MongoDB providing a non-blocking driver as well as some useful additions for handling JSON.
  * Check out their GitHub: [Play-ReactiveMongo](https://github.com/ReactiveMongo/Play-ReactiveMongo)



Getting Started
----------

Your development environment will require:
*  SBT / Play see [here]() for installation instructions.
*  MongoDB see [here]() for installation instructions.

Once the prerequisites have been installed, you will be able to execute the following from a terminal.

```
../modern-web-template >  play run
```

This should fetch all the dependencies and start a Web Server listening on *localhost:9000*

```
[info] Loading project definition from ../modern-web-template/project
[info] Set current project to modern-web-template
[info] Updating modern-web-template...
...
[info] Done updating.

--- (Running the application from SBT, auto-reloading is enabled) ---

[info] play - Listening for HTTP on /0:0:0:0:0:0:0:0:9000

(Server started, use Ctrl+D to stop and go back to the console...)

```

Note: This will create a MongoDB Collection for you automatically, a free-be from the Driver! 

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
 - phone
 - sms
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
otherwise drop


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
otherwise drop



schedule 'on-call':

during(Monday to Friday) phone user(Mark)
during(Weekends) phone user(Jason)

#alert policy(`on call`)

#during(8am to 9pm) notify [team!phone, team!sms, user(`Mike`)!phone, rollup(2h)]

should be able to have a 
 - list of action + target pairs
 - an action + list of targets
