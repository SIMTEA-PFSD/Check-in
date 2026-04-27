package checkin.infrastructure.http

import cats.effect.{IO, Resource}
import com.comcast.ip4s._
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.Server
import org.http4s.server.middleware.Logger


object HttpServer {

  def build(routes: CheckInRoutes, port: Int): Resource[IO, Server] = {
    val httpApp = Logger.httpApp(logHeaders = true, logBody = false)(routes.routes.orNotFound)

    EmberServerBuilder.default[IO]
      .withHost(ipv4"0.0.0.0")
      .withPort(Port.fromInt(port).getOrElse(port"8081"))
      .withHttpApp(httpApp)
      .build
  }
}