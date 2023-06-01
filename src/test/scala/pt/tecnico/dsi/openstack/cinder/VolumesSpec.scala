package pt.tecnico.dsi.openstack.cinder

import scala.concurrent.duration.DurationInt
import cats.effect.{IO, Resource}
import cats.syntax.show.*
import org.scalatest.OptionValues
import pt.tecnico.dsi.openstack.cinder.models.Volume
import pt.tecnico.dsi.openstack.cinder.services.Volumes
import pt.tecnico.dsi.openstack.keystone.models.Project
import squants.information.InformationConversions.*

class VolumesSpec extends Utils with OptionValues:
  val withStubVolume: Resource[IO, (Volumes[IO], Project, Volume.Create, Volume)] =
    val create = withRandomName { name =>
      val volumeCreate = Volume.Create(1.gibibytes, name = name, description = Some("a description"))
      for
        admin <- adminProject
        volumes = cinder.volumes(admin.id)
        volume <- volumes.create(volumeCreate)
      yield (volumes, admin, volumeCreate, volume)
    }
    
    Resource.make(create) { case (volumes, _, _, volume) =>
      // When a Volume is created its status is set to creating. We cannot delete a volume with status creating.
      // We asynchronously wait bit so the status can hopefully become available.
      IO.sleep(1.second) *> volumes.delete(volume.id)
    }
  
  "Volumes service" should:
    "create volumes" in withStubVolume.use { case (_, _, volumeCreate, volume) =>
      IO.pure:
        volume.description shouldBe volumeCreate.description
        volume.name shouldBe volumeCreate.name
        volume.availabilityZone shouldBe "nova"
    }
    "delete volumes" in withStubVolume.use { case (volumes, _, _, volume) =>
      val delete = IO.sleep(2.second) *> volumes.delete(volume.id)
      delete.idempotently(_ shouldBe ())
    }
    "list summary volumes" in withStubVolume.use { case (volumes, _, volumeCreate, volume) =>
      volumes.listSummary().idempotently { volumesSummary =>
        volumesSummary.exists(_.id == volume.id) shouldBe true
        volumesSummary.exists(_.name == volumeCreate.name) shouldBe true
      }
    }
    "list volumes" in withStubVolume.use { case (volumes, adminProject, _, volume) =>
      volumes.list().idempotently { volumes =>
        val createdVolumeInList = volumes.find(_.id == volume.id)
        createdVolumeInList.value.projectId.value shouldBe adminProject.id
      }
    }
    "get a volume (existing id)" in withStubVolume.use { case (volumes, adminProject, volumeCreate, volume) =>
      volumes.get(volume.id).idempotently { vol =>
        vol.value.id shouldBe volume.id
        vol.value.description shouldBe volumeCreate.description
        vol.value.name shouldBe volumeCreate.name
        vol.value.availabilityZone shouldBe "nova"
        vol.value.projectId.value shouldBe adminProject.id
        vol.value.size shouldBe volumeCreate.size
      }
    }
    "get a volume (non-existing id)" in withStubVolume.use { case (volumes, _, _, _) =>
      volumes.get("non-existing-id").idempotently(_ shouldBe None)
    }
    
    s"show volumes" in withStubVolume.use { case (_, _, _, model) =>
      //This line is a fail fast mechanism, and prevents false positives from the linter
      println(show"$model")
      IO("""show"$model"""" should compile)
    }
