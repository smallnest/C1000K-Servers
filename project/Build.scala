import java.text.SimpleDateFormat
import java.util.Date

import com.typesafe.sbt.packager.archetypes.{AkkaAppPackaging, JavaAppPackaging}
import com.typesafe.sbt.packager.universal.UniversalDeployPlugin
import sbt.Defaults._
import sbt.Keys._
import sbt._
import sbtbuildinfo.Plugin.{BuildInfoKey, _}
import sbtrelease.ReleasePlugin._

object Build extends sbt.Build {
  val netty_version = "5.0.0.Alpha2"
  val jzlib_version = "1.1.3"
  val undertow_version = "1.2.0.CR1"
  val spray_version = "1.3.1"
  val spray_websocket_version = "0.1.4"
  val akka_version = "2.3.9"
  val typesafe_config_version = "1.2.1"
  val scala_xml_version = "1.0.3"
  val jetty_websocket_version = "9.3.0.M2"
  val javax_websocket_version = "1.1"
  val metrics_version = "3.1.2"
  val scala_logging_version = "3.1.0"
  val logback_version = "1.1.3"

  val commondependencies = Seq(
    "com.typesafe" % "config" % typesafe_config_version,
    "com.typesafe.scala-logging" %% "scala-logging" % scala_logging_version,
    "ch.qos.logback" % "logback-classic" % logback_version
  )
  val nettydependencies = Seq(
    "io.netty" % "netty-all" % netty_version,
    "com.jcraft" % "jzlib" % jzlib_version,
    "io.netty" % "netty-transport-native-epoll" % netty_version classifier "linux-x86_64"
  ) ++ commondependencies

  val undertowdependencies = Seq(
    "io.undertow" % "undertow-core" % undertow_version
  ) ++ commondependencies

  val spraydependencies = Seq(
    "org.scala-lang.modules" %% "scala-xml" % scala_xml_version,
    "io.spray" %% "spray-can" % spray_version,
    "com.typesafe.akka" %% "akka-actor" % akka_version,
    "com.typesafe.akka" %% "akka-kernel" % akka_version,
    "com.typesafe.akka" %% "akka-slf4j" % akka_version,
    "io.spray" %% "spray-http" % spray_version,
    "io.spray" %% "spray-json" % spray_version,
    "io.spray" %% "spray-routing-shapeless2" % spray_version,
    "com.wandoulabs.akka" %% "spray-websocket" % spray_websocket_version
  ) ++ commondependencies

  lazy val testClientdependencies = Seq(
    "org.eclipse.jetty.websocket" % "javax-websocket-client-impl" % jetty_websocket_version,
    "javax.websocket" % "javax.websocket-api" % javax_websocket_version,
    "io.dropwizard.metrics" % "metrics-core" % metrics_version
  ) ++ commondependencies

  lazy val root = Project("c1000k", file("."))
    .settings(defaultSettings: _*)
    .aggregate(undertow, netty, spray, testclient)

  lazy val undertow = Project("undertow", file("undertow"))
    .enablePlugins(sbtassembly.AssemblyPlugin)
    .enablePlugins(JavaAppPackaging)
    .settings(defaultSettings: _*)
    .settings(libraryDependencies ++= undertowdependencies)
    .enablePlugins(sbtassembly.AssemblyPlugin)

  lazy val netty = Project("netty", file("netty"))
    .enablePlugins(sbtassembly.AssemblyPlugin)
    .enablePlugins(JavaAppPackaging)
    .settings(defaultSettings: _*)
    .settings(libraryDependencies ++= nettydependencies)


  lazy val spray = Project("spray-can", file("spray-can"))
    .enablePlugins(sbtassembly.AssemblyPlugin)
    .enablePlugins(AkkaAppPackaging)
    .enablePlugins(UniversalDeployPlugin)
    .settings(defaultSettings: _*)
    .settings(buildInfoSettings ++ Seq(
    sourceGenerators in Compile <+= buildInfo,
    buildInfoKeys := Seq[BuildInfoKey](name, version, scalaVersion, sbtVersion, "buildDate" -> new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'").format(new Date())),
    buildInfoPackage := "com.colobu.webtest"): _*)
    .settings(libraryDependencies ++= spraydependencies)
    .settings(
      mainClass in Compile := Some("com.colobu.webtest.spray.WebServer"),
      fork in run := true)

  lazy val testclient = Project("testclient", file("testclient"))
    .enablePlugins(sbtassembly.AssemblyPlugin)
    .enablePlugins(JavaAppPackaging)
    .settings(defaultSettings: _*)
    .settings(libraryDependencies ++= testClientdependencies)
    .settings(mainClass in Compile := Some("com.colobu.c1000k.testclient.Main"))

  lazy val defaultSettings = coreDefaultSettings ++ releaseSettings ++ Seq(
    organization := "com.colobu.c1000k",
    version := "0.1",
    externalResolvers := Resolvers.all,
    scalaVersion := "2.11.6",
    scalacOptions := Seq("-deprecation", "-feature")
  )

  object Resolvers {
    val jgitrepo = "jgit-repo" at "http://download.eclipse.org/jgit/maven"
    val typesafe = "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"
    val sbtPlugins = "sbt-plugins" at "http://scalasbt.artifactoryonline.com/scalasbt/sbt-plugin-releases/"
    val spray = "spray" at "http://repo.spray.io"
    val mvnrepository = "mvnrepository" at "http://mvnrepository.com/artifact/"
    val mavenCenter = "Maven Central Server" at "http://repo1.maven.org/maven2/"
    val all = Seq(mvnrepository, mavenCenter, jgitrepo, typesafe, sbtPlugins, spray)
  }

}