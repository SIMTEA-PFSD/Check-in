ThisBuild / version := "0.1.0"
ThisBuild / scalaVersion := "2.12.18"

lazy val root = (project in file("."))
  .settings(
    name := "simtea-producer",
    libraryDependencies ++= Seq(
      "org.apache.kafka" % "kafka-clients" % "3.5.1"
    )
  )