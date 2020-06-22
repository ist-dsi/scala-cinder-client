package pt.tecnico.dsi.cinder.models

import io.circe.Codec
import io.circe.derivation.deriveCodec
import org.http4s.Uri
import org.http4s.circe.{decodeUri, encodeUri}

object Links {
  implicit val codec: Codec.AsObject[Links] = deriveCodec
}
case class Links(href: Uri, rel: String)