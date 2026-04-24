package checkin.domain.event

/**
 * Tipos de eventos del sistema completo.
 *
 * El microservicio de Check-in solo emite los primeros dos:
 *   - PasajeroRegistrado
 *   - EquipajeEscaneado
 *
 * Los demás están aquí para que los demás microservicios del equipo
 * usen el mismo enum compartido al serializar/deserializar eventos.
 */
sealed trait TipoEvento { def topico: String }

object TipoEvento {
  case object PasajeroRegistrado extends TipoEvento { val topico = "registro.pasajero" }
  case object EquipajeEscaneado extends TipoEvento { val topico = "equipaje.seguridad" }
  case object EquipajeAsignado extends TipoEvento { val topico = "equipaje.bodega" }
  case object VehiculoDespachado extends TipoEvento { val topico = "equipaje.bodega" }
  case object EquipajeEntregado extends TipoEvento { val topico = "equipaje.bodega" }
  case object EventualidadDetectada extends TipoEvento { val topico = "vuelo.eventualidades" }
}