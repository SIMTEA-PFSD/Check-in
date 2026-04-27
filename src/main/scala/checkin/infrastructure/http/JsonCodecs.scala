package checkin.infrastructure.http

import checkin.application._
import io.circe.{Decoder, Encoder, Json}
import io.circe.syntax._

/**
 * Codificación/decodificación JSON - DTOs de aplicación.
 */
object JsonCodecs {


  implicit val equipajeInputDecoder: Decoder[EquipajeInput] =
    Decoder.forProduct2("codigoRFID", "peso")(EquipajeInput.apply)

  implicit val checkInCommandDecoder: Decoder[CheckInCommand] =
    Decoder.forProduct6(
      "pasajeroId", "nombrePasajero", "documento",
      "email", "vueloId", "equipajes"
    )(CheckInCommand.apply)

  implicit val checkInResponseEncoder: Encoder[CheckInResponse] =
    Encoder.forProduct4(
      "pasajeroId", "vueloId",
      "equipajesRegistrados", "eventosPublicados"
    )(r => (r.pasajeroId, r.vueloId, r.equipajesRegistrados, r.eventosPublicados))

  /**
   * Encoder del ADT de errores. Devuelve un JSON con el tipo y el mensaje,
   * para que el cliente del API sepa qué error fue.
   */
  implicit val checkInErrorEncoder: Encoder[CheckInError] = Encoder.instance { err =>
    val tipo = err match {
      case _: CheckInError.PasajeroInvalido => "PasajeroInvalido"
      case _: CheckInError.EquipajeExcedePeso => "EquipajeExcedePeso"
      case _: CheckInError.ErrorPublicacionEvento => "ErrorPublicacionEvento"
      case _: CheckInError.ErrorPersistencia => "ErrorPersistencia"
      case _: CheckInError.SinEquipajes => "SinEquipajes"
    }
    Json.obj(
      "error"   -> tipo.asJson,
      "mensaje" -> err.mensaje.asJson
    )
  }
}