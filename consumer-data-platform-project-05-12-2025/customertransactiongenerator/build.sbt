ThisBuild / version := "0.1.0-SNAPSHOT"

scalaVersion := "2.12.15"

val sparkVersion = "3.5.1"

lazy val root = (project in file("."))
  .settings(
    name := "CustomerTransactionGenerator"
  )

libraryDependencies ++= Seq(
  // Testing
  "org.scalatest" %% "scalatest" % "3.2.2" % "test",

  // Spark core + SQL + Streaming
  "org.apache.spark" %% "spark-core"              % sparkVersion,
  "org.apache.spark" %% "spark-sql"               % sparkVersion,
  "org.apache.spark" %% "spark-streaming"         % sparkVersion,
  "org.apache.spark" %% "spark-streaming-kafka-0-10" % sparkVersion,
  "org.apache.spark" %% "spark-sql-kafka-0-10"    % sparkVersion,

  // Cassandra
  "com.datastax.spark" %% "spark-cassandra-connector" % "3.2.0",

  // Kafka client
  "org.apache.kafka" % "kafka-clients" % "3.7.0",

  // Utilities
  "joda-time" % "joda-time" % "2.10.10",
  "com.typesafe" % "config" % "1.4.2",

  // Hadoop + AWS S3
  "org.apache.hadoop" % "hadoop-common" % "3.3.1",
  "org.apache.hadoop" % "hadoop-aws"    % "3.3.1",
  "com.amazonaws"     % "aws-java-sdk-bundle" % "1.11.375",

  // Protobuf runtime
  "com.google.protobuf" % "protobuf-java" % "3.25.3",

  // MySQL
  "mysql" % "mysql-connector-java" % "8.0.33",

  // Logging
  "com.typesafe.scala-logging" %% "scala-logging" % "3.9.5",
  "org.slf4j" % "slf4j-api" % "2.0.9",
  "ch.qos.logback" % "logback-classic" % "1.4.11"
)
