package pt.tecnico.dsi.openstack.cinder

import cats.effect.IO
import pt.tecnico.dsi.openstack.cinder.models.Quota
import pt.tecnico.dsi.openstack.keystone.models.Project
import squants.information.InformationConversions._

class QuotasSpec extends Utils {
  val withStubProject: IO[(CinderClient[IO], String, String)] =
    for {
      keystone <- keystoneClient
      adminProject <- keystone.projects.get("admin", keystone.session.user.domainId)
      dummyProject <- keystone.projects.create(Project("dummy", "dummy project", "default"))
      cinder <- client
    } yield (cinder, adminProject.id, dummyProject.id)

  "Quotas service" should {
    "get default quotas for a project" in withStubProject.flatMap { case (cinder, adminProjectId, dummyProjectId) =>
      cinder.quotas(adminProjectId).getDefaults(dummyProjectId).idempotently(_.volumes shouldBe 10)
    }
    "get quotas for a project" in withStubProject.flatMap { case (cinder, adminProjectId, dummyProjectId) =>
      cinder.quotas(adminProjectId).get(dummyProjectId).idempotently(_.volumes shouldBe 10)
    }
    "get usage quotas for a project" in withStubProject.flatMap { case (cinder, adminProjectId, dummyProjectId) =>
      cinder.quotas(adminProjectId).getUsage(dummyProjectId).idempotently(_.volumes.limit shouldBe 10)
    }
    "update quotas for a project" in withStubProject.flatMap { case (cinder, adminProjectId, dummyProjectId) =>
      val newQuotas = Quota.Update(volumes = Some(25), maxVolumeSize = Some(25.gibibytes))
      cinder.quotas(adminProjectId).update(dummyProjectId, newQuotas).idempotently { quota =>
        quota.volumes shouldBe 25
        quota.maxVolumeSize shouldBe 25.gibibytes
      }
    }
    "delete quotas for a project" in withStubProject.flatMap { case (cinder, adminProjectId, dummyProjectId) =>
      cinder.quotas(adminProjectId).delete(dummyProjectId).idempotently(_ shouldBe ())
    }
  }
}
