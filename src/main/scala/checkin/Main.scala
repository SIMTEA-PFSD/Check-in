package checkin

import checkin.application.{CheckInCommand, CheckInUseCase, EquipajeInput}
import checkin.infrastructure.config.AppConfig
import checkin.infrastructure.kafka.KafkaEventPublisher
import checkin.infrastructure.persistence.{
  InMemoryEquipajeRepository,
  InMemoryPasajeroRepository
}

import java.util.UUID

/**
 * ═════════════════════════════════════════════════════════════
 *  PUNTO DE ENTRADA DEL MICROSERVICIO CHECK-IN
 * ═════════════════════════════════════════════════════════════
 *
 * AQUÍ es donde se conectan TODAS las capas de la arquitectura.
 * Es el único lugar donde se "eligen" las implementaciones concretas
 * de los puertos. Patrón: Composition Root.
 *
 * Para correr:
 *   sbt run
 *
 * Con Kafka en Docker:
 *   KAFKA_BOOTSTRAP=localhost:9092 sbt run
 */
object Main extends App {

  println("══════════════════════════════════════════════════")
  println("  Microservicio Check-In · Iniciando…")
  println("══════════════════════════════════════════════════")

  // 1. Cargar configuración
  val config = AppConfig.load()
  println(s"[Config] Kafka bootstrap → ${config.kafkaBootstrap}")
  println(s"[Config] Tópico          → ${config.kafkaTopic}")

  // 2. Crear los adaptadores de infraestructura (implementan los ports)
  val pasajeroRepo = new InMemoryPasajeroRepository
  val equipajeRepo = new InMemoryEquipajeRepository
  val publisher    = new KafkaEventPublisher(config)

  // 3. Inyectar los adaptadores al caso de uso
  val checkInUseCase = new CheckInUseCase(pasajeroRepo, equipajeRepo, publisher)

  // 4. DEMO: simula una solicitud de check-in
  val cmdDemo = CheckInCommand(
    pasajeroId     = UUID.randomUUID().toString,
    nombrePasajero = "Paula Valentina Lozano",
    documento      = "1000123456",
    email          = "vale@example.com",
    vueloId        = "AV8321",
    equipajes      = List(
      EquipajeInput(codigoRFID = "RFID-001", peso = 18.5),
      EquipajeInput(codigoRFID = "RFID-002", peso = 21.0)
    )
  )

  println("\n[Check-in] Ejecutando comando de prueba…")

  checkInUseCase.ejecutar(cmdDemo) match {
    case Right(resp) =>
      println("\n✓ CHECK-IN EXITOSO")
      println(s"  Pasajero:      ${resp.pasajeroId}")
      println(s"  Vuelo:         ${resp.vueloId}")
      println(s"  Equipajes:     ${resp.equipajesRegistrados.mkString(", ")}")
      println(s"  Eventos Kafka: ${resp.eventosPublicados}")

    case Left(error) =>
      println(s"\n✗ ERROR EN CHECK-IN: ${error.mensaje}")
  }

  // 5. Cerrar recursos
  publisher.cerrar()
  println("\n[Shutdown] Recursos cerrados. Fin.")
}