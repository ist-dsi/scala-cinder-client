package pt.tecnico.dsi.openstack.cinder

import scala.annotation.nowarn
import cats.effect.{IO, Resource}
import cats.syntax.show._
import org.scalatest.Assertion
import pt.tecnico.dsi.openstack.cinder.models.{Quota, QuotaUsage}
import pt.tecnico.dsi.openstack.cinder.services.Quotas
import pt.tecnico.dsi.openstack.common.models.Usage
import pt.tecnico.dsi.openstack.keystone.models.Project
import squants.information.InformationConversions._

class QuotasSpec extends Utils {
  val withStub: Resource[IO, (Quotas[IO], Project)] = withStubProject.evalMap { project =>
    adminProject.map(admin => (cinder.quotas(admin.id), project))
  }

  // These are the default quotas for the cinder we are testing against
  val defaultQuotas = Quota(
    volumes = 10,
    volumesPerType = Map.empty,
    snapshots = 10,
    snapshotsPerType = Map.empty,
    backups = 10,
    groups = 10,
    maxVolumeSize = -1.gibibytes,
    backupsStorage = 1000.gibibytes,
    volumesStorage = 1000.gibibytes,
    volumesStoragePerType = Map.empty,
  )
  val defaultUsageQuotas = QuotaUsage(
    volumes = Usage(0, 10, 0),
    volumesPerType = Map.empty,
    snapshots = Usage(0, 10, 0),
    snapshotsPerType = Map.empty,
    backups = Usage(0, 10, 0),
    groups = Usage(0, 10, 0),
    maxVolumeSize = Usage(0.gibibytes, -1.gibibytes, 0.gibibytes),
    backupsStorage = Usage(0.gibibytes, 1000.gibibytes, 0.gibibytes),
    volumesStorage = Usage(0.gibibytes, 1000.gibibytes, 0.gibibytes),
    volumesStoragePerType = Map.empty,
  )

  "Quotas service" should {
    "apply quotas for a project (existing id)" in withStub.use[IO, Assertion] { case (quotas, project) =>
      quotas.apply(project.id).idempotently(_ shouldBe defaultQuotas)
    }
    "apply quotas for a project (non-existing id)" in withStub.use[IO, Assertion] { case (quotas, _) =>
      // This is not a mistake in the test. Cinder does return a Quota even if the project does not exist :faceplam:
      quotas.apply("non-existing-id").idempotently(_ shouldBe defaultQuotas)
    }

    "apply usage quotas for a project (existing id)" in withStub.use[IO, Assertion] { case (quotas, project) =>
      quotas.applyUsage(project.id).idempotently(_ shouldBe defaultUsageQuotas)
    }
    "apply usage quotas for a project (non-existing id)" in withStub.use[IO, Assertion] { case (quotas, _) =>
      // This is not a mistake in the test. Cinder does return a Quota even if the project does not exist :faceplam:
      quotas.applyUsage("non-existing-id").idempotently(_ shouldBe defaultUsageQuotas)
    }

    "apply default quotas for a project (existing id)" in withStub.use[IO, Assertion] { case (quotas, project) =>
      quotas.applyDefaults(project.id).idempotently(_ shouldBe defaultQuotas)
    }
    "apply default quotas for a project (non-existing id)" in withStub.use[IO, Assertion] { case (quotas, _) =>
      // This is not a mistake in the test. Cinder does return a Quota even if the project does not exist :faceplam:
      quotas.applyDefaults("non-existing-id").idempotently(_ shouldBe defaultQuotas)
    }

    "update quotas for a project" in withStub.use[IO, Assertion] { case (quotas, project) =>
      val newQuotas = Quota.Update(volumes = Some(25), maxVolumeSize = Some(25.gibibytes))
      quotas.update(project.id, newQuotas).idempotently { quota =>
        quota.volumes shouldBe 25
        quota.maxVolumeSize shouldBe 25.gibibytes
      }
    }
    "delete quotas for a project" in withStub.use[IO, Assertion] { case (quotas, project) =>
      quotas.delete(project.id).idempotently(_ shouldBe ())
    }

    s"show quotas" in withStub.use[IO, Assertion] { case (quotas, project) =>
      quotas.applyDefaults(project.id).map { quotas =>
        //This line is a fail fast mechanism, and prevents false positives from the linter
        println(show"$quotas")
        """show"$quotas"""" should compile: @nowarn
      }
    }

    s"show quota usage" in withStub.use[IO, Assertion] { case (quotas, project) =>
      quotas.applyUsage(project.id).map { usage =>
        //This line is a fail fast mechanism, and prevents false positives from the linter
        println(show"$usage")
        """show"$usage"""" should compile: @nowarn
      }
    }
  }
}
