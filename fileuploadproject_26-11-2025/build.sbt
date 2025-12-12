import scala.collection.Seq

name := "FileUploadProject"
organization := "com.payoda"
version := "1.0-SNAPSHOT"
scalaVersion := "2.13.16"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

fork := true

libraryDependencies ++= Seq(
  // Play framework + DI + JSON
  guice,
  "com.typesafe.play" %% "play-json" % "2.9.4",

  // Play + Slick (DB)
  "org.playframework" %% "play-slick" % "6.1.0",
  "org.playframework" %% "play-slick-evolutions" % "6.1.0",
  "mysql" % "mysql-connector-java" % "8.0.26",

  // Testing
  "org.scalatestplus.play" %% "scalatestplus-play" % "7.0.2" % Test,
  "org.scalatest" %% "scalatest" % "3.2.15" % Test
)
