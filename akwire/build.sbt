name := """akwire"""

version := "0.2-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.5"

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
  "org.scaldi" %% "scaldi" % "0.5.4",
  "org.scaldi" %% "scaldi-play" % "0.5.4",
  "org.scaldi" %% "scaldi-akka" % "0.5.4",
  // For user authentication and sign in
  "ws.securesocial" %% "securesocial" % "3.0-M1"
)

/** WEBJARS (client-side dependencies) **/
libraryDependencies ++= Seq(
  "org.webjars" %% "webjars-play" % "2.3.0-2",
  "org.webjars.bower" % "angular" % "1.3.14",
  "org.webjars.bower" % "angular-resource" % "1.3.14",
  "org.webjars.bower" % "angular-cookies" % "1.3.14",
  "org.webjars.bower" % "angular-sanitize" % "1.3.14",
  "org.webjars.bower" % "angular-route" % "1.3.14",
  "org.webjars.bower" % "angular-bootstrap" % "0.12.1",
  "org.webjars.bower" % "bootstrap" % "3.3.4",
  "org.webjars.bower" % "ng-table" % "0.5.4"
)

