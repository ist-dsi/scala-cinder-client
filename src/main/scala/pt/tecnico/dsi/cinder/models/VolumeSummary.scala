package pt.tecnico.dsi.cinder.models

import io.circe.Decoder
import io.circe.derivation.deriveDecoder

object VolumeSummary {
  implicit val decoder: Decoder[VolumeSummary] = deriveDecoder
}
case class VolumeSummary(name: Option[String] = None, links: List[Links] = List.empty)
