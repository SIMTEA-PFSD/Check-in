package checkin.application

/**
 * Errores de negocio del Check-In.
 *
 * Usamos ADT (Algebraic Data Type) con sealed trait: el compilador obliga
 * a que los `match` cubran todos los casos. Esto es 100% funcional —
 * preferimos tipos sobre excepciones.
 */
sealed trait CheckInError {
  def mensaje: String
}

object CheckInError {
  final case class PasajeroInvalido(mensaje: String) extends CheckInError
  final case class EquipajeExcedePeso(peso: Double) extends CheckInError {
    val mensaje = s"Equipaje excede el peso máximo permitido: ${peso}kg"
  }
  final case class ErrorPublicacionEvento(causa: String) extends CheckInError {
    val mensaje = s"Error publicando evento en Kafka: $causa"
  }
  final case class ErrorPersistencia(causa: String) extends CheckInError {
    val mensaje = s"Error de persistencia: $causa"
  }
  final case class SinEquipajes(mensaje: String = "El pasajero debe registrar al menos 1 maleta")
    extends CheckInError
}