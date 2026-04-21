package checkin.infrastructure.http

import cats.effect.{IO, Resource}
import com.comcast.ip4s._
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.Server
import org.http4s.server.middleware.Logger

/**
 * Constructor del servidor HTTP (Ember, el servidor nativo de http4s).
 *
 * Envuelve las rutas con un middleware de logging que imprime cada request
 * con su método, path y código de respuesta. Útil para depurar integración
 * con el equipo.
 *
 * Devuelve un `Resource[IO, Server]` — eso garantiza que el servidor se
 * cierra limpiamente cuando termina la app (Ctrl+C), sin conexiones colgadas.
 */
object HttpServer {

  def build(routes: CheckInRoutes, port: Int): Resource[IO, Server] = {
    val httpApp = Logger.httpApp(
      logHeaders = true,
      logBody    = false   // evitar loguear bodies grandes
    )(routes.routes.orNotFound)

    EmberServerBuilder.default[IO]
      .withHost(ipv4"0.0.0.0")
      .withPort(Port.fromInt(port).getOrElse(port"8081"))
      .withHttpApp(httpApp)
      .build
  }
}