package checkin.domain.event

import java.time.Instant
import java.util.UUID

final case class EventoEquipaje(
  eventId: UUID,
  tipo: TipoEvento,
  equipajeId: String,
  timestamp: Instant,
  topico: String,
  payload: Map[String, String]
)

object EventoEquipaje {

  /** Construye el evento PASAJERO_REGISTRADO*/
  def pasajeroRegistrado(equipajeId: String, pasajeroId: String, vueloId: String ): EventoEquipaje =
    EventoEquipaje(
      eventId = UUID.randomUUID(),
      tipo = TipoEvento.PasajeroRegistrado,
      equipajeId = equipajeId,
      timestamp = Instant.now(),
      topico = TipoEvento.PasajeroRegistrado.topico,
      payload = Map(
        "pasajeroId" -> pasajeroId,
        "vueloId" -> vueloId,
        "equipajeId" -> equipajeId
      )
    )

  /** Construye el evento EQUIPAJE_ESCANEADO (cuando el RFID se registra). */
  def equipajeEscaneado(equipajeId: String, codigoRFID: String, peso: Double): EventoEquipaje =
    EventoEquipaje(
      eventId = UUID.randomUUID(),
      tipo = TipoEvento.EquipajeEscaneado,
      equipajeId = equipajeId,
      timestamp = Instant.now(),
      topico = TipoEvento.EquipajeEscaneado.topico,
      payload = Map(
        "equipajeId" -> equipajeId,
        "codigoRFID" -> codigoRFID,
        "peso" -> peso.toString
      )
    )
}