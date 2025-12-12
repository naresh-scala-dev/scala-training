ThisBuild / scalaVersion := "2.12.10"
ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / organization := "com.payoda"

val sparkVersion = "3.2.1"

lazy val root = (project in file("."))
  .settings(
    name := "spark-bigdata-casestudy",

    libraryDependencies ++= Seq(
      "org.apache.spark" %% "spark-core" % sparkVersion,
      "org.apache.spark" %% "spark-sql" % sparkVersion,
      "org.apache.spark" %% "spark-streaming" % sparkVersion,
      "org.apache.spark" %% "spark-sql-kafka-0-10" % sparkVersion,
      "org.apache.spark" %% "spark-streaming-kafka-0-10" % sparkVersion,

      "mysql" % "mysql-connector-java" % "8.0.19"
    )
  )