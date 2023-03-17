name := "zio-watchdog"
organization := "com.github.fortega"
version := "0.0.1"

scalaVersion := "2.13.10"

libraryDependencies ++= Seq(
  "zio" -> "dev.zio" %% "zio-streams" % "2.0.10",
  "scalaTest" -> "org.scalatest" %% "scalatest" % "3.2.15" % Test
).map { case (_, module) => module }
