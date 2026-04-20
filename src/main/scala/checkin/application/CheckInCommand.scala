package checkin.application

/**
 * Comando de entrada al caso de uso Check-In.
 *
 * Es el DTO (Data Transfer Object) que llega desde afuera: puede venir
 * de un endpoint REST, de un mensaje Kafka, o de cualquier otro adaptador.
 *
 * Nota: los DTOs viven en `application` porque son parte del contrato del
 * caso de uso — NO del dominio puro.
 */
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