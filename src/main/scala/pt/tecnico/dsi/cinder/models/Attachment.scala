package pt.tecnico.dsi.cinder.models

import java.time.LocalDateTime
import io.circe.Codec
import io.circe.derivation.deriveCodec

object Attachment {
  implicit val codec: Codec.AsObject[Attachment] = deriveCodec
}
case class Attachment(
  serverId: String,
  attachmentId: String,
  attachedAt: LocalDateTime,
  hostName: String,
  volumeId: String,
  device: String,
  id: String
)