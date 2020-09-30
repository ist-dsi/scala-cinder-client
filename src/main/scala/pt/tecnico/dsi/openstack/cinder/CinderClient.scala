package pt.tecnico.dsi.openstack.cinder

import cats.effect.Sync
import org.http4s.Uri
import org.http4s.Uri.Path
import org.http4s.client.Client
import pt.tecnico.dsi.openstack.cinder.services._
import pt.tecnico.dsi.openstack.keystone.models.{ClientBuilder, Session}

object CinderClient extends ClientBuilder {
	final type OpenstackClient[F[_]] = CinderClient[F]
	final val `type`: String = "volumev3"

	override def apply[F[_]: Sync: Client](baseUri: Uri, session: Session): CinderClient[F] =
		new CinderClient[F](baseUri, session)
}
class CinderClient[F[_]: Sync](baseUri: Uri, session: Session)(implicit client: Client[F]) {
	val uri: Uri = {
		val lastSegment = baseUri.path.dropEndsWithSlash.segments.lastOption
		if (lastSegment.contains(Path.Segment("v3"))) baseUri else baseUri / "v3"
	}

	def quotas(adminProjectId: String) = new Quotas[F](uri / adminProjectId, session)
	def volumes(projectId: String) = new Volumes[F](uri / projectId, session)
}
