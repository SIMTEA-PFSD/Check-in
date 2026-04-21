package checkin.infrastructure.http

import cats.effect.IO
import checkin.application.{CheckInCommand, CheckInError, CheckInUseCase}
import io.circe.Json
import io.circe.syntax._
import org.http4s._
import org.http4s.circe.CirceEntityDecoder._
import org.http4s.circe.CirceEntityEncoder._
import org.http4s.dsl.io._

/**
 * «adapter HTTP» — Adaptador de entrada REST.
 *
 * Traduce peticiones HTTP al comando de aplicación (CheckInCommand) y las
 * respuestas del caso de uso a códigos HTTP apropiados:
 *
 *   201 Created              → check-in exitoso
 *   400 Bad Request          → validación falló (peso, sin equipajes…)
 *   500 Internal Server Error → Kafka o persistencia falló
 *   200 OK                   → /health
 *
 * NO contiene lógica de negocio — delega todo al CheckInUseCase.
 */
class CheckInRoutes(useCase: CheckInUseCase) {
  import JsonCodecs._

  val routes: HttpRoutes[IO] = HttpRoutes.of[IO] {

    // ─── Health check (para Docker / Kubernetes / UI admin) ───
    case GET -> Root / "health" =>
      Ok(Json.obj(
        "status"  -> "UP".asJson,
        "service" -> "checkin".asJson
      ))

    // ─── Endpoint principal: registrar un check-in ────────────
    case req @ POST -> Root / "api" / "v1" / "checkin" =>
      req.attemptAs[CheckInCommand].value.flatMap {
        case Left(decodeFailure) =>
          BadRequest(Json.obj(
            "error"   -> "ErrorDeDeserializacion".asJson,
            "mensaje" -> decodeFailure.getMessage.asJson
          ))

        case Right(cmd) =>
          IO.blocking(useCase.ejecutar(cmd)).flatMap {
            case Right(resp) => Created(resp.asJson)
            case Left(e)     =>
              val json = (e: CheckInError).asJson
              e match {
                case _: CheckInError.ErrorPublicacionEvento => InternalServerError(json)
                case _: CheckInError.ErrorPersistencia      => InternalServerError(json)
                case _                                      => BadRequest(json)
              }
          }
      }
  }
}