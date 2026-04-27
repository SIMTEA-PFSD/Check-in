package checkin.infrastructure.config

import com.typesafe.config.{Config, ConfigFactory}

final case class DatabaseConfig(
  url: String,
  user: String,
  password: String,
  driver: String,
  poolSize: Int
)

final case class OutboxConfig(
  pollIntervalSeconds: Int,
  batchSize: Int
)

final case class AppConfig(
  httpPort: Int,
  kafkaBootstrap: String,
  kafkaTopic: String,
  acks: String,
  db: DatabaseConfig,
  outbox: OutboxConfig
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
      ),
      outbox = OutboxConfig(
        pollIntervalSeconds = conf.getInt("checkin.outbox.poll-interval-seconds"),
        batchSize           = conf.getInt("checkin.outbox.batch-size")
      )
    )
  }
}