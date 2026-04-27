package checkin.infrastructure.persistence

import cats.effect.IO
import cats.effect.unsafe.IORuntime
import checkin.domain.ports.{EventoOutboxPendiente, OutboxRepository}
import doobie._
import doobie.implicits._
import doobie.postgres.implicits._
import doobie.hikari.HikariTransactor

import java.util.UUID
import scala.util.control.NonFatal

/**
 * Implementación PostgreSQL del OutboxRepository.
 * Lee eventos pendientes y actualiza su estado de publicación.
 */
final class PostgresOutboxRepository(
  xa: HikariTransactor[IO]
)(implicit runtime: IORuntime) extends OutboxRepository {

  override def obtenerPendientes(limite: Int): List[EventoOutboxPendiente] = {
    val q =
      sql"""
        SELECT event_id, topico, clave, payload
        FROM outbox_events
        WHERE published_at IS NULL
        ORDER BY created_at
        LIMIT $limite
      """.query[(UUID, String, String, String)].to[List]

    try {
      q.transact(xa).unsafeRunSync()
        .map { case (id, t, k, p) =>
          EventoOutboxPendiente(id, t, k, p)
        }
    } catch {
      case NonFatal(ex) =>
        println(s"[Outbox] Error leyendo pendientes: ${ex.getMessage}")
        List.empty
    }
  }

  override def marcarPublicado(eventId: UUID): Unit = {
    val u =
      sql"""
        UPDATE outbox_events
        SET published_at = NOW()
        WHERE event_id = $eventId
      """.update.run

    try u.transact(xa).unsafeRunSync()
    catch {
      case NonFatal(ex) =>
        println(s"[Outbox] Error marcando publicado $eventId: ${ex.getMessage}")
    }
  }

  override def marcarError(eventId: UUID, mensaje: String): Unit = {
    val u =
      sql"""
        UPDATE outbox_events
        SET intentos     = intentos + 1,
            ultimo_error = $mensaje
        WHERE event_id = $eventId
      """.update.run

    try u.transact(xa).unsafeRunSync()
    catch {
      case NonFatal(ex) =>
        println(s"[Outbox] Error marcando error $eventId: ${ex.getMessage}")
    }
  }
}