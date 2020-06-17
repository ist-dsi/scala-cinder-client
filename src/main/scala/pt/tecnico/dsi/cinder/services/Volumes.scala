package pt.tecnico.dsi.cinder.services

import cats.effect.Sync
import cats.syntax.flatMap._
import fs2.Stream
import org.http4s.Status.{Forbidden, NotFound, Successful}
import org.http4s.client.{Client, UnexpectedStatus}
import org.http4s.{Header, Query, Uri}
import pt.tecnico.dsi.cinder.models.{Volume, WithId}

final class Volumes[F[_]: Sync: Client](baseUri: Uri, authToken: Header) extends CRUDService[F, Volume](baseUri, "volume", authToken) {
  import dsl._

  /**
    * @param name filters the response by a domain name.
    * @param enabled filters the response by either enabled (true) or disabled (false) domains.
    * @return a stream of domains filtered by the various parameters.
    */
  def list(name: Option[String] = None, enabled: Option[Boolean]): Stream[F, WithId[Volume]] =
    list(Query.fromVector(Vector(
      "name" -> name,
      "enabled" -> enabled.map(_.toString),
    )))

  /**
    * Get detailed information about the domain specified by name.
    *
    * @param name the domain name
    * @return the domain matching the name.
    */
  def getByName(name: String): F[WithId[Volume]] = {
    // A domain name is globally unique across all domains.
    list(Query.fromPairs("name" -> name)).compile.lastOrError
  }

  override def create(domain: Volume): F[WithId[Volume]] = createHandleConflict(domain) { _ =>
    getByName(domain.name).flatMap(existingDomain => update(existingDomain.id, domain))
  }
}
