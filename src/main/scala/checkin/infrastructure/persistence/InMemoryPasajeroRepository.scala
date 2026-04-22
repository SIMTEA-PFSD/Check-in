package checkin.infrastructure.persistence

import checkin.domain.model.Pasajero
import checkin.domain.ports.PasajeroRepository

import scala.collection.concurrent.TrieMap

/**
 * Implementación en memoria del PasajeroRepository.
 *
 * Útil para el MVP (semana 1) y para tests. Cuando el equipo integre
 * PostgreSQL, basta con crear `PostgresPasajeroRepository extends PasajeroRepository`
 * y cambiar solo el Main.scala — ni el caso de uso ni el dominio cambian.
 *
 * Usa TrieMap porque es thread-safe (varios hilos pueden usarlo a la vez).
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