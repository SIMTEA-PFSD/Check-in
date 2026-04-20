package checkin.application

import checkin.domain.event.EventoEquipaje
import checkin.domain.model.{Equipaje, Pasajero}
import checkin.domain.ports.{EquipajeRepository, EventPublisher, PasajeroRepository}

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

import scala.collection.mutable.ListBuffer

/**
 * Tests unitarios del CheckInUseCase.
 *
 * Observa cómo usamos MOCKS MANUALES (sin Mockito ni libs externas).
 * Esto es posible gracias a que los puertos son traits simples — ésta
 * es la razón principal por la que vale la pena esta arquitectura:
 * testabilidad total sin hacer red, sin levantar Kafka ni DB.
 */
class CheckInUseCaseSpec extends AnyFunSuite with Matchers {

  // ─── Mocks en memoria ───────────────────────────────────────

  class FakePasajeroRepo extends PasajeroRepository {
    val guardados = ListBuffer.empty[Pasajero]
    def guardar(p: Pasajero) = { guardados += p; Right(p) }
    def buscarPorId(id: String) = guardados.find(_.id == id)
    def buscarPorDocumento(d: String) = guardados.find(_.documento == d)
  }

  class FakeEquipajeRepo extends EquipajeRepository {
    val guardados = ListBuffer.empty[Equipaje]
    def guardar(e: Equipaje) = { guardados += e; Right(e) }
    def buscarPorId(id: String) = guardados.find(_.id == id)
    def buscarPorPasajero(p: String) = guardados.filter(_.pasajeroId == p).toList
  }

  class FakePublisher extends EventPublisher {
    val eventos = ListBuffer.empty[EventoEquipaje]
    def publicar(e: EventoEquipaje) = { eventos += e; Right(()) }
    def cerrar() = ()
  }

  // ─── Tests ──────────────────────────────────────────────────

  test("registra correctamente pasajero, equipajes y publica eventos") {
    val pRepo = new FakePasajeroRepo
    val eRepo = new FakeEquipajeRepo
    val pub   = new FakePublisher
    val useCase = new CheckInUseCase(pRepo, eRepo, pub)

    val cmd = CheckInCommand(
      pasajeroId     = "p-1",
      nombrePasajero = "Vale Lozano",
      documento      = "1000",
      email          = "v@x.com",
      vueloId        = "AV100",
      equipajes      = List(EquipajeInput("R1", 15.0), EquipajeInput("R2", 20.0))
    )

    val result = useCase.ejecutar(cmd)

    result.isRight shouldBe true
    pRepo.guardados.size shouldBe 1
    eRepo.guardados.size shouldBe 2
    pub.eventos.size     shouldBe 2
  }

  test("rechaza check-in si un equipaje excede el peso máximo") {
    val useCase = new CheckInUseCase(
      new FakePasajeroRepo, new FakeEquipajeRepo, new FakePublisher
    )

    val cmd = CheckInCommand(
      "p-1", "X", "123", "a@b.com", "V1",
      List(EquipajeInput("R1", 30.0))  // 30kg > 23kg
    )

    useCase.ejecutar(cmd) shouldBe a [Left[_, _]]
  }

  test("rechaza check-in si no hay equipajes") {
    val useCase = new CheckInUseCase(
      new FakePasajeroRepo, new FakeEquipajeRepo, new FakePublisher
    )

    val cmd = CheckInCommand("p-1", "X", "123", "a@b.com", "V1", List.empty)

    useCase.ejecutar(cmd) match {
      case Left(CheckInError.SinEquipajes(_)) => succeed
      case other => fail(s"Se esperaba SinEquipajes, se obtuvo: $other")
    }
  }
}