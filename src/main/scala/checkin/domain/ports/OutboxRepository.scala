package checkin.domain.ports

import java.util.UUID

/**
 * Representación de un evento ya serializado en la tabla outbox_events.
 */
final case class EventoOutboxPendiente(
  eventId: UUID,
  topico: String,
  clave: String,
  payload: String
)

trait OutboxRepository {
  def obtenerPendientes(limite: Int): List[EventoOutboxPendiente]
  def marcarPublicado(eventId: UUID): Unit
  def marcarError(eventId: UUID, mensaje: String): Unit
}
