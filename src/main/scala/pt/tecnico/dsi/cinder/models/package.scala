package pt.tecnico.dsi.cinder

import io.circe.{Decoder, Encoder}
import squants.information.Information
import squants.information.InformationConversions._

package object models {
  // Sizes/Quotas in Cinder are always in gibibytes.
  // When setting sizes/quotas they are always whole numbers.
  implicit val decoderInformation: Decoder[Information] = Decoder.decodeDouble.map(_.gibibytes)
  implicit val encoderInformation: Encoder[Information] = Encoder.encodeInt.contramap(_.toGibibytes.ceil.toInt)
}
