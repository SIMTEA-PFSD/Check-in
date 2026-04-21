package checkin.infrastructure.http

import checkin.application._
import io.circe.{Decoder, Encoder, Json}
import io.circe.syntax._

/**
 * Codificación/decodificación JSON ↔ DTOs de aplicación.
 *
 * Usamos Circe con constructores manuales (forProductN) en vez de auto-
 * derivación por macros. Es más explícito y más fácil de depurar —
 * se ve exactamente qué campos JSON se mapean a qué campos del case class.
 */
object JsonCodecs {

  // ─── Decoders (JSON entrante → objetos Scala) ────────────

  implicit val equipajeInputDecoder: Decoder[EquipajeInput] =
    Decoder.forProduct2("codigoRFID", "peso")(EquipajeInput.apply)

  implicit val checkInCommandDecoder: Decoder[CheckInCommand] =
    Decoder.forProduct6(
      "pasajeroId", "nombrePasajero", "documento",
      "email", "vueloId", "equipajes"
    )(CheckInCommand.apply)

  // ─── Encoders (objetos Scala → JSON saliente) ────────────

  implicit val checkInResponseEncoder: Encoder[CheckInResponse] =
    Encoder.forProduct4(
      "pasajeroId", "vueloId",
      "equipajesRegistrados", "eventosPublicados"
    )(r => (r.pasajeroId, r.vueloId, r.equipajesRegistrados, r.eventosPublicados))

  /**
   * Encoder del ADT de errores. Devuelve un JSON con el tipo y el mensaje,
   * útil para que el cliente del API sepa qué error fue sin parsear strings.
   */
  implicit val checkInErrorEncoder: Encoder[CheckInError] = Encoder.instance { err =>
    val tipo = err match {
      case _: CheckInError.PasajeroInvalido       => "PasajeroInvalido"
      case _: CheckInError.EquipajeExcedePeso     => "EquipajeExcedePeso"
      case _: CheckInError.ErrorPublicacionEvento => "ErrorPublicacionEvento"
      case _: CheckInError.ErrorPersistencia      => "ErrorPersistencia"
      case _: CheckInError.SinEquipajes           => "SinEquipajes"
    }
    Json.obj(
      "error"   -> tipo.asJson,
      "mensaje" -> err.mensaje.asJson
    )
  }
}