package pt.tecnico.dsi.cinder

import cats.effect.Sync
import org.http4s.client.Client
import org.http4s.{Header, Uri}
import pt.tecnico.dsi.cinder.services._

class CinderClient[F[_]: Sync](baseUri: Uri, authToken: Header)(implicit client: Client[F]) {
	val uri: Uri = baseUri / "v3"

	val quotas = new Quotas[F](uri, authToken)
	val volumes = new Volumes[F](uri, authToken)
}