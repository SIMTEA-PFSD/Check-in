import java.time.Duration
import java.time.LocalDateTime
import java.util.Collections
import java.util.UUID

import config.{KafkaConsumerConfig, KafkaProducerConfig}
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}
import rules.RiskRule

object Main {
  def main(args: Array[String]): Unit = {
    val inputTopic = "maleta-checkin"
    val alertTopic = "maleta-alertas"
    val historyTopic = "maleta-historico"

    val consumer = new KafkaConsumer[String, String](KafkaConsumerConfig.properties)
    val producer = new KafkaProducer[String, String](KafkaProducerConfig.properties)

    consumer.subscribe(Collections.singletonList(inputTopic))

    println(s"Escuchando eventos del tópico: $inputTopic")

    try {
      while (true) {
        val records = consumer.poll(Duration.ofSeconds(2))

        records.forEach { record =>
          println("Evento recibido desde Kafka:")
          println(s"Clave: ${record.key()}")
          println(s"Valor: ${record.value()}")
          println(s"Partición: ${record.partition()}")
          println(s"Offset: ${record.offset()}")

          val risk = RiskRule.evaluate(record.value())
          println(s"Resultado del procesamiento: $risk")

          val historyMessage =
            s"""{
               |  "historyId": "${UUID.randomUUID().toString}",
               |  "baggageId": "${record.key()}",
               |  "processedAt": "${LocalDateTime.now()}",
               |  "sourceTopic": "$inputTopic",
               |  "riskResult": "$risk",
               |  "originalEvent": ${record.value()}
               |}""".stripMargin

          val historyRecord = new ProducerRecord[String, String](
            historyTopic,
            record.key(),
            historyMessage
          )

          producer.send(historyRecord)
          println(s"Evento histórico publicado en tópico: $historyTopic")

          if (risk == "ALTO") {
            val alertMessage =
              s"""{
                 |  "alertType": "Riesgo alto detectado",
                 |  "baggageId": "${record.key()}",
                 |  "sourceTopic": "$inputTopic",
                 |  "message": "Maleta con nivel de riesgo ALTO. Notificar a seguridad."
                 |}""".stripMargin

            val alertRecord = new ProducerRecord[String, String](
              alertTopic,
              record.key(),
              alertMessage
            )

            producer.send(alertRecord)
            println(s"Alerta publicada en tópico: $alertTopic")
            println(alertMessage)
          }

          println("----------------------------------------")
        }
      }
    } catch {
      case e: Exception =>
        println("Error en el processor:")
        e.printStackTrace()
    } finally {
      consumer.close()
      producer.close()
    }
  }
}