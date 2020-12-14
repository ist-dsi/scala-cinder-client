package pt.tecnico.dsi.openstack.cinder.models

import java.time.LocalDateTime
import cats.derived
import cats.derived.ShowPretty
import cats.effect.Sync
import io.circe.derivation.{deriveDecoder, deriveEncoder, renaming}
import io.circe.{Decoder, Encoder, JsonObject}
import pt.tecnico.dsi.openstack.common.models.{Identifiable, Link}
import pt.tecnico.dsi.openstack.keystone.KeystoneClient
import pt.tecnico.dsi.openstack.keystone.models.Project
import squants.information.Information

object Snapshot {
  object Create {
    implicit val encoder: Encoder[Create] = deriveEncoder(renaming.snakeCase)
    implicit val show: ShowPretty[Create] = derived.semiauto.showPretty
  }
  case class Create(
    name: String,
    volumeId: String,
    description: Option[String] = None,
    force: Boolean = false,
    metadata: Map[String, String] = Map.empty,
  )

  object Update {
    implicit val encoder: Encoder[Update] = deriveEncoder(renaming.snakeCase)
    implicit val show: ShowPretty[Update] = derived.semiauto.showPretty
  }
  case class Update(
    name: Option[String] = None,
    description: Option[String] = None,
  )

  implicit val decoder: Decoder[Snapshot] = deriveDecoder[Snapshot](Map(
    "projectId" -> "os-extended-snapshot-attributes:project_id",
    "progress" -> "os-extended-snapshot-attributes:progress",
  ).withDefault(renaming.snakeCase))
  implicit val show: ShowPretty[Snapshot] = derived.semiauto.showPretty
}
case class Snapshot(
  id: String,
  name: String,
  description: Option[String] = None,
  projectId: String,

  size: Information,
  status: SnapshotStatus,
  volumeId: String,
  progress: String, // This is a percentage "12%" we should be able to get a better type for this
  metadata: JsonObject = JsonObject.empty,

  createdAt: LocalDateTime,
  updatedAt: Option[LocalDateTime] = None,
  links: List[Link] = List.empty,
) extends Identifiable {
  def project[F[_]: Sync](implicit keystone: KeystoneClient[F]): F[Project] = keystone.projects(id)
}