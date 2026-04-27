package checkin.domain.model

final case class Pasajero(id: String, nombre: String, documento: String, email: String) {
    require(nombre.nonEmpty, "El nombre del pasajero no puede estar vacío")
    require(documento.nonEmpty, "El documento no puede estar vacío")
    require(email.contains("@"), s"Email inválido: $email")
}