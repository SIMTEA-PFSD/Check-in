// ============================================================
//  Microservicio Check-in - Sistema de Gestión de Equipajes
//  Programación Funcional y Sistemas Distribuidos
// ============================================================
// Generado completamente con ayuda de IA

name         := "checkin-service"
version      := "0.1.0"
scalaVersion := "2.13.12"

scalacOptions ++= Seq(
  "-deprecation",
  "-feature",
  "-unchecked",
  "-Xlint:_",
  "-Ywarn-dead-code"
)

// ─── Versiones ──────────────────────────────────────────────
val http4sVersion     = "0.23.25"
val circeVersion      = "0.14.6"
val catsEffectVersion = "3.5.2"
val doobieVersion     = "1.0.0-RC4"

// ─── Dependencias ───────────────────────────────────────────
libraryDependencies ++= Seq(
  // Kafka (productor de eventos)
  "org.apache.kafka"  %  "kafka-clients"        % "3.6.0",

  // JSON funcional (Circe)
  "io.circe"          %% "circe-core"           % circeVersion,
  "io.circe"          %% "circe-generic"        % circeVersion,
  "io.circe"          %% "circe-parser"         % circeVersion,

  // Efectos funcionales + servidor HTTP (http4s)
  "org.typelevel"     %% "cats-effect"          % catsEffectVersion,
  "org.http4s"        %% "http4s-ember-server"  % http4sVersion,
  "org.http4s"        %% "http4s-dsl"           % http4sVersion,
  "org.http4s"        %% "http4s-circe"         % http4sVersion,

  // Base de datos (PostgreSQL + Doobie, FP puro)
  "org.tpolecat"      %% "doobie-core"          % doobieVersion,
  "org.tpolecat"      %% "doobie-hikari"        % doobieVersion,
  "org.tpolecat"      %% "doobie-postgres"      % doobieVersion,
  "org.postgresql"    %  "postgresql"           % "42.7.1",

  // Config tipada y logs
  "com.typesafe"      %  "config"               % "1.4.3",
  "org.slf4j"         %  "slf4j-simple"         % "2.0.9",

  // Test
  "org.scalatest"     %% "scalatest"            % "3.2.17" % Test
)

Compile / mainClass := Some("checkin.Main")

// ─── Configuración Docker (sbt-native-packager) ────────────
// Genera la imagen con:   sbt Docker/publishLocal
// Y queda como:            checkin-service:0.1.0
enablePlugins(JavaAppPackaging, DockerPlugin)

Docker / packageName := "checkin-service"
Docker / version     := version.value
dockerBaseImage      := "eclipse-temurin:17-jre"
dockerExposedPorts   := Seq(8081)
dockerUpdateLatest   := true