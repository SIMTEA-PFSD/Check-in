package checkin.infrastructure.persistence

import cats.effect.{IO, Resource}
import checkin.infrastructure.config.DatabaseConfig
import com.zaxxer.hikari.HikariConfig
import doobie.hikari.HikariTransactor

/**
 * Construye un `HikariTransactor[IO]` como `Resource` — garantiza que el
 * pool de conexiones se CIERRA correctamente cuando la app termina.
 *
 * Nota: en Doobie 1.0.0-RC4 el Transactor maneja su propio scheduling
 * via cats-effect 3 (Async), ya no hace falta pasar un ExecutionContext.
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