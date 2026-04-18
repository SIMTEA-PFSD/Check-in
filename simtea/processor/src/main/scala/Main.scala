import java.time.Duration
import java.util.Collections

import config.KafkaConsumerConfig
import org.apache.kafka.clients.consumer.KafkaConsumer

object Main {
  def main(args: Array[String]): Unit = {
    val topic = "maleta-checkin"

    val consumer = new KafkaConsumer[String, String](KafkaConsumerConfig.properties)
    consumer.subscribe(Collections.singletonList(topic))

    println(s"Escuchando eventos del tópico: $topic")

    try {
      while (true) {
        val records = consumer.poll(Duration.ofSeconds(2))

        records.forEach { record =>
          println("Evento recibido desde Kafka:")
          println(s"Clave: ${record.key()}")
          println(s"Valor: ${record.value()}")
          println(s"Partición: ${record.partition()}")
          println(s"Offset: ${record.offset()}")
          println("----------------------------------------")
        }
      }
    } catch {
      case e: Exception =>
        println("Error en el consumidor:")
        e.printStackTrace()
    } finally {
      consumer.close()
    }
  }
}