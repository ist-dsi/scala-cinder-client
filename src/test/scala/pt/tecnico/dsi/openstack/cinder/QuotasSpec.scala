package pt.tecnico.dsi.openstack.cinder

import cats.effect.{IO, Resource}
import org.scalatest.Assertion
import pt.tecnico.dsi.openstack.cinder.models.Quota
import pt.tecnico.dsi.openstack.cinder.services.Quotas
import pt.tecnico.dsi.openstack.common.models.WithId
import pt.tecnico.dsi.openstack.keystone.models.Project
import squants.information.InformationConversions._

class QuotasSpec extends Utils {
  val withStub: Resource[IO, (Quotas[IO], WithId[Project])] = withStubProject.evalMap { project =>
    adminProject.map(admin => (cinder.quotas(admin.id), project))
  }

  "Quotas service" should {
    "get default quotas for a project" in withStub.use[IO, Assertion] { case (quotas, project) =>
      quotas.getDefaults(project.id).idempotently(_.volumes shouldBe 10)
    }
    "get quotas for a project" in withStub.use[IO, Assertion] { case (quotas, project) =>
      quotas.get(project.id).idempotently(_.volumes shouldBe 10)
    }
    "get usage quotas for a project" in withStub.use[IO, Assertion] { case (quotas, project) =>
      quotas.getUsage(project.id).idempotently(_.volumes.limit shouldBe 10)
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
  }
}
