package checkin.domain.ports

import checkin.domain.model.Pasajero

/**
 * Interfaz definida en el dominio.
 *
 * Las implementacionesviven en `infrastructure` (PostgreSQL, InMemory, Mongo...).
 * Inversión de dependencias:
 * El dominio no sabe cuál se está usando.
 */
trait PasajeroRepository {
  def guardar(pasajero: Pasajero): Either[String, Pasajero]
  def buscarPorId(id: String): Option[Pasajero]
  def buscarPorDocumento(doc: String): Option[Pasajero]
}