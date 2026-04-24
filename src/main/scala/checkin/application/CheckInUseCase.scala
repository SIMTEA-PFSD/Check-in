package checkin.application

import checkin.domain.model.{Equipaje, Pasajero}
import checkin.domain.event.EventoEquipaje
import checkin.domain.ports.{EquipajeRepository, EventPublisher, PasajeroRepository}

/**
 * Flujo completo:
 *   1. Valida el comando de entrada.
 *   2. Registra el pasajero en el repositorio.
 *   3. Registra cada equipaje con estado inicial REGISTRADO.
 *   4. Publica un evento por cada equipaje a Kafka.
 *
 * Recibe los puertos (traits) por constructor y no conoce a Kafka ni
 * PostgreSQL.
 *
 * Usa for-comprehension sobre Either para corto-circuitar en el primer
 * error, sin if/else anidados. 
 */
class CheckInUseCase(
  pasajeroRepo: PasajeroRepository,
  equipajeRepo: EquipajeRepository,
  publisher: EventPublisher
) {

  /** Política de negocio: Peso máximo permitido por maleta */
  private val PESO_MAX_KG = 23.0

  def ejecutar(cmd: CheckInCommand): Either[CheckInError, CheckInResponse] =
    for {
      _         <- validar(cmd)
      pasajero  <- registrarPasajero(cmd)
      equipajes <- registrarEquipajes(cmd, pasajero)
      _         <- publicarEventos(equipajes, cmd.vueloId)
    } yield CheckInResponse(
      pasajeroId = pasajero.id,
      vueloId = cmd.vueloId,
      equipajesRegistrados = equipajes.map(_.id),
      eventosPublicados = equipajes.size
    )

  // Funciones auxiliares para cada paso del proceso

  private def validar(cmd: CheckInCommand): Either[CheckInError, Unit] =
    if (cmd.equipajes.isEmpty)
      Left(CheckInError.SinEquipajes())
    else
      cmd.equipajes.find(_.peso > PESO_MAX_KG) match {
        case Some(e) => Left(CheckInError.EquipajeExcedePeso(e.peso))
        case None    => Right(())
      }

  private def registrarPasajero(cmd: CheckInCommand): Either[CheckInError, Pasajero] = {
    val pasajero =
      Pasajero(cmd.pasajeroId, cmd.nombrePasajero, cmd.documento, cmd.email)

    pasajeroRepo.guardar(pasajero)
      .left.map(err => CheckInError.ErrorPersistencia(err))
  }

  private def registrarEquipajes(cmd: CheckInCommand, pasajero: Pasajero): Either[CheckInError, List[Equipaje]] = {
    // Construye un Equipaje por cada input
    val equipajes = cmd.equipajes.map {e => Equipaje.registrar(e.codigoRFID, e.peso, pasajero.id, cmd.vueloId)}

    // Guarda todos y acumula errores
    val resultados = equipajes.map(equipajeRepo.guardar)
    val errores = resultados.collect { case Left(err) => err }

    if (errores.nonEmpty)
      Left(CheckInError.ErrorPersistencia(errores.mkString("; ")))
    else
      Right(resultados.collect { case Right(e) => e })
  }

  private def publicarEventos(equipajes: List[Equipaje], vueloId: String): Either[CheckInError, Unit] = {
    val eventos = equipajes.map { e => EventoEquipaje.pasajeroRegistrado(e.id, e.pasajeroId, vueloId) }
    // Publica todos, si alguno falla corta-circuita
    eventos.foldLeft[Either[CheckInError, Unit]](Right(())) { (acc, evento) =>
      acc.flatMap { _ =>
        publisher.publicar(evento)
          .left.map(err => CheckInError.ErrorPublicacionEvento(err))
      }
    }
  }
}