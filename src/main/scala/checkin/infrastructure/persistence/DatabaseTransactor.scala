package checkin.infrastructure.persistence

import cats.effect.{IO, Resource}
import checkin.infrastructure.config.DatabaseConfig
import com.zaxxer.hikari.HikariConfig
import doobie.hikari.HikariTransactor

/**
 * Construye un HikariTransactor[IO] como resource — garantiza que el
 * pool de conexiones se cierra correctamente cuando la app termina.
 *
 */
object DatabaseTransactor {

  def build(config: DatabaseConfig): Resource[IO, HikariTransactor[IO]] = {
    val hc = new HikariConfig()
    hc.setDriverClassName(config.driver)
    hc.setJdbcUrl(config.url)
    hc.setUsername(config.user)
    hc.setPassword(config.password)
    hc.setMaximumPoolSize(config.poolSize)
    hc.setPoolName("checkin-pool")

    HikariTransactor.fromHikariConfig[IO](hc)
  }
}