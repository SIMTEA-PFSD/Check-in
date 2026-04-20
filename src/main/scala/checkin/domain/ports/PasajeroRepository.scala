package checkin.domain.ports

import checkin.domain.model.Pasajero

/**
 * «port» — Interfaz definida en el dominio.
 *
 * Las IMPLEMENTACIONES viven en `infrastructure` (PostgreSQL, InMemory, Mongo...).
 * El dominio NO sabe cuál se está usando — eso es inversión de dependencias.
 */
trait PasajeroRepository {
  def guardar(pasajero: Pasajero): Either[String, Pasajero]
  def buscarPorId(id: String): Option[Pasajero]
  def buscarPorDocumento(doc: String): Option[Pasajero]
}