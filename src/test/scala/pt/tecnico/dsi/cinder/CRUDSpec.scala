package pt.tecnico.dsi.cinder

import cats.effect.IO
import org.scalatest.Assertion
import pt.tecnico.dsi.cinder.models.WithId
import pt.tecnico.dsi.cinder.services.CRUDService

abstract class CRUDSpec[T]
  (val name: String, val service: CinderClient[IO] => CRUDService[IO, T], idempotent: Boolean = true) extends Utils {

  def stub: IO[T]

  val withSubCreated: IO[(WithId[T], CRUDService[IO, T])] =
    for {
      client <- scopedClient
      crudService = service(client)
      expected <- stub
      createdStub <- crudService.create(expected)
    } yield (createdStub, crudService)

  s"The ${name} service" should {
    s"create ${name}s" in {
      val createIO = for {
        client <- scopedClient
        expected <- stub
        createdStub <- service(client).create(expected)
      } yield (createdStub, expected)

      def test(t: (WithId[T], T)): Assertion = {
        val (createdStub, expected) = t
        createdStub.model shouldBe expected
      }

      if (idempotent) {
        createIO.idempotently(test)
      } else {
        createIO.map(test)
      }
    }

    s"list ${name}s" in {
      withSubCreated.flatMap { case (createdStub, service) =>
        service.list().compile.toList.idempotently(_ should contain (createdStub))
      }
    }

    s"get ${name}s" in {
      withSubCreated.flatMap { case (createdStub, service) =>
        service.get(createdStub.id).valueShouldIdempotentlyBe(createdStub)
      }
    }

    s"delete a ${name}" in {
      withSubCreated.flatMap { case (createdStub, service) =>
        service.delete(createdStub.id).valueShouldIdempotentlyBe(())
      }
    }
  }
}
