package checkin.infrastructure.config

import com.typesafe.config.{Config, ConfigFactory}

/**
 * Configuración tipada del microservicio.
 *
 * Lee de `src/main/resources/application.conf`. Cualquier valor puede
 * sobrescribirse con variables de entorno.
 *
 * Ejemplos:
 *   HTTP_PORT=8082 sbt run
 *   KAFKA_BOOTSTRAP=kafka-del-equipo:9092 sbt run
 *   DB_URL=jdbc:postgresql://pg-del-equipo:5432/checkin_db sbt run
 */
final case class DatabaseConfig(
  url:      String,
  user:     String,
  password: String,
  driver:   String,
  poolSize: Int
)

final case class AppConfig(
  httpPort:       Int,
  kafkaBootstrap: String,
  kafkaTopic:     String,
  acks:           String,
  db:             DatabaseConfig
)

object AppConfig {
  def load(): AppConfig = {
    val conf: Config = ConfigFactory.load()
    AppConfig(
      httpPort       = conf.getInt("checkin.http.port"),
      kafkaBootstrap = conf.getString("checkin.kafka.bootstrap-servers"),
      kafkaTopic     = conf.getString("checkin.kafka.topic"),
      acks           = conf.getString("checkin.kafka.acks"),
      db = DatabaseConfig(
        url      = conf.getString("checkin.db.url"),
        user     = conf.getString("checkin.db.user"),
        password = conf.getString("checkin.db.password"),
        driver   = conf.getString("checkin.db.driver"),
        poolSize = conf.getInt("checkin.db.pool-size")
      )
    )
  }
}