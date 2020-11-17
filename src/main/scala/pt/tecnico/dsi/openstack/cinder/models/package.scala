package pt.tecnico.dsi.openstack.cinder

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME
import cats.Show
import io.circe.{Decoder, Encoder}
import squants.information.Information
import squants.information.InformationConversions._

package object models {
  // Sizes/Quotas in Cinder are always in gibibytes.
  // When setting sizes/quotas they are always whole numbers.
  implicit val decoderInformation: Decoder[Information] = Decoder.decodeInt.map(_.gibibytes)
  implicit val encoderInformation: Encoder[Information] = Encoder.encodeInt.contramap(_.toGibibytes.ceil.toInt)

  implicit val showInformation: Show[Information] = Show.fromToString

  // Show we instead use https://github.com/ChristopherDavenport/cats-time?
  implicit val showOffsetDateTime: Show[LocalDateTime] = Show.show(_.format(ISO_LOCAL_DATE_TIME))
}
