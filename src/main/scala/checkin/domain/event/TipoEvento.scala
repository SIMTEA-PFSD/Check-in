package checkin.domain.event

/**
 * Tipos de eventos del sistema completo.
 *
 *   - PasajeroRegistrado
 *   - EquipajeEscaneado
 *
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