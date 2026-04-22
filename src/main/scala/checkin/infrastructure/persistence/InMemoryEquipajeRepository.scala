package checkin.infrastructure.persistence

import checkin.domain.model.Equipaje
import checkin.domain.ports.EquipajeRepository

import scala.collection.concurrent.TrieMap

/**
 * Implementación en memoria del EquipajeRepository.
 *
 * Cuando se integre PostgreSQL, esta clase se reemplaza con una que
 * use JDBC/Slick/Doobie — el caso de uso ni se entera.
 */
class InMemoryEquipajeRepository extends EquipajeRepository {

  private val store = TrieMap.empty[String, Equipaje]

  override def guardar(equipaje: Equipaje): Either[String, Equipaje] = {
    store.put(equipaje.id, equipaje)
    Right(equipaje)
  }

  override def buscarPorId(id: String): Option[Equipaje] =
    store.get(id)

  override def buscarPorPasajero(pasajeroId: String): List[Equipaje] =
    store.values.filter(_.pasajeroId == pasajeroId).toList
}