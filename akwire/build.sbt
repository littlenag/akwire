import play.PlayImport.PlayKeys._

name := """akwire"""

version := "0.2-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala).settings(
  routesImport ++= Seq("controllers.Binders._", "com.mongodb.casbah.Imports.ObjectId", "models._")
)

scalaVersion := "2.11.7"


/** JAVA APPLICATION DEPS **/
libraryDependencies ++= Seq(
  // AWS SDK provides access to SNS
  "com.amazonaws" % "aws-java-sdk" % "1.9.31",
  "com.twilio.sdk" % "twilio-java-sdk" % "3.4.5",
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
  "ws.securesocial" %% "securesocial" % "3.0-M1",
  // Streams for Akka
  "com.typesafe.akka" %% "akka-stream-experimental" % "1.0",
  "com.typesafe.akka" %% "akka-testkit" % "2.3.12",
  "org.scalatest"     %% "scalatest" % "2.2.0"
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
  "org.webjars.bower" % "angular-ui-router" % "0.2.14",
  "org.webjars.bower" % "angular-local-storage" % "0.1.5",
  "org.webjars.bower" % "bootstrap" % "3.3.4",
  "org.webjars.bower" % "ng-table" % "0.5.4",
  "org.webjars"       % "angular-ui-ace" % "0.2.3"
)

dependencyOverrides ++= Set("org.webjars.bower" % "angular" % "1.3.14")
