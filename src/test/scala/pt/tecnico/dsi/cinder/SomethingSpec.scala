package pt.tecnico.dsi.cinder

import pt.tecnico.dsi.keystone.models.Project

class SomethingSpec extends Utils {
  "something" should {
    "do stuff" in {
      for {
        keystone <- keystoneClient
        adminProject <- keystone.projects.get("admin", keystone.session.user.domainId)
        dummyProject <- keystone.projects.create(Project("dummy", "dummy project", "default"))
        cinder <- client
        defaults <- cinder.quotas(adminProject.id).getDefaults(dummyProject.id)
        _ = println(defaults.toString)
      } yield defaults.volumes shouldBe 10
    }
  }
}
