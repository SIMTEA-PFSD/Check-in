package checkin.infrastructure.kafka

import checkin.domain.ports.EventPublisher
import checkin.infrastructure.config.AppConfig

import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}
import org.apache.kafka.common.serialization.StringSerializer

import java.util.Properties
import scala.util.{Failure, Success, Try}

/**
 * Adaptador de salida hacia Kafka.
 * Implementa el puerto EventPublisher usando el KafkaProducer oficial.
 */
class KafkaEventPublisher(config: AppConfig) extends EventPublisher {

  private val props = new Properties()

  // Broker de Kafka al que se conecta el producer.
  props.put("bootstrap.servers", config.kafkaBootstrap)

  // Tanto la clave como el payload se envían como String.
  props.put("key.serializer", classOf[StringSerializer].getName)
  props.put("value.serializer", classOf[StringSerializer].getName)

  // Nivel de confirmación configurado para publicación.
  props.put("acks", config.acks)

  // Reintentos ante errores transitorios de publicación.
  props.put("retries", "3")
  props.put("client.id", "checkin-service")

  // Timeouts reducidos para evitar bloqueos largos si Kafka está caído.
  props.put("max.block.ms", "5000")
  props.put("delivery.timeout.ms","10000")
  props.put("request.timeout.ms","5000")

  private val producer = new KafkaProducer[String, String](props)

  override def publicar(
    topico: String,
    clave: String,
    payload:String
  ): Either[String, Unit] = {

    // ProducerRecord representa el mensaje concreto que se enviará a Kafka.
    val record = new ProducerRecord[String, String](topico, clave, payload)

    // En esta versión esperamos la confirmación de Kafka de forma síncrona.
    Try(producer.send(record).get()) match {
      case Success(_)  => Right(())
      case Failure(ex) => Left(s"Fallo enviando a Kafka: ${ex.getMessage}")
    }
  }

  override def cerrar(): Unit = {
    producer.flush()
    producer.close()
  }
}