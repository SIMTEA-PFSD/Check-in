package checkin.domain.ports

import checkin.domain.event.EventoEquipaje
import checkin.domain.model.Equipaje

trait EquipajeRepository {
  def guardar(equipaje: Equipaje): Either[String, Equipaje]
  def buscarPorId(id: String): Option[Equipaje]
  def buscarPorPasajero(pasajeroId: String): List[Equipaje]

  def guardarTodos(equipajes: List[Equipaje]): Either[String, List[Equipaje]] = {
    val resultados = equipajes.map(guardar)
    val errores = resultados.collect { case Left(err) => err }
    if (errores.nonEmpty) Left(errores.mkString("; "))
    else Right(resultados.collect { case Right(e) => e })
  }

  
  def guardarTodosConEventos(equipajes: List[Equipaje], eventos: List[EventoEquipaje] ): Either[String, List[Equipaje]] =
    guardarTodos(equipajes)
}