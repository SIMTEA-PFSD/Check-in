// ============================================================
//  Microservicio Check-in - Sistema de Gestión de Equipajes
//  Programación Funcional y Sistemas Distribuidos
// ============================================================

name := "checkin-service"
version := "0.1.0"
scalaVersion := "2.13.12"

// Opciones del compilador para código más seguro e idiomático
scalacOptions ++= Seq(
  "-deprecation",
  "-feature",
  "-unchecked",
  "-Xlint:_",
  "-Ywarn-dead-code",
  "-Ywarn-unused"
)

// Dependencias principales
libraryDependencies ++= Seq(
  // Kafka - cliente oficial (Java lib, pero se usa muy bien desde Scala)
  "org.apache.kafka"  %  "kafka-clients"     % "3.6.0",

  // Circe - librería funcional para JSON (inmutable, type-safe)
  "io.circe"          %% "circe-core"        % "0.14.6",
  "io.circe"          %% "circe-generic"     % "0.14.6",
  "io.circe"          %% "circe-parser"      % "0.14.6",

  // Configuración tipada (lee application.conf)
  "com.typesafe"      %  "config"            % "1.4.3",

  // Logging simple
  "org.slf4j"         %  "slf4j-simple"      % "2.0.9",

  // Testing
  "org.scalatest"     %% "scalatest"         % "3.2.17" % Test
)

// Punto de entrada para `sbt run`
Compile / mainClass := Some("checkin.Main")