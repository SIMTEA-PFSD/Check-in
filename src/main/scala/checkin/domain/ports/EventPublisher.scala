package checkin.domain.ports


trait EventPublisher {
  def publicar(topico: String, clave: String, payload: String): Either[String, Unit]
  def cerrar(): Unit
}