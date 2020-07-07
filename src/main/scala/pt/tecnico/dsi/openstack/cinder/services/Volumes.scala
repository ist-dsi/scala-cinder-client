package pt.tecnico.dsi.openstack.cinder.services

import cats.effect.Sync
import fs2.Stream
import org.http4s.client.Client
import org.http4s.{Header, Query, Uri}
import pt.tecnico.dsi.openstack.cinder.models.{Volume, VolumeSummary}
import pt.tecnico.dsi.openstack.common.models.WithId
import pt.tecnico.dsi.openstack.common.services.CrudService

final class Volumes[F[_]: Sync: Client](baseUri: Uri, authToken: Header)
  extends CrudService[F, Volume, Volume.Create, Volume.Update](baseUri, "volume", authToken) {

  /**
    * Creates a new volume.
    *
    * @note this method is not idempotent! The Openstack API creates a new Volume every time.
    * @param volume the volume create options.
    */
  override def create(volume: Volume.Create, extraHeaders: Header*): F[WithId[Volume]] = super.create(volume, extraHeaders:_*)

  /**
    * Lists summary information for all Block Storage volumes that the project can access.
 *
    * @param query extra query params to pass in the request.
    */
  def listSummary(query: Query = Query.empty): Stream[F, WithId[VolumeSummary]] = super.list[WithId[VolumeSummary]](pluralName, uri, query)

  override def list(query: Query, extraHeaders: Header*): Stream[F, WithId[Volume]] = super.list[WithId[Volume]](pluralName, uri / "detail", query, extraHeaders:_*)

  /**
    * Deletes a volume.
    *
    * @param id the id of the volume.
    * @param cascade remove any snapshots along with the volume.
    * @param force indicates whether to force delete a volume even if the volume is in deleting or error_deleting.
    */
  def delete(id: String, cascade: Boolean = false, force: Boolean = false): F[Unit] =
    super.delete((uri / id).+?("cascade", cascade).+?("force", force))
}
