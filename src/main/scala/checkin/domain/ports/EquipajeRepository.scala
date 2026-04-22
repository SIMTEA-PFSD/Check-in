package checkin.domain.ports

import checkin.domain.model.Equipaje

/**
 * Persistencia de equipajes.
 *
 * Devolvemos Either[String, Equipaje] para manejar errores de manera
 * funcional (sin excepciones). El String describe qué falló.
 */
trait EquipajeRepository {
  def guardar(equipaje: Equipaje): Either[String, Equipaje]
  def buscarPorId(id: String): Option[Equipaje]
  def buscarPorPasajero(pasajeroId: String): List[Equipaje]
}