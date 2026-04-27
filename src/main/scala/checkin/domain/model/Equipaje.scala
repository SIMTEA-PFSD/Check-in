package checkin.domain.model

import java.time.Instant
import java.util.UUID

final case class Equipaje(
    id: String,
    codigoRFID: String,
    peso: Double,
    estado: EstadoEquipaje,
    pasajeroId: String,
    vueloId: String,
    timestampCheckin: Instant,
    timestampEntrega: Option[Instant] = None,
    vehiculoId: Option[String] = None
) {
    require(peso > 0, s"El peso deber ser positivo, fue $peso")
    require(codigoRFID.nonEmpty, "El código RFID no puede estar vacío")

    /** Método puro: devuelve una nueva instancia con el estado actualizado.*/
    def actualizarEstado(nuevo : EstadoEquipaje) : Either[String, Equipaje] =
        if (EstadoEquipaje.esTransicionValida(this.estado, nuevo))
            Right(this.copy(estado = nuevo))
        else
            Left(s"Transición de estado no válida: $estado -> $nuevo")
}

object Equipaje {
    /**
    * Factory method: crea un equipaje nuevo en estado REGISTRADO.
    * Único lugar donde un Equipaje puede comenzar su ciclo de vida.
    */
    def registrar(codigoRFID: String, peso: Double, pasajeroId: String, vueloId: String): Equipaje = {
        Equipaje(
            id = UUID.randomUUID().toString,
            codigoRFID = codigoRFID,
            peso = peso,
            estado = EstadoEquipaje.Registrado,
            pasajeroId = pasajeroId,
            vueloId = vueloId,
            timestampCheckin = Instant.now()
        )
    }
}