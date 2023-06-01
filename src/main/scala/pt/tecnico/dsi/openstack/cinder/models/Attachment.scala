package pt.tecnico.dsi.openstack.cinder.models

import java.time.LocalDateTime
import cats.derived.derived
import cats.derived.ShowPretty
import io.circe.derivation.ConfiguredCodec

case class Attachment(
  id: String,
  serverId: String,
  attachmentId: String,
  attachedAt: LocalDateTime,
  hostName: String,
  volumeId: String,
  device: String,
) derives ConfiguredCodec, ShowPretty