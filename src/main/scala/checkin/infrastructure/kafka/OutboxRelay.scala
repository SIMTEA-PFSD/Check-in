package checkin.infrastructure.kafka

import cats.effect.IO
import checkin.domain.ports.{EventPublisher, OutboxRepository}

import scala.concurrent.duration._

/**
 * Proceso en segundo plano encargado de publicar en Kafka
 * los eventos pendientes guardados en la tabla outbox_events.
 */
class OutboxRelay(
  outboxRepo: OutboxRepository,
  publisher:  EventPublisher,
  intervalo:  FiniteDuration,
  loteSize:   Int
) {

  /**
   * Loop infinito:
   * ejecuta una pasada, espera el intervalo configurado y repite.
   */
  val loop: IO[Unit] =
    (unaPasada >> IO.sleep(intervalo)).foreverM

  private def unaPasada: IO[Unit] = IO {
    val pendientes = outboxRepo.obtenerPendientes(loteSize)

    if (pendientes.nonEmpty) {
      println(s"[OutboxRelay] Procesando ${pendientes.size} pendiente(s)...")

      pendientes.foreach { ev =>
        publisher.publicar(ev.topico, ev.clave, ev.payload) match {

          case Right(_) =>
            // Si Kafka confirma la publicación, marcamos el evento como publicado.
            outboxRepo.marcarPublicado(ev.eventId)
            println(s"[OutboxRelay] ✓ ${ev.eventId} → ${ev.topico}")

          case Left(err) =>
            // Si Kafka falla, dejamos el evento pendiente y registramos el error.
            outboxRepo.marcarError(ev.eventId, err)
            println(s"[OutboxRelay] ✗ ${ev.eventId} → ${err}")
        }
      }
    }
  }.handleErrorWith { ex =>
    // Evita que una excepción detenga definitivamente el relay.
    IO(println(s"[OutboxRelay] Error en pasada (continúa): ${ex.getMessage}"))
  }
}