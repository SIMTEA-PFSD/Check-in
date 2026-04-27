package checkin.infrastructure.http

import cats.effect.IO
import checkin.application.{CheckInCommand, CheckInError, CheckInUseCase}
import io.circe.Json
import io.circe.syntax._
import org.http4s._
import org.http4s.circe.CirceEntityDecoder._
import org.http4s.circe.CirceEntityEncoder._
import org.http4s.dsl.io._


class CheckInRoutes(useCase: CheckInUseCase) {
  import JsonCodecs._

  val routes: HttpRoutes[IO] = HttpRoutes.of[IO] {

    case GET -> Root / "health" =>
      Ok(Json.obj(
        "status"  -> "UP".asJson,
        "service" -> "checkin".asJson
      ))

    case req @ POST -> Root / "api" / "v1" / "checkin" =>
      req.attemptAs[CheckInCommand].value.flatMap {
        case Left(decodeFailure) =>
          BadRequest(Json.obj(
            "error" -> "ErrorDeDeserializacion".asJson,
            "mensaje" -> decodeFailure.getMessage.asJson
          ))

        case Right(cmd) =>
          IO.blocking(useCase.ejecutar(cmd)).flatMap {
            case Right(resp) => Created(resp.asJson)
            case Left(e) => val json = (e: CheckInError).asJson
              e match {
                case _: CheckInError.ErrorPublicacionEvento => InternalServerError(json)
                case _: CheckInError.ErrorPersistencia => InternalServerError(json)
                case _ => BadRequest(json)
              }
          }
      }
  }
}