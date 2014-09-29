import com.google.javascript.jscomp.{CompilationLevel, CompilerOptions}
import sbt._
import Keys._
import play.Project._

object ApplicationBuild extends Build {

  val appName         = "akwire"
  val appVersion      = "0.1-SNAPSHOT"

  val appDependencies = Seq(
    /** JAVA DEPS **/
    "javax.inject" % "javax.inject" % "1",

    "com.rabbitmq" % "amqp-client" % "3.3.0",

    "org.mockito" % "mockito-core" % "1.9.5" % "test",

    /** SCALA DEPS **/

    "org.mongodb" %% "casbah" % "2.7.1",
    "com.novus" %% "salat" % "1.9.8",

    // SCALDI for dependency injection (dropping spring)
    "org.scaldi" %% "scaldi" % "0.3.2",
    "org.scaldi" %% "scaldi-play" % "0.3.2",
    "org.scaldi" %% "scaldi-akka" % "0.3.2",

    // For User authentication and login
    "ws.securesocial" %% "securesocial" % "2.1.4",

    /** Likely will want this eventually **/
    "com.github.sstone" %% "akka-amqp-proxies" % "1.3",

    // Using clojure and reimann for a rules engine (instead of esper)
    "org.clojure" % "clojure" % "1.6.0",
    "org.clojure" % "tools.logging" % "0.2.6",
    "org.clojure" % "math.numeric-tower" % "0.0.4"
  )


  val root = new java.io.File(".")
  val jsOptions = new CompilerOptions()
  jsOptions.closurePass = true
  jsOptions.setProcessCommonJSModules(true)
  jsOptions.setCommonJSModulePathPrefix(root.getCanonicalPath + "/app/assets/javascripts/")
  jsOptions.setLanguageIn(CompilerOptions.LanguageMode.ECMASCRIPT5)

  CompilationLevel.WHITESPACE_ONLY.setOptionsForCompilationLevel(jsOptions)

  // Add your own project settings here'
  val main = play.Project(appName, appVersion, appDependencies)
    .settings(defaultScalaSettings:_*)
    .settings(closureCompilerSettings(jsOptions):_*)
    .settings(
    resolvers += "SpringSource repository" at "https://repo.springsource.org/libs-milestone/",
    resolvers += Resolver.sonatypeRepo("releases"),
    requireJs += "main.js",
    requireJsShim += "main.js"
    //unmanagedResourceDirectories in Runtime += baseDirectory.value / "riemann",
    //includeFilter in unmanagedResources := "*.clj"
  )
}
