import java.time.LocalDateTime
import java.util.UUID

import config.KafkaProducerConfig
import models.BaggageEvent
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}

object Main {
  def main(args: Array[String]): Unit = {
    val topic = "maleta-checkin"

    val producer = new KafkaProducer[String, String](KafkaProducerConfig.properties)

    val event = BaggageEvent(
      eventId = UUID.randomUUID().toString,
      eventType = "Maleta registrada",
      timestamp = LocalDateTime.now().toString,
      baggageId = "BG-001",
      flightId = "AV-1234",
      location = "Check-in",
      status = "REGISTRADA",
      riskLevel = "BAJO",
      details = "Registro inicial de maleta en el sistema"
    )

    val record = new ProducerRecord[String, String](
      topic,
      event.baggageId,
      event.toJson
    )

    try {
      val metadata = producer.send(record).get()
      println("Evento enviado correctamente a Kafka.")
      println(s"Tópico: ${metadata.topic()}")
      println(s"Partición: ${metadata.partition()}")
      println(s"Offset: ${metadata.offset()}")
      println("Contenido del evento:")
      println(event.toJson)
    } catch {
      case e: Exception =>
        println("Error al enviar el evento a Kafka:")
        e.printStackTrace()
    } finally {
      producer.close()
    }
  }
}