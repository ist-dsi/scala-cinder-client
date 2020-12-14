package pt.tecnico.dsi.openstack.cinder.services

import cats.effect.Sync
import io.circe.{Decoder, Encoder, Json}
import org.http4s.client.Client
import org.http4s.{Header, Query, Uri}
import pt.tecnico.dsi.openstack.cinder.models.Snapshot
import pt.tecnico.dsi.openstack.common.services._
import pt.tecnico.dsi.openstack.keystone.models.Session

final class Snapshots[F[_]: Sync: Client](baseUri: Uri, session: Session)
  extends PartialCrudService[F](baseUri, "snapshot", session.authToken)
    with CreateNonIdempotentOperations[F, Snapshot, Snapshot.Create]
    with UpdateOperations[F, Snapshot, Snapshot.Update]
    with ListOperations[F, Snapshot]
    with ReadOperations[F, Snapshot]
    with DeleteOperations[F, Snapshot] {
  override implicit val modelDecoder: Decoder[Snapshot] = Snapshot.decoder
  override implicit val createEncoder: Encoder[Snapshot.Create] = Snapshot.Create.encoder
  override implicit val updateEncoder: Encoder[Snapshot.Update] = Snapshot.Update.encoder

  /**
    * Creates a new snapshot.
    *
    * @note this method is not idempotent! The Openstack API creates a new Snapshot every time.
    * @param snapshot the snapshot create options.
    */
  override def create(snapshot: Snapshot.Create, extraHeaders: Header*): F[Snapshot] = super.create(snapshot, extraHeaders:_*)

  override def list(query: Query, extraHeaders: Header*): F[List[Snapshot]] =
    super.list[Snapshot](pluralName, (uri / "detail").copy(query = query), extraHeaders:_*)

  override def stream(query: Query, extraHeaders: Header*): fs2.Stream[F, Snapshot] =
    super.stream[Snapshot](pluralName, (uri / "detail").copy(query = query), extraHeaders:_*)

  /**
    * Deletes a snapshot.
    *
    * @param id the id of the snapshot.
    * @param force indicates whether to force delete a snapshot, regardless of state.
    */
  def delete(id: String, force: Boolean = false): F[Unit] = force match {
    case false => super.delete(uri / id)
    case true => super.post(wrappedAt = None, Json.obj("os-force_delete" -> Json.obj()), uri / id / "action")
  }
}
