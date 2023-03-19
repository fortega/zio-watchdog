name := "zio-watchdog"
organization := "com.github.fortega"
version := "0.1.0"

scalaVersion := "2.13.10"

libraryDependencies ++= Seq(
  "zio" -> "dev.zio" %% "zio-streams" % "2.0.10",
  "zio-slf4j" -> "dev.zio" %% "zio-logging-slf4j2" % "2.1.11" % Test,
  "logback" -> "ch.qos.logback" % "logback-classic" % "1.4.6" % Test,
  "scalaTest" -> "org.scalatest" %% "scalatest" % "3.2.15" % Test
).map { case (_, module) => module }
