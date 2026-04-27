package checkin.application

/**
 * Resultado exitoso del check-in.
 *
 */
final case class CheckInResponse(
  pasajeroId: String,
  vueloId: String,
  equipajesRegistrados: List[String],  // IDs de equipajes creados
  eventosPublicados: Int
)