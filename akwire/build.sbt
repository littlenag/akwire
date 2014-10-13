name := """akwire"""

version := "0.1-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.1"

/** JAVA DEPS **/
libraryDependencies ++= Seq(
  "javax.inject" % "javax.inject" % "1",
  "com.rabbitmq" % "amqp-client" % "3.3.0",
  "org.mockito" % "mockito-core" % "1.9.5" % "test"
)

/** SCALA APPLICATION DEPS **/
libraryDependencies ++= Seq(
  // MONGODB for persistence
  "org.mongodb" %% "casbah" % "2.7.3",
  "com.novus" %% "salat" % "1.9.9",
  // SCALDI for dependency injection
  "org.scaldi" %% "scaldi" % "0.4",
  "org.scaldi" %% "scaldi-play" % "0.4",
  "org.scaldi" %% "scaldi-akka" % "0.4",
  // For user authentication and sign in
  "ws.securesocial" %% "securesocial" % "3.0-M1"
)

/** CLOJURE DEPS (Using clojure and reimann for a rules engine (instead of esper)) **/
libraryDependencies ++= Seq(
  "org.clojure" % "clojure" % "1.6.0",
  "org.clojure" % "tools.logging" % "0.2.6",
  "org.clojure" % "math.numeric-tower" % "0.0.4"
)

