package pt.tecnico.dsi.cinder.services

import cats.effect.Sync
import org.http4s.client.Client
import org.http4s.{Header, Query, Uri}
import pt.tecnico.dsi.cinder.models.{Summary, Volume, VolumeSummary, WithId}
import fs2.Stream

final class Volumes[F[_]: Sync: Client](baseUri: Uri, authToken: Header)
  extends AsymmetricCrudService[F, Volume](baseUri, "volume", authToken) {

  override type Create = Volume.Create
  override type Update = Volume.Update

  /**
    * Lists summary information for all Block Storage volumes that the project can access.
    * @param query extra query params to pass in the request.
    */
  def listSummary(query: Query = Query.empty): Stream[F, WithId[VolumeSummary]] =
    super.list[WithId[VolumeSummary]](pluralName, uri, query)

  override def list(query: Query): Stream[F, WithId[Volume]] = super.list[WithId[Volume]](pluralName, uri / "detail", query)

  /**
    * Deletes a volume.
    *
    * @param id the id of the volume.
    * @param cascade remove any snapshots along with the volume.
    * @param force indicates whether to force delete a volume even if the volume is in deleting or error_deleting.
    */
  def delete(id: String, cascade: Boolean = false, force: Boolean = false): F[Unit] =
    super.delete(uri / id +?("cascade", cascade) +?("force", force))

  /** Display volumes summary with total number of volumes and total size. */
  val summary: F[Summary] = super.get(uri / "summary", wrappedAt = Some("volume-summary"))
}
