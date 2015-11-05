import _root_.sbt.Keys._
import _root_.sbt._
import sbtassembly.AssemblyKeys._

object Build extends Build {

  import BuildSettings._

  override lazy val settings = super.settings :+ {
    shellPrompt := (s => Project.extract(s).currentProject.id + " > ")
  }

  lazy val root = Project("crawler-service", file("."))
    .aggregate(news)

  ///////////////////////////////////////////////////////////////
  // projects
  ///////////////////////////////////////////////////////////////
  lazy val app = Project("app", file("app"))
    .dependsOn(news, util)
    .settings(basicSettings: _*)
    .settings(
      description := "app",
      assemblyJarName in assembly := "crawler-app.jar",
      mainClass in assembly := Some("crawler.app.Main"),
      libraryDependencies ++= Seq(
        _json4sJackson,
        _akkaHttpJson4s,
        _akkaHttp
      ))

  lazy val news = Project("news", file("news"))
    .dependsOn(util)
    .settings(basicSettings: _*)
    .settings(
      description := "news",
      libraryDependencies ++= Seq(
        _json4sJackson % "provided",
        _akkaHttpJson4s % "provided",
        _akkaHttp % "provided"
      ))

  lazy val util = Project("util", file("util"))
    .settings(basicSettings: _*)
    .settings(
      description := "util",
      libraryDependencies ++= Seq(
        _json4sJackson % "provided",
        _akkaHttpJson4s % "provided",
        _akkaHttp % "provided",
        _mongoScala,
        _scalaLogging,
        _asyncHttpClient,
        _jsoup,
        _akkaActor,
        _logbackClassic
      ))
}