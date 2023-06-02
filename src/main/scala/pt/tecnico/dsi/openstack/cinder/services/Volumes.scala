package pt.tecnico.dsi.openstack.cinder.services

import cats.effect.Concurrent
import fs2.Stream
import io.circe.{Decoder, Encoder}
import org.http4s.client.Client
import org.http4s.{Header, Query, Uri}
import pt.tecnico.dsi.openstack.cinder.models.Volume
import pt.tecnico.dsi.openstack.common.services.*
import pt.tecnico.dsi.openstack.keystone.models.Session

final class Volumes[F[_]: Concurrent: Client](baseUri: Uri, session: Session)
  extends PartialCrudService[F](baseUri, "volume", session.authToken)
    with CreateNonIdempotentOperations[F, Volume, Volume.Create]
    with UpdateOperations[F, Volume, Volume.Update]
    with ListOperations[F, Volume]
    with ReadOperations[F, Volume]
    with DeleteOperations[F, Volume]:
  override given modelDecoder: Decoder[Volume] = Volume.given_Decoder_Volume
  override given createEncoder: Encoder[Volume.Create] = Volume.Create.derived$ConfiguredEncoder
  override given updateEncoder: Encoder[Volume.Update] = Volume.Update.derived$ConfiguredEncoder
  
  /**
    * Creates a new volume.
    * @param volume the volume create options.
    */
  override def create(volume: Volume.Create, extraHeaders: Header.ToRaw*): F[Volume] = super.create(volume, extraHeaders*)
  
  /**
    * Lists summary information for all Block Storage volumes that the project can access.
    *
    * @param query extra query params to pass in the request.
    */
  def listSummary(query: Query = Query.empty): F[List[Volume.Summary]] = super.list(pluralName, uri.copy(query = query))
  
  /**
    * Streams summary information for all Block Storage volumes that the project can access.
    *
    * @param query extra query params to pass in the request.
    */
  def streamSummary(query: Query = Query.empty): Stream[F, Volume.Summary] = super.stream(pluralName, uri.copy(query = query))
  
  override def list(query: Query, extraHeaders: Header.ToRaw*): F[List[Volume]] =
    super.list(pluralName, (uri / "detail").copy(query = query), extraHeaders*)
  
  override def stream(query: Query, extraHeaders: Header.ToRaw*): fs2.Stream[F, Volume] =
    super.stream(pluralName, (uri / "detail").copy(query = query), extraHeaders*)
  
  /**
    * Deletes a volume.
    *
    * @param id the id of the volume.
    * @param cascade remove any snapshots along with the volume.
    * @param force indicates whether to force delete a volume even if the volume is in deleting or error_deleting.
    */
  def delete(id: String, cascade: Boolean = false, force: Boolean = false): F[Unit] =
    super.delete((uri / id).withQueryParam("cascade", cascade).withQueryParam("force", force))
