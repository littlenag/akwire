import sbt._

object ApplicationBuild extends Build {

  val appName         = "akwire"
  val appVersion      = "0.1-SNAPSHOT"

  val appDependencies = Seq(
    /** JAVA DEPS **/
    // replace with spring
    "com.google.inject" % "guice" % "3.0",
    "javax.inject" % "javax.inject" % "1",

    "com.rabbitmq" % "amqp-client" % "3.3.0",

    "org.mockito" % "mockito-core" % "1.9.5" % "test",

    "org.springframework.scala" % "spring-scala" % "1.0.0.M2",

    /** SCALA DEPS **/
    "org.reactivemongo" %% "reactivemongo" % "0.10.0",
    "org.reactivemongo" %% "play2-reactivemongo" % "0.10.2",

    /** Likely will want this eventually **/
    "com.github.sstone" %% "akka-amqp-proxies" % "1.3"
  )


  val main = play.Project(appName, appVersion, appDependencies).settings(
    // Add your own project settings here      
  )

}
