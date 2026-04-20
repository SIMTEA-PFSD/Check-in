package checkin.infrastructure.config

import com.typesafe.config.{Config, ConfigFactory}

/**
 * Configuración tipada del microservicio.
 *
 * Lee de `src/main/resources/application.conf` (ver ese archivo).
 * Podemos sobrescribir cualquier valor con variables de entorno:
 *   KAFKA_BOOTSTRAP=kafka:9092 sbt run
 */
final case class AppConfig(
  kafkaBootstrap: String,
  kafkaTopic: String,
  acks: String
)

object AppConfig {
  def load(): AppConfig = {
    val conf: Config = ConfigFactory.load()

    AppConfig(
      kafkaBootstrap = conf.getString("checkin.kafka.bootstrap-servers"),
      kafkaTopic = conf.getString("checkin.kafka.topic"),
      acks = conf.getString("checkin.kafka.acks")
    )
  }
}