ThisBuild / scalaVersion := "2.12.10"
ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / organization := "com.payoda"

val sparkVersion = "3.2.1"
//val hadoopVersion = "3.3.1"

lazy val root = (project in file("."))
  .settings(
    name := "urbanmove-spark",

    libraryDependencies ++= Seq(
      "org.apache.spark" %% "spark-core" % sparkVersion,
      "org.apache.spark" %% "spark-sql" % sparkVersion,
      "org.apache.spark" %% "spark-streaming" % sparkVersion,
      "org.apache.spark" %% "spark-sql-kafka-0-10" % sparkVersion,
      "org.apache.spark" %% "spark-streaming-kafka-0-10" % sparkVersion,

//      // Hadoop for local file system access
//      "org.apache.hadoop" % "hadoop-common" % hadoopVersion,
//      "org.apache.hadoop" % "hadoop-client" % hadoopVersion,

      // Testing
      "org.scalatest" %% "scalatest" % "3.2.2" % Test,

      "mysql" % "mysql-connector-java" % "8.0.19"
    )
  )