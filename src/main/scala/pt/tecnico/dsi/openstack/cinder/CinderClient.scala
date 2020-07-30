package pt.tecnico.dsi.openstack.cinder

import cats.effect.Sync
import org.http4s.Uri.Path
import org.http4s.client.Client
import org.http4s.{Header, Uri}
import pt.tecnico.dsi.openstack.cinder.services._

class CinderClient[F[_]: Sync](baseUri: Uri, authToken: Header)(implicit client: Client[F]) {
	val uri: Uri = {
		val lastSegment = baseUri.path.dropEndsWithSlash.segments.lastOption
		if (lastSegment.contains(Path.Segment("v3"))) baseUri else baseUri / "v3"
	}

	def quotas(adminProjectId: String) = new Quotas[F](uri / adminProjectId, authToken)
	def volumes(projectId: String) = new Volumes[F](uri / projectId, authToken)
}
