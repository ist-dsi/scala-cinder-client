package pt.tecnico.dsi.openstack.cinder

import scala.concurrent.duration.DurationInt
import cats.effect.IO
import org.scalatest.{Assertion, OptionValues}
import pt.tecnico.dsi.openstack.cinder.models.Volume
import pt.tecnico.dsi.openstack.cinder.models.VolumeStatus.{Available, Creating}
import pt.tecnico.dsi.openstack.cinder.services.Volumes
import pt.tecnico.dsi.openstack.common.models.WithId
import squants.information.InformationConversions._

class VolumesSpec extends Utils with OptionValues {
  def withStubVolume(name: String)(f: (Volumes[IO], String, Volume.Create, WithId[Volume]) => IO[Assertion]): IO[Assertion] =
    for {
      keystone <- keystoneClient
      adminProject <- keystone.projects.get("admin", keystone.session.user.domainId)
      cinder <- client
      volumeCreate = Volume.Create(1.gibibytes, name = Some(name), description = Some("a description"))
      service = cinder.volumes(adminProject.id)
      volume <- service.create(volumeCreate)
      result <- f(service, adminProject.id, volumeCreate, volume)
      // When a Volume is created its status is set to creating. We cannot delete a volume with status creating.
      // We asynchronously wait bit so the status can hopefully become available.
      _ <- IO.sleep(1.second) *> cinder.volumes(adminProject.id).delete(volume.id)
    } yield result

  "Volumes service" should {
    "create volumes" in withStubVolume("create") { (_, _, volumeCreate, volume) =>
      IO.pure {
        // Creating a Volume is not an idempotent operation because:
        //  The endpoint always creates new Volumes even if the name is the same
        //  It also does not return a Conflict in any scenario
        volume.description shouldBe volumeCreate.description
        volume.name shouldBe volumeCreate.name
        volume.availabilityZone shouldBe "nova"
      }
    }
    "delete volumes" in withStubVolume("delete") { (service, _, _, volume) =>
      val delete = IO.sleep(2.second) *> service.delete(volume.id)
      delete.idempotently(_ shouldBe ())
    }
    "list summary volumes" in withStubVolume("list-summary") { (service, _, volumeCreate, volume) =>
      service.listSummary().compile.toList.idempotently { volumesSummary =>
        volumesSummary.exists(_.id == volume.id) shouldBe true
        volumesSummary.exists(_.name == volumeCreate.name) shouldBe true
      }
    }
    "list volumes" in withStubVolume("list") { (service, adminProjectId, _, volume) =>
      service.list().compile.toList.idempotently { volumes =>
        val createdVolumeInList = volumes.find(_.id == volume.id)
        createdVolumeInList.value.projectId.value shouldBe adminProjectId
        createdVolumeInList.value.status should (equal (Available) or equal(Creating))
      }
    }
    "get a volume" in withStubVolume("get") { (service, adminProjectId, volumeCreate, volume) =>
      service.get(volume.id).idempotently { vol =>
        vol.id shouldBe volume.id
        vol.description shouldBe volumeCreate.description
        vol.name shouldBe volumeCreate.name
        vol.availabilityZone shouldBe "nova"
        vol.projectId.value shouldBe adminProjectId
        vol.size shouldBe volumeCreate.size
      }
    }
  }
}
