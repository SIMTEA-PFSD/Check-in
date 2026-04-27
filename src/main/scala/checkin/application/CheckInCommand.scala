package checkin.application

final case class CheckInCommand(
  pasajeroId: String,
  nombrePasajero: String,
  documento: String,
  email: String,
  vueloId: String,
  equipajes: List[EquipajeInput]
)

/** Datos de una maleta individual en el comando. */
final case class EquipajeInput(
  codigoRFID: String,
  peso: Double
)