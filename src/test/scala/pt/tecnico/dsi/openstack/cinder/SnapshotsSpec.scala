package pt.tecnico.dsi.openstack.cinder

//import scala.annotation.nowarn
import scala.concurrent.duration.DurationInt
import cats.effect.{IO, Resource}
import org.scalatest.{Assertion, OptionValues}
import pt.tecnico.dsi.openstack.cinder.models.{Snapshot, Volume}
import pt.tecnico.dsi.openstack.cinder.services.Snapshots
import pt.tecnico.dsi.openstack.keystone.models.Project
import squants.information.InformationConversions._

class SnapshotsSpec extends Utils with OptionValues {
  val withStubSnapshot: Resource[IO, (Snapshots[IO], Project, Snapshot.Create, Snapshot)] = {
    val creates = for {
      admin <- adminProject
      volumes = cinder.volumes(admin.id)
      volume <- volumes(Volume.Create(1.gibibytes, name = Some(randomName()), description = Some("a description")))
      // When a Volume is created its status is set to creating. We asynchronously wait bit so the status can hopefully become available.
      _ <- IO.sleep(5.second)
      snapshots = cinder.snapshots(admin.id)
      snapshotCreate = Snapshot.Create(randomName(), volume.id, Some("a description"))
      snapshot <- snapshots.create(snapshotCreate)
    } yield (snapshots, admin, snapshotCreate, snapshot)

    Resource.make(creates) { case (snapshots, _, _, snapshot) =>
      // When a Volume is created its status is set to creating. We cannot delete a volume with status creating.
      // We asynchronously wait bit so the status can hopefully become available.
      IO.sleep(1.second) *> snapshots.delete(snapshot.id)
    }
  }

  "Snapshots service" should {
    "create volumes" in withStubSnapshot.use[IO, Assertion] { case (_, _, snapshotCreate, snapshot) =>
      IO.pure {
        // Creating a Snapshot is not an idempotent operation because:
        //  The endpoint always creates new Snapshots even if the name is the same
        //  It also does not return a Conflict in any scenario
        snapshot.name shouldBe snapshotCreate.name
        snapshot.volumeId shouldBe snapshotCreate.volumeId
        snapshot.description shouldBe snapshotCreate.description
      }
    }
    /*
    "delete volumes" in withStubSnapshot.use[IO, Assertion] { case (volumes, _, _, volume) =>
      val delete = IO.sleep(2.second) *> volumes.delete(volume.id)
      delete.idempotently(_ shouldBe ())
    }
    "list summary volumes" in withStubSnapshot.use[IO, Assertion] { case (volumes, _, volumeCreate, volume) =>
      volumes.listSummary().idempotently { volumesSummary =>
        volumesSummary.exists(_.id == volume.id) shouldBe true
        volumesSummary.exists(_.name == volumeCreate.name) shouldBe true
      }
    }
    "list volumes" in withStubSnapshot.use[IO, Assertion] { case (volumes, adminProject, _, volume) =>
      volumes.list().idempotently { volumes =>
        val createdVolumeInList = volumes.find(_.id == volume.id)
        createdVolumeInList.value.projectId.value shouldBe adminProject.id
      }
    }
    "get a volume (existing id)" in withStubSnapshot.use[IO, Assertion] { case (volumes, adminProject, volumeCreate, volume) =>
      volumes.get(volume.id).idempotently { vol =>
        vol.value.id shouldBe volume.id
        vol.value.description shouldBe volumeCreate.description
        vol.value.name shouldBe volumeCreate.name
        vol.value.availabilityZone shouldBe "nova"
        vol.value.projectId.value shouldBe adminProject.id
        vol.value.size shouldBe volumeCreate.size
      }
    }
    "get a volume (non-existing id)" in withStubSnapshot.use[IO, Assertion] { case (volumes, _, _, _) =>
      volumes.get("non-existing-id").idempotently(_ shouldBe None)
    }

    s"show volumes" in withStubSnapshot.use[IO, Assertion] { case (_, _, _, model) =>
      //This line is a fail fast mechanism, and prevents false positives from the linter
      println(show"$model")
      IO("""show"$model"""" should compile): @nowarn
    }
    */
  }
}
