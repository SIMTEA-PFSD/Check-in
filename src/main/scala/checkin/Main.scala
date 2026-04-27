package checkin

import cats.effect.{ExitCode, IO, IOApp}
import checkin.application.CheckInUseCase
import checkin.infrastructure.config.AppConfig
import checkin.infrastructure.http.{CheckInRoutes, HttpServer}
import checkin.infrastructure.kafka.{KafkaEventPublisher, OutboxRelay}
import checkin.infrastructure.persistence.{
  DatabaseTransactor,
  PostgresEquipajeRepository,
  PostgresOutboxRepository,
  PostgresPasajeroRepository
}

import scala.concurrent.duration._


object Main extends IOApp {

  override def run(args: List[String]): IO[ExitCode] = {
    val config    = AppConfig.load()
    val publisher = new KafkaEventPublisher(config)

    implicit val rt = runtime

    val banner: String =
      s"""|
          |══════════════════════════════════════════════════
          |  Microservicio Check-In - Arriba
          |══════════════════════════════════════════════════
          |  HTTP        -> http://localhost:${config.httpPort}
          |  Kafka       -> ${config.kafkaBootstrap}
          |  Tópico      -> ${config.kafkaTopic}
          |  DB          -> ${config.db.url}
          |  Outbox poll -> cada ${config.outbox.pollIntervalSeconds}s (lote ${config.outbox.batchSize})
          |──────────────────────────────────────────────────
          |  Endpoints:
          |    GET  /health
          |    POST /api/v1/checkin
          |══════════════════════════════════════════════════
          |  (Ctrl+C para detener)
          |""".stripMargin

    val programa = for {
      xa <- DatabaseTransactor.build(config.db)
      server <- {
        val pasajeroRepo = new PostgresPasajeroRepository(xa)
        val equipajeRepo = new PostgresEquipajeRepository(xa)
        val outboxRepo   = new PostgresOutboxRepository(xa)

        val useCase = new CheckInUseCase(pasajeroRepo, equipajeRepo)
        val routes  = new CheckInRoutes(useCase)

        val relay = new OutboxRelay(
          outboxRepo,
          publisher,
          intervalo = config.outbox.pollIntervalSeconds.seconds,
          loteSize  = config.outbox.batchSize
        )

        // Lanzamos el relay como fiber al construir el server.
        // El fiber se cancela cuando el Resource del server se cierra.
        for {
          relayFiber <- cats.effect.Resource.make(relay.loop.start)(_.cancel)
          srv        <- HttpServer.build(routes, config.httpPort)
        } yield (srv, relayFiber)
      }
    } yield server

    programa.use { _ =>
      IO.println(banner) *> IO.never
    }.guarantee(IO(publisher.cerrar()))
     .as(ExitCode.Success)
  }
}