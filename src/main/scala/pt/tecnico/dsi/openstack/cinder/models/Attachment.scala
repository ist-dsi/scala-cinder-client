package pt.tecnico.dsi.openstack.cinder.models

import java.time.LocalDateTime
import cats.derived
import cats.derived.ShowPretty
import io.circe.Codec
import io.circe.derivation.deriveCodec

object Attachment {
  implicit val codec: Codec.AsObject[Attachment] = deriveCodec
  implicit val show: ShowPretty[Attachment] = derived.semiauto.showPretty
}
case class Attachment(
  id: String,
  serverId: String,
  attachmentId: String,
  attachedAt: LocalDateTime,
  hostName: String,
  volumeId: String,
  device: String,
)