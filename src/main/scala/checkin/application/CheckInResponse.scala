package checkin.application

/**
 * Resultado exitoso del check-in.
 *
 * Se devuelve al adaptador que invocó el caso de uso para que
 * lo convierta a HTTP 200, log, o lo que corresponda.
 */
final case class CheckInResponse(
  pasajeroId: String,
  vueloId: String,
  equipajesRegistrados: List[String],  // IDs de equipajes creados
  eventosPublicados: Int
)