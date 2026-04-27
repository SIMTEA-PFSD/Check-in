package checkin.domain.ports

import checkin.domain.model.Pasajero


trait PasajeroRepository {
  def guardar(pasajero: Pasajero): Either[String, Pasajero]
  def buscarPorId(id: String): Option[Pasajero]
  def buscarPorDocumento(doc: String): Option[Pasajero]
}