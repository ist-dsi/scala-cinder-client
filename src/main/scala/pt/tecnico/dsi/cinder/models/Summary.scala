package pt.tecnico.dsi.cinder.models

import io.circe.{Decoder, JsonObject}
import io.circe.derivation.{deriveDecoder, renaming}
import squants.information.Information

object Summary {
  implicit val decoder: Decoder[Summary] = deriveDecoder(renaming.snakeCase)
}
case class Summary(totalSize: Information, totalCount: Int, metadata: JsonObject)
