package pt.tecnico.dsi.openstack.cinder.services

import cats.effect.Sync
import fs2.Stream
import io.circe.{Decoder, Encoder}
import org.http4s.client.Client
import org.http4s.{Header, Query, Uri}
import pt.tecnico.dsi.openstack.cinder.models.{Volume, VolumeSummary}
import pt.tecnico.dsi.openstack.common.services.{BaseCrudService, CreateNonIdempotentOperations, DeleteOperations, ListOperations, ReadOperations, UpdateOperations}
import pt.tecnico.dsi.openstack.keystone.models.Session

final class Volumes[F[_]: Sync: Client](baseUri: Uri, session: Session)
  extends BaseCrudService[F](baseUri, "volume", session.authToken)
    with CreateNonIdempotentOperations[F, Volume, Volume.Create]
    with UpdateOperations[F, Volume, Volume.Update]
    with ListOperations[F, Volume]
    with ReadOperations[F, Volume]
    with DeleteOperations[F, Volume] {
  override implicit val modelDecoder: Decoder[Volume] = Volume.decoder
  override implicit val createEncoder: Encoder[Volume.Create] = Volume.Create.encoder
  override implicit val updateEncoder: Encoder[Volume.Update] = Volume.Update.encoder
  
  /**
    * Creates a new volume.
    * @param volume the volume create options.
    */
  override def create(volume: Volume.Create, extraHeaders: Header*): F[Volume] = super.create(volume, extraHeaders:_*)
  
  /**
    * Lists summary information for all Block Storage volumes that the project can access.
    *
    * @param query extra query params to pass in the request.
    */
  def listSummary(query: Query = Query.empty): F[List[VolumeSummary]] = super.list[VolumeSummary](pluralName, uri.copy(query = query))
  
  /**
    * Streams summary information for all Block Storage volumes that the project can access.
    *
    * @param query extra query params to pass in the request.
    */
  def streamSummary(query: Query = Query.empty): Stream[F, VolumeSummary] = super.stream[VolumeSummary](pluralName, uri.copy(query = query))
  
  override def list(query: Query, extraHeaders: Header*): F[List[Volume]] =
    super.list[Volume](pluralName, (uri / "detail").copy(query = query), extraHeaders:_*)
  
  override def stream(query: Query, extraHeaders: Header*): fs2.Stream[F, Volume] =
    super.stream[Volume](pluralName, (uri / "detail").copy(query = query), extraHeaders:_*)
  
  /**
    * Deletes a volume.
    *
    * @param id the id of the volume.
    * @param cascade remove any snapshots along with the volume.
    * @param force indicates whether to force delete a volume even if the volume is in deleting or error_deleting.
    */
  def delete(id: String, cascade: Boolean = false, force: Boolean = false): F[Unit] =
    super.delete((uri / id).withQueryParam("cascade", cascade).withQueryParam("force", force))
}
