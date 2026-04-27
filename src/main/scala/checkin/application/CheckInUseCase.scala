package checkin.application

import checkin.domain.event.EventoEquipaje
import checkin.domain.model.{Equipaje, Pasajero}
import checkin.domain.ports.{EquipajeRepository, PasajeroRepository}


class CheckInUseCase(
  pasajeroRepo: PasajeroRepository,
  equipajeRepo: EquipajeRepository
) {

  private val PESO_MAX_KG = 23.0

  def ejecutar(cmd: CheckInCommand): Either[CheckInError, CheckInResponse] =
    for {
      _         <- validar(cmd)
      pasajero  <- registrarPasajero(cmd)
      equipajes <- registrarEquipajesYEventos(cmd, pasajero)
    } yield CheckInResponse(
      pasajeroId = pasajero.id,
      vueloId = cmd.vueloId,
      equipajesRegistrados = equipajes.map(_.id),
      eventosPublicados = equipajes.size  // van al outbox, el relay los publica
    )


  private def validar(cmd: CheckInCommand): Either[CheckInError, Unit] =
    if (cmd.equipajes.isEmpty)
      Left(CheckInError.SinEquipajes())
    else
      cmd.equipajes.find(_.peso > PESO_MAX_KG) match {
        case Some(e) => Left(CheckInError.EquipajeExcedePeso(e.peso))
        case None    => Right(())
      }

  private def registrarPasajero(cmd: CheckInCommand): Either[CheckInError, Pasajero] = {
    val pasajero = Pasajero(cmd.pasajeroId, cmd.nombrePasajero, cmd.documento, cmd.email)
    pasajeroRepo.guardar(pasajero).left.map(err => CheckInError.ErrorPersistencia(err))
  }

  /**
   * Crea N equipajes y N eventos y los persiste en una sola transacción
   * Si cualquiera falla, rollback de todo.
   */
   
  private def registrarEquipajesYEventos(cmd: CheckInCommand, pasajero: Pasajero): Either[CheckInError, List[Equipaje]] = {
    val equipajes = cmd.equipajes.map { e =>
      Equipaje.registrar(e.codigoRFID, e.peso, pasajero.id, cmd.vueloId)
    }
  
    val eventos: List[EventoEquipaje] = equipajes.map { e =>
      EventoEquipaje.pasajeroRegistrado(e.id, e.pasajeroId, cmd.vueloId)
    }

    equipajeRepo.guardarTodosConEventos(equipajes, eventos)
      .left.map(err => CheckInError.ErrorPersistencia(err))
  }
}