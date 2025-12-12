import scala.collection.Seq

name := "customer-analytics-api"
organization := "com.payoda"
version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.13.16"

libraryDependencySchemes += "org.scala-lang.modules" %% "scala-parser-combinators" % "always"

libraryDependencies ++= Seq(
  guice,
  filters,
  "org.scalatestplus.play" %% "scalatestplus-play" % "7.0.1" % Test,
  "org.scalatest" %% "scalatest" % "3.2.18" % Test,
  "org.playframework" %% "play-json" % "3.0.4",

  // Play + Slick
  "org.playframework" %% "play-slick" % "6.1.0",
  "org.playframework" %% "play-slick-evolutions" % "6.1.0",
  "com.typesafe.play" %% "play-json" % "2.9.4",
  "mysql" % "mysql-connector-java" % "8.0.26",


  // ===== Cassandra Driver =====
  "com.datastax.oss" % "java-driver-core" % "4.17.0",

  // ===== AWS SDK for S3 =====
  "software.amazon.awssdk" % "s3" % "2.20.25",
  "software.amazon.awssdk" % "auth" % "2.20.25",

  // ===== L1 Cache (Caffeine) - Ultra-fast in-memory cache =====
  "com.github.ben-manes.caffeine" % "caffeine" % "3.1.8",

  // ===== Logging =====
  "com.typesafe.scala-logging" %% "scala-logging" % "3.9.5",
  "org.slf4j" % "slf4j-api" % "2.0.12",
  "ch.qos.logback" % "logback-classic" % "1.5.3",

  // ===== Parquet & Hadoop =====
  "com.github.mjakubowski84" %% "parquet4s-core" % "2.22.0",
  "org.apache.hadoop" % "hadoop-client" % "3.3.6",
  "org.apache.hadoop" % "hadoop-aws" % "3.3.6",
  "org.apache.parquet" % "parquet-avro" % "1.13.1",
  // Auth
  "com.auth0" % "java-jwt" % "4.3.0"

)

dependencyOverrides ++= Seq(
  "org.scala-lang.modules" %% "scala-parser-combinators" % "2.4.0"
)

fork := true
javaOptions ++= Seq(
  "-Xms512m",
  "-Xmx1g",
  "-XX:+UseG1GC",
  "-XX:MaxGCPauseMillis=100"
)
// Suppress deprecation and feature warnings
scalacOptions ++= Seq(
  "-feature",
  "-deprecation:false", // Disable deprecation warnings
  "-Wconf:cat=deprecation:s", // Silence deprecation warnings
  "-Wconf:cat=feature:s", // Silence feature warnings
  "-Wconf:cat=optimizer:s", // Silence optimizer warnings
  "-Xlint:-unused", // Disable unused warnings
  "-Ywarn-unused:imports", // Only warn on unused imports
  "-encoding", "UTF-8"
)



