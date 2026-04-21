package checkin.infrastructure.persistence

import cats.effect.IO
import cats.effect.unsafe.IORuntime
import checkin.domain.model.{Equipaje, EstadoEquipaje}
import checkin.domain.ports.EquipajeRepository
import doobie._
import doobie.implicits._
import doobie.postgres.implicits._   // trae Meta[Instant] para TIMESTAMPTZ
import doobie.hikari.HikariTransactor

import java.time.Instant
import scala.util.control.NonFatal

/**
 * «adapter» Postgres para el puerto EquipajeRepository.
 *
 * Reglas especiales para mapear el dominio ↔ SQL:
 *   - EstadoEquipaje (ADT sealed) ←→ VARCHAR. Definimos un `Meta` custom.
 *   - Instant ←→ TIMESTAMPTZ. Viene de `doobie.postgres.implicits`.
 *   - Option[X] ←→ columna NULL-able. Doobie lo maneja automáticamente.
 */
final class PostgresEquipajeRepository(
  xa: HikariTransactor[IO]
)(implicit runtime: IORuntime) extends EquipajeRepository {

  // ─── Mapeo EstadoEquipaje ↔ VARCHAR ──────────────────
  // El Meta es cómo Doobie "sabe" leer/escribir un tipo custom a SQL.
  private implicit val estadoMeta: Meta[EstadoEquipaje] =
    Meta[String].timap(parseEstado)(_.nombre)

  private def parseEstado(s: String): EstadoEquipaje = s.toUpperCase match {
    case "REGISTRADO"   => EstadoEquipaje.Registrado
    case "ENSEGURIDAD"  => EstadoEquipaje.EnSeguridad
    case "ENBODEGA"     => EstadoEquipaje.EnBodega
    case "ENVEHICULO"   => EstadoEquipaje.EnVehiculo
    case "ENTREGADO"    => EstadoEquipaje.Entregado
    case "PERDIDO"      => EstadoEquipaje.Perdido
    case otro           => throw new IllegalStateException(s"Estado desconocido en DB: $otro")
  }

  // Read orden: coincide con SELECT de abajo
  private implicit val equipajeRead: Read[Equipaje] =
    Read[(String, String, Double, EstadoEquipaje, String, String, Instant, Option[Instant], Option[String])]
      .map { case (id, rfid, peso, estado, pasajeroId, vueloId, ts, tsEntrega, vehId) =>
        Equipaje(id, rfid, peso, estado, pasajeroId, vueloId, ts, tsEntrega, vehId)
      }

  // ─── Operaciones ──────────────────────────────────────

  override def guardar(e: Equipaje): Either[String, Equipaje] = {
    val ins =
      sql"""
        INSERT INTO equipajes (
          id, codigo_rfid, peso, estado,
          pasajero_id, vuelo_id, timestamp_checkin,
          timestamp_entrega, vehiculo_id
        ) VALUES (
          ${e.id}, ${e.codigoRFID}, ${e.peso}, ${e.estado},
          ${e.pasajeroId}, ${e.vueloId}, ${e.timestampCheckin},
          ${e.timestampEntrega}, ${e.vehiculoId}
        )
        ON CONFLICT (id) DO UPDATE
          SET estado            = EXCLUDED.estado,
              timestamp_entrega = EXCLUDED.timestamp_entrega,
              vehiculo_id       = EXCLUDED.vehiculo_id
      """.update.run

    try {
      ins.transact(xa).unsafeRunSync()
      Right(e)
    } catch {
      case NonFatal(ex) => Left(s"Error guardando equipaje: ${ex.getMessage}")
    }
  }

  override def buscarPorId(id: String): Option[Equipaje] = {
    val q =
      sql"""
        SELECT id, codigo_rfid, peso, estado,
               pasajero_id, vuelo_id, timestamp_checkin,
               timestamp_entrega, vehiculo_id
        FROM equipajes
        WHERE id = $id
      """.query[Equipaje].option

    try q.transact(xa).unsafeRunSync()
    catch { case NonFatal(_) => None }
  }

  override def buscarPorPasajero(pasajeroId: String): List[Equipaje] = {
    val q =
      sql"""
        SELECT id, codigo_rfid, peso, estado,
               pasajero_id, vuelo_id, timestamp_checkin,
               timestamp_entrega, vehiculo_id
        FROM equipajes
        WHERE pasajero_id = $pasajeroId
        ORDER BY timestamp_checkin
      """.query[Equipaje].to[List]

    try q.transact(xa).unsafeRunSync()
    catch { case NonFatal(_) => List.empty }
  }
}