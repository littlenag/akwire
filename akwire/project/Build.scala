import sbt._
import Keys._
import play.Project._

object ApplicationBuild extends Build {

  val appName         = "akwire"
  val appVersion      = "0.1-SNAPSHOT"

  val appDependencies = Seq(
    /** JAVA DEPS **/
    // replace with spring
    //"com.google.inject" % "guice" % "3.0",
    "javax.inject" % "javax.inject" % "1",

    "com.rabbitmq" % "amqp-client" % "3.3.0",

    "org.mockito" % "mockito-core" % "1.9.5" % "test",

    /** SCALA DEPS **/
    "org.reactivemongo" %% "reactivemongo" % "0.10.0",
    "org.reactivemongo" %% "play2-reactivemongo" % "0.10.2",

    "org.mongodb" %% "casbah" % "2.7.2",

    /** Likely will want this eventually **/
    "com.github.sstone" %% "akka-amqp-proxies" % "1.3",

    // Update to RC1 when feasible
    "org.springframework.scala" %% "spring-scala" % "1.0.0.RC1",

    // TODO Find a replacement for these
    "com.espertech" % "esper" % "4.10.0",

    // TODO These should be removed at some point
    "org.mongodb" % "mongo-java-driver" % "2.11.3",
    "org.springframework.data" % "spring-data-mongodb" % "1.3.2.RELEASE"
  )

  // Add your own project settings here'
  val main = play.Project(appName, appVersion, appDependencies).settings(defaultScalaSettings:_*).settings(
    resolvers += "SpringSource repository" at "https://repo.springsource.org/libs-milestone/"
  )
}
