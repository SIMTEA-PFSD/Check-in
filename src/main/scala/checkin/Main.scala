package checkin

import cats.effect.{ExitCode, IO, IOApp}
import checkin.application.CheckInUseCase
import checkin.infrastructure.config.AppConfig
import checkin.infrastructure.http.{CheckInRoutes, HttpServer}
import checkin.infrastructure.kafka.KafkaEventPublisher
import checkin.infrastructure.persistence.{
  DatabaseTransactor,
  PostgresEquipajeRepository,
  PostgresPasajeroRepository
}

/**
 * ═════════════════════════════════════════════════════════════
 *  PUNTO DE ENTRADA DEL MICROSERVICIO CHECK-IN
 * ═════════════════════════════════════════════════════════════
 *
 *  Composition Root:
 *   1. Carga configuración
 *   2. Abre el pool de Postgres (Resource → se cierra solo al apagar)
 *   3. Instancia los adaptadores de infrastructure
 *   4. Construye el caso de uso (inyecta los puertos)
 *   5. Arranca el servidor HTTP en el puerto configurado
 *   6. Mantiene el servidor vivo hasta Ctrl+C
 *   7. Al cerrar, libera productor Kafka + pool DB + servidor HTTP
 *
 *  Extender IOApp nos da un runtime funcional con manejo automático
 *  de señales (SIGINT) y cierre limpio de recursos.
 */
object Main extends IOApp {

  override def run(args: List[String]): IO[ExitCode] = {

    // 1. Configuración
    val config = AppConfig.load()

    // 2. Kafka (productor) — no es Resource, se cierra en guarantee()
    val publisher = new KafkaEventPublisher(config)

    // IORuntime implícito que necesitan los repos para unsafeRunSync
    implicit val rt = runtime

    val banner: String =
      s"""|
          |══════════════════════════════════════════════════
          |  Microservicio Check-In · ARRIBA
          |══════════════════════════════════════════════════
          |  HTTP   → http://localhost:${config.httpPort}
          |  Kafka  → ${config.kafkaBootstrap}
          |  Tópico → ${config.kafkaTopic}
          |  DB     → ${config.db.url}
          |──────────────────────────────────────────────────
          |  Endpoints disponibles:
          |    · GET  /health
          |    · POST /api/v1/checkin
          |══════════════════════════════════════════════════
          |  (Ctrl+C para detener)
          |""".stripMargin

    // 3. Recursos (DB pool + HTTP server) — se cierran en orden inverso.
    val programa = for {
      xa     <- DatabaseTransactor.build(config.db)
      server <- {
        val pasajeroRepo = new PostgresPasajeroRepository(xa)
        val equipajeRepo = new PostgresEquipajeRepository(xa)
        val useCase      = new CheckInUseCase(pasajeroRepo, equipajeRepo, publisher)
        val routes       = new CheckInRoutes(useCase)
        HttpServer.build(routes, config.httpPort)
      }
    } yield server

    programa.use { _ =>
      IO.println(banner) *> IO.never
    }.guarantee(IO(publisher.cerrar()))
     .as(ExitCode.Success)
  }
}