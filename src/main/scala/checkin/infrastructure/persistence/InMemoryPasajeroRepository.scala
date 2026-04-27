package checkin.infrastructure.persistence

import checkin.domain.model.Pasajero
import checkin.domain.ports.PasajeroRepository

import scala.collection.concurrent.TrieMap

/**
 * Para pruebas.
 */
class InMemoryPasajeroRepository extends PasajeroRepository {

  private val store = TrieMap.empty[String, Pasajero]

  override def guardar(pasajero: Pasajero): Either[String, Pasajero] = {
    store.put(pasajero.id, pasajero)
    Right(pasajero)
  }

  override def buscarPorId(id: String): Option[Pasajero] =
    store.get(id)

  override def buscarPorDocumento(doc: String): Option[Pasajero] =
    store.values.find(_.documento == doc)
}