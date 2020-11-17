package pt.tecnico.dsi.openstack.cinder.models

import cats.derived
import cats.derived.ShowPretty
import io.circe.Decoder
import io.circe.derivation.deriveDecoder
import pt.tecnico.dsi.openstack.common.models.{Identifiable, Link}

object VolumeSummary {
  implicit val decoder: Decoder[VolumeSummary] = deriveDecoder
  implicit val show: ShowPretty[VolumeSummary] = derived.semiauto.showPretty
}
case class VolumeSummary(
  id: String,
  name: Option[String] = None,
  links: List[Link] = List.empty,
) extends Identifiable
