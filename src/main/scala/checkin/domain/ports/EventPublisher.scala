package checkin.domain.ports

import checkin.domain.event.EventoEquipaje

/**
 * Publicación de eventos al event bus.
 *
 * El dominio dice "quiero publicar este evento"; la implementación
 * concreta (KafkaEventPublisher en `infrastructure`) se encarga del
 * cómo: serializar a JSON, conexión TCP al broker, retries, etc.
 *
 * Esto permite, por ejemplo, reemplazar Kafka por RabbitMQ o un
 * publisher mock en tests sin tocar el caso de uso.
 */
trait EventPublisher {
  def publicar(evento: EventoEquipaje): Either[String, Unit]
  def cerrar(): Unit
}