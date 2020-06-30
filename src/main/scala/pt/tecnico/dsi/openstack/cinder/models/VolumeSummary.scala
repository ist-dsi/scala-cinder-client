package pt.tecnico.dsi.openstack.cinder.models

import io.circe.Decoder
import io.circe.derivation.deriveDecoder

object VolumeSummary {
  implicit val decoder: Decoder[VolumeSummary] = deriveDecoder
}
case class VolumeSummary(name: Option[String] = None)
