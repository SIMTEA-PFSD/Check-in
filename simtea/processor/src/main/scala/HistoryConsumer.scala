import java.time.Duration
import java.util.Collections

import config.KafkaConsumerConfig
import org.apache.kafka.clients.consumer.KafkaConsumer

object HistoryConsumer {
  def main(args: Array[String]): Unit = {
    val topic = "maleta-historico"

    val consumer = new KafkaConsumer[String, String](KafkaConsumerConfig.properties)
    consumer.subscribe(Collections.singletonList(topic))

    println(s"Escuchando históricos del tópico: $topic")

    try {
      while (true) {
        val records = consumer.poll(Duration.ofSeconds(2))

        records.forEach { record =>
          println("HISTÓRICO RECIBIDO:")
          println(s"Clave: ${record.key()}")
          println(s"Valor: ${record.value()}")
          println("----------------------------------------")
        }
      }
    } catch {
      case e: Exception =>
        println("Error en el consumidor histórico:")
        e.printStackTrace()
    } finally {
      consumer.close()
    }
  }
}