package checkin.infrastructure.persistence

import cats.effect.IO
import cats.effect.unsafe.IORuntime
import cats.implicits._
import checkin.domain.event.EventoEquipaje
import checkin.domain.model.{Equipaje, EstadoEquipaje}
import checkin.domain.ports.EquipajeRepository
import checkin.infrastructure.kafka.EventoSerializer
import doobie._
import doobie.implicits._
import doobie.postgres.implicits._
import doobie.hikari.HikariTransactor

import java.time.Instant
import scala.util.control.NonFatal

final class PostgresEquipajeRepository(
  xa: HikariTransactor[IO]
)(implicit runtime: IORuntime) extends EquipajeRepository {

  private implicit val estadoMeta: Meta[EstadoEquipaje] =
    Meta[String].timap(parseEstado)(_.nombre)// Guarda como texto en DB, pero mapea a enum en Scala.

  private def parseEstado(s: String): EstadoEquipaje = s.toUpperCase match {
    case "REGISTRADO" => EstadoEquipaje.Registrado
    case "ENSEGURIDAD" => EstadoEquipaje.EnSeguridad
    case "ENBODEGA" => EstadoEquipaje.EnBodega
    case "ENVEHICULO" => EstadoEquipaje.EnVehiculo
    case "ENTREGADO" => EstadoEquipaje.Entregado
    case "PERDIDO" => EstadoEquipaje.Perdido
    case otro => throw new IllegalStateException(s"Estado desconocido en DB: $otro")
  }

  private implicit val equipajeRead: Read[Equipaje] =
    Read[(String, String, Double, EstadoEquipaje, String, String, Instant, Option[Instant], Option[String])]
      .map { case (id, rfid, peso, estado, pasajeroId, vueloId, ts, tsEntrega, vehId) =>
        Equipaje(id, rfid, peso, estado, pasajeroId, vueloId, ts, tsEntrega, vehId)
      }


  private def insertEquipajeOp(e: Equipaje): ConnectionIO[Equipaje] =
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
    """.update.run.as(e) // Operación de escritura

  private def insertOutboxOp(evento: EventoEquipaje): ConnectionIO[Unit] = {
    val payload = EventoSerializer.aJson(evento)
    sql"""
      INSERT INTO outbox_events (event_id, topico, clave, payload)
      VALUES (${evento.eventId}, ${evento.topico}, ${evento.equipajeId}, $payload)
      ON CONFLICT (event_id) DO NOTHING
    """.update.run.void
  }

  // Operaciones

  override def guardar(e: Equipaje): Either[String, Equipaje] = {
    try {
      insertEquipajeOp(e).transact(xa).unsafeRunSync()
      Right(e)
    } catch {
      case NonFatal(ex) => Left(s"Error guardando equipaje: ${ex.getMessage}")
    }
  }

  override def guardarTodos(equipajes: List[Equipaje]): Either[String, List[Equipaje]] = {
    val op: ConnectionIO[List[Equipaje]] =
      equipajes.traverse(insertEquipajeOp)
    try {
      Right(op.transact(xa).unsafeRunSync())
    } catch {
      case NonFatal(ex) =>
        Left(s"Error guardando equipajes atómicamente (rollback): ${ex.getMessage}")
    }
  }


  override def guardarTodosConEventos(
    equipajes: List[Equipaje],
    eventos:   List[EventoEquipaje]
  ): Either[String, List[Equipaje]] = {
    val op: ConnectionIO[List[Equipaje]] = for {
      //primero inserta equipajes
      //después inserta eventos outbox
      es <- equipajes.traverse(insertEquipajeOp)
      _  <- eventos.traverse_(insertOutboxOp)
    } yield es

    try {
      Right(op.transact(xa).unsafeRunSync())
    } catch {
      case NonFatal(ex) =>
        Left(s"Error guardando equipajes+outbox atómicamente (rollback): ${ex.getMessage}")
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