package pt.tecnico.dsi.openstack.cinder.models

import io.circe.Decoder
import io.circe.derivation.deriveDecoder
import pt.tecnico.dsi.openstack.common.models.{Identifiable, Link}

object VolumeSummary {
  implicit val decoder: Decoder[VolumeSummary] = deriveDecoder
}
case class VolumeSummary(
  id: String,
  name: Option[String] = None,
  links: List[Link] = List.empty,
) extends Identifiable
