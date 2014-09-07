;
;
; From common.clj
;
;

(ns akwire.common
  (:use [clojure.string :only [split join]]
        [riemann.time :only [unix-time]]
        clojure.math.numeric-tower))

(defrecord Event [network host observer key value tags time])

(defprotocol Match
  (match [pred object]
    "Does predicate describe object?"))

(defn re-matches?
  "Does the given regex match string? Nil if string is nil."
  [re string]
  (when string
    (re-find re string)))

(defn map-matches?
  "Does the given map pattern match obj?"
  [pat obj]
    (every? (fn [[k v]] (match v (get obj k))) pat))

; Matching
(extend-protocol Match
  ; Regexes are matched against strings.
  java.util.regex.Pattern
  (match [re string]
         (try (re-find re string)
           (catch NullPointerException _ false)
           (catch ClassCastException _ false)))

  ; Functions are called with the given object.
  java.util.concurrent.Callable
  (match [f obj]
         (f obj))

  ; Map types
  clojure.lang.PersistentArrayMap
  (match [pat obj] (map-matches? pat obj))

  clojure.lang.PersistentHashMap
  (match [pat obj] (map-matches? pat obj))

  clojure.lang.PersistentTreeMap
  (match [pat obj] (map-matches? pat obj))

  ; Falls back to object equality
  java.lang.Object
  (match [pred object]
    (= pred object))

  ; Nils match nils only.
  nil
  (match [_ object]
    (nil? object)))

(defn event
  "Create a new event from a map."
  [opts]
  (let [t (long (round (or (opts :time)
                           (unix-time))))]
    (map->Event (merge opts {:time t}))))

(defn exception->event
  "Creates an event from an Exception."
  [^Throwable e]
  ; TODO might need a more descriptive record type
  (map->Event {:time (unix-time)
               :host "localhost"
               :observer "akwire"
               :key "exception"
               :tags ["exception" (.getName (class e))]
               :description (str e "\n\n"
                                 (join "\n" (.getStackTrace e)))}))

;
;
; From streams.clj
;
;

(ns akwire.streams
  (:use [akwire.common :exclude [match]])
  (:import (services.ObsProcesser)
           (services.TriggerCallback)
           (services.TriggerAlert$)
           ))

(defn warn [ex & children]
  (prn (str "WARNING:") ex children)
)

(def ^:dynamic *exception-stream*
  "When an exception is caught, it's converted to an event and sent here."
  nil)

(defmacro call-rescue
  "Call each child stream with event, in order. Rescues and logs any failure."
  [event children]
  `(do
     (doseq [child# ~children]
       (try
         (child# ~event)
         (catch Throwable e#
           (warn e# (str child# " threw"))
           (if-let [ex-stream# *exception-stream*]
             (ex-stream# (exception->event e#))))))
     ; TODO: Why return true?
     true))

(defmacro exception-stream
  "Catches exceptions, converts them to events, and sends those events to a
  special exception stream.

  (exception-stream (email \"polito@vonbraun.com\")
    (async-queue! :graphite {:core-pool-size 128}
      graph))

  Streams often take multiple children and send an event to each using
  call-rescue. Call-rescue will rescue any exception thrown by a child stream,
  log it, and move on to the next child stream, so that a failure in one child
  won't prevent others from executing.

  Exceptions binds a dynamically scoped thread-local variable
  *exception-stream*. When call-rescue encounters an exception, it will *also*
  route the error to this exception stream. When switching threads (e.g. when
  using an executor or Thread), you
  must use (bound-fn) to preserve this binding.

  This is a little more complex than you might think, because we *not only*
  need to bind this variable during the runtime execution of child streams, but
  *also* during the evaluation of the child streams themselves, e.g. at the
  invocation time of exceptions itself. If we write

  (exception-stream (email ...)
    (rate 5 index))

  then (rate), when invoked, might need access to this variable immediately.
  Therefore, this macro binds *exception-stream* twice: one when evaluating
  children, and again, every time the returned stream is invoked."
  [exception-stream & children]
  `(let [ex-stream# ~exception-stream
         children#  (binding [*exception-stream* ex-stream#]
                      (list ~@children))]
     (fn stream# [event#]
       (binding [*exception-stream* ex-stream#]
         (call-rescue event# children#)))))

(defn bit-bucket
  "Discards arguments."
  [args])

(defn- where-test [k v]
  ;(prn "where test" k v)
  (condp some [k]
    ; Tagged checks that v is a member of tags.
    #{'tagged 'tagged-all} (list 'when (list :tags 'event)
                             (list `tagged-all? (list 'flatten [v]) 'event))
    #{'tagged-any} (list 'when (list :tags 'event)
                     (list `tagged-any? (list 'flatten [v]) 'event))
    ; Otherwise, match.
    (list 'akwire.common/match v (list (keyword k) 'event))))

; Hack hack hack hack
(defn where-rewrite
  "Rewrites lists recursively. Replaces (metric x y z) with a test matching
  (:metric event) to any of x, y, or z, either by = or re-find. Replaces any
  other instance of metric with (:metric event). Does the same for host,
  service, event, state, time, ttl, tags (which performs an exact match of the
  tag vector), tagged (which checks to see if the given tag is present at all),
  metric_f, and description."
  [expr]
  (let [syms #{'network
               'host
               'observer
               'key
               'value
               'value_f
               'time
               'tags
               'tagged
               'tagged-all
               'tagged-any}]
    (if (list? expr)
      ; This is a list.
      (if (syms (first expr))
        ; list starting with a magic symbol
        (let [[field & values] expr]
          (if (= 1 (count values))
            ; Match one value
            (where-test field (first values))
            ; Any of the values
            (concat '(or) (map (fn [value] (where-test field value)) values))))

        ; Other list
        (map where-rewrite expr))

      ; Not a list
      (if (syms expr)
        ; Expr *is* a magic sym
        (list (keyword expr) 'event)
        expr))))

(defn where-partition-clauses
  "Given expressions like (a (else b) c (else d)), returns [[a c] [b d]]"
  [exprs]
  (map vec
       ((juxt remove
              (comp (partial mapcat rest) filter))
          (fn [expr]
            (when (list? expr)
              (= 'else (first expr))))
          exprs)))

(defmacro where*
  "A simpler, less magical variant of (where). Instead of binding symbols in
  the context of an expression, where* takes a function which takes an event.
  When (f event) is truthy, passes event to children--and otherwise, passes
  event to (else ...) children. For example:

  (where* (fn [e] (< 2 (:metric e))) prn)

  (where* expired?
    (partial prn \"Expired\")
    (else
      (partial prn \"Not expired!\")))"
  [f & children]
  (let [[true-kids else-kids] (where-partition-clauses children)]
    `(let [true-kids# ~true-kids
           else-kids# ~else-kids]
      (fn stream# [event#]
         (let [value# (~f event#)]
           (if value#
             (call-rescue event# true-kids#)
             (call-rescue event# else-kids#))
           value#)))))

(defmacro where
  "Passes on events where expr is true. Expr is rewritten using where-rewrite.
  'event is bound to the event under consideration. Examples:

  ; Match any event where metric is either 1, 2, 3, or 4.
  (where (metric 1 2 3 4) ...)

  ; Match a event where the metric is negative AND the state is ok.
  (where (and (> 0 metric)
              (state \"ok\")) ...)

  ; Match a event where the host begins with web
  (where (host #\"^web\") ...)


  ; Match an event where the service is in a set of services
  (where (service #{\"service-foo\" \"service-bar\"}) ...)
  ; which is equivalent to
  (where (service \"service-foo\" \"service-bar\") ...)

  If a child begins with (else ...), the else's body is executed when expr is
  false. For instance:

  (where (service \"www\")
    (notify-www-team)
    (else
      (notify-misc-team)))

  The streams generated by (where) return the value of expr: truthy if expr
  matched the given event, and falsey otherwise. This means (where (metric 5))
  tests events and returns true if their metric is five."
  [expr & children]
  (let [p (where-rewrite expr)
        [true-kids else-kids] (where-partition-clauses children)]
    `(let [true-kids# ~true-kids
           else-kids# ~else-kids]
       (fn stream [event#]
         (let [value# (let [~'event event#] ~p)]
           (if value#
             (call-rescue event# true-kids#)
             (call-rescue event# else-kids#))
           value#)))))

; My stuff

(defn make-event
  "Converts a models.core.Observation to an Event struct."
  [obs]
  (if (instance? models.core.ObservedMeasurement obs)
    ; (defrecord Event [host observer key value tags time])
    (akwire.common.Event. (.instance obs) (.host obs) (.observer obs) (.key obs) (.value obs) "" (.timestamp obs))
    (throw (Exception. "Expected an instace of models.core.Observation"))
  )
)

(defn make-obs
  "Converts a record of akwire.common.Event to a models.core.Observation."
  [event]
  (if (instance? akwire.common.Event event)
    ; (defrecord Event [network host observer key value tags time])
    (models.core.ObservedMeasurement. (:time event) (:network event) (:host event) (:observer event) (:key event) (:value event))
    (throw (Exception. "Expected an instace of akwire.common.Event"))
  )
)

(defn trigger
  "Triggers a new alert from the passed in event(s). trigger is a terminal function
  and does not pass on events to any child streams.

  Example:
  (where (and (host \"www\") (> value 10))
    (trigger)
    (else
      (resolve)))

  In the case of multi-stream rules, you may have multiple observations that are responsible for triggering
  the alert condition so it makes the most sense to pass all of these forward,
  rather than just the last observation that trips the alert."
  [events]
  (prn "alert triggered")
  (prn events)
  (if (list? events)
    (.trigger user/akwire-binding-trigger (java.util.ArrayList. (map make-obs events)))
    (.trigger user/akwire-binding-trigger (java.util.ArrayList. (map make-obs [events])))
  )
)
