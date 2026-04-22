package checkin.infrastructure.kafka

import checkin.domain.event.EventoEquipaje
import checkin.domain.ports.EventPublisher
import checkin.infrastructure.config.AppConfig

import io.circe.Json
import io.circe.syntax._

import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}
import org.apache.kafka.common.serialization.StringSerializer

import java.util.Properties
import scala.util.{Failure, Success, Try}

/**
 * Publicador de eventos a Kafka.
 *
 * Implementa el port `EventPublisher` del dominio. Se encarga de:
 *   1. Serializar el EventoEquipaje a JSON (Circe)
 *   2. Enviar al tópico correspondiente
 *   3. Esperar confirmación con `.get()` (sincrónico para simplificar MVP;
 *      en producción usaríamos Future/IO)
 */
class KafkaEventPublisher(config: AppConfig) extends EventPublisher {

  private val props = new Properties()
  props.put("bootstrap.servers", config.kafkaBootstrap)
  props.put("key.serializer",   classOf[StringSerializer].getName)
  props.put("value.serializer", classOf[StringSerializer].getName)
  props.put("acks",             config.acks)
  props.put("retries",          "3")
  props.put("client.id",        "checkin-service")

  private val producer = new KafkaProducer[String, String](props)

  override def publicar(evento: EventoEquipaje): Either[String, Unit] = {
    val json = serializarEvento(evento).noSpaces

    // La key del mensaje es el equipajeId → Kafka enruta todos los eventos
    // del mismo equipaje a la misma partición (ordenados).
    val record = new ProducerRecord[String, String](
      evento.topico,
      evento.equipajeId,
      json
    )

    Try(producer.send(record).get()) match {
      case Success(_)   =>
        println(s"[Kafka] ✓ Evento publicado → ${evento.topico} | ${evento.tipo}")
        Right(())
      case Failure(ex)  =>
        Left(s"Fallo enviando a Kafka: ${ex.getMessage}")
    }
  }

  override def cerrar(): Unit = {
    producer.flush()
    producer.close()
  }

  /** Serialización manual con Circe (sin macros — más predecible). */
  private def serializarEvento(e: EventoEquipaje): Json = {
    val payloadJson = Json.obj(
      e.payload.view.mapValues(Json.fromString).toSeq: _*
    )

    Json.obj(
      "eventId"    -> e.eventId.toString.asJson,
      "tipo"       -> e.tipo.toString.asJson,
      "equipajeId" -> e.equipajeId.asJson,
      "timestamp"  -> e.timestamp.toString.asJson,
      "topico"     -> e.topico.asJson,
      "payload"    -> payloadJson
    )
  }
}