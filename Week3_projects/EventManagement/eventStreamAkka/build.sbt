import Dependencies.*


ThisBuild / scalaVersion     := "2.13.16"
ThisBuild / version          := "0.1.0-SNAPSHOT"
ThisBuild / organization     := "com.example"
ThisBuild / organizationName := "example"

lazy val root = (project in file("."))
  .settings(
    name := "EventStream",
    libraryDependencies += munit % Test
  )

libraryDependencies += "org.scalatestplus.play" %% "scalatestplus-play" % "7.0.2" % Test

resolvers += "Akka Repository" at "https://repo.akka.io/releases"

lazy val akkaVersion = "2.8.5" // latest 2.8.x
lazy val akkaHttpVersion = "10.2.10" // latest compatible with Akka 2.8
lazy val akkaStreamKafkaVersion = "3.0.0" // latest for Scala 2.13 + Akka 2.8

libraryDependencies ++= Seq(


  // Testing
  "org.scalatestplus.play" %% "scalatestplus-play" % "7.0.0" % Test,
  "org.scalatest" %% "scalatest" % "3.2.15" % Test,

  // Play + Slick
  "org.playframework" %% "play-slick" % "6.1.0",
  "org.playframework" %% "play-slick-evolutions" % "6.1.0",
  "com.typesafe.play" %% "play-json" % "2.9.4",
  "mysql" % "mysql-connector-java" % "8.0.26",

  // Auth
  "com.auth0" % "java-jwt" % "4.3.0",

  // Akka / Streams / Kafka
  "com.typesafe.akka" %% "akka-actor-typed" % akkaVersion,
  "com.typesafe.akka" %% "akka-stream" % akkaVersion,
  "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
  "com.typesafe.akka" %% "akka-stream-kafka" % akkaStreamKafkaVersion,
  "org.apache.kafka" % "kafka-clients" % "3.7.0",
  "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion


  //mail

)
libraryDependencies += "org.eclipse.angus" % "angus-mail" % "2.0.2"
libraryDependencies += "com.sun.mail" % "jakarta.mail" % "2.0.1"
libraryDependencies += "com.sun.activation" % "jakarta.activation" % "2.0.1"

fork := true
