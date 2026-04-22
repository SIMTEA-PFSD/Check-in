package checkin.infrastructure.persistence

import cats.effect.IO
import cats.effect.unsafe.IORuntime
import checkin.domain.model.Pasajero
import checkin.domain.ports.PasajeroRepository
import doobie._
import doobie.implicits._
import doobie.hikari.HikariTransactor

import scala.util.control.NonFatal

/**
 * Postgres para el puerto PasajeroRepository.
 *
 * Usa Doobie: escribimos SQL a mano como `sql"..."` (interpolación segura
 * que previene SQL injection), y lo ejecutamos vía Transactor.
 *
 * Usa UPSERT (`ON CONFLICT DO UPDATE`) — así el mismo pasajero puede
 * hacer múltiples check-ins sin que explote por clave duplicada.
 */
final class PostgresPasajeroRepository(
  xa: HikariTransactor[IO]
)(implicit runtime: IORuntime) extends PasajeroRepository {

  override def guardar(pasajero: Pasajero): Either[String, Pasajero] = {
    val sqlUpsert =
      sql"""
        INSERT INTO pasajeros (id, nombre, documento, email)
        VALUES (${pasajero.id}, ${pasajero.nombre}, ${pasajero.documento}, ${pasajero.email})
        ON CONFLICT (id) DO UPDATE
          SET nombre    = EXCLUDED.nombre,
              documento = EXCLUDED.documento,
              email     = EXCLUDED.email
      """.update.run

    try {
      sqlUpsert.transact(xa).unsafeRunSync()
      Right(pasajero)
    } catch {
      case NonFatal(e) => Left(s"Error guardando pasajero: ${e.getMessage}")
    }
  }

  override def buscarPorId(id: String): Option[Pasajero] = {
    val q =
      sql"""
        SELECT id, nombre, documento, email
        FROM pasajeros
        WHERE id = $id
      """.query[Pasajero].option

    try q.transact(xa).unsafeRunSync()
    catch { case NonFatal(_) => None }
  }

  override def buscarPorDocumento(doc: String): Option[Pasajero] = {
    val q =
      sql"""
        SELECT id, nombre, documento, email
        FROM pasajeros
        WHERE documento = $doc
      """.query[Pasajero].option

    try q.transact(xa).unsafeRunSync()
    catch { case NonFatal(_) => None }
  }
}