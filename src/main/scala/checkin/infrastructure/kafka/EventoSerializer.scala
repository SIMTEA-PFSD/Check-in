package checkin.infrastructure.kafka

import checkin.domain.event.EventoEquipaje
import io.circe.Json
import io.circe.syntax._

/**
 * Convierte un EventoEquipaje del dominio en un JSON compacto
 * listo para guardarse en el Outbox o enviarse a Kafka.
 */
object EventoSerializer {

  def aJson(e: EventoEquipaje): String = {

    // Convierte el payload Map[String, String] en un objeto JSON.
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
    ).noSpaces
  }
}