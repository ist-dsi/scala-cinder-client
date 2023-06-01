package pt.tecnico.dsi.openstack.cinder.models

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME
import cats.Show
import io.circe.derivation.Configuration
import io.circe.{Decoder, Encoder}
import squants.information.Information
import squants.information.InformationConversions.*

given Configuration = Configuration.default.withDefaults.withSnakeCaseMemberNames

// Sizes/Quotas in Cinder are always in gibibytes.
// When setting sizes/quotas they are always whole numbers.
given Decoder[Information] = Decoder.decodeInt.map(_.gibibytes)
given Encoder[Information] = Encoder.encodeInt.contramap(_.toGibibytes.ceil.toInt)

given Show[Information] = Show.fromToString

// Show we instead use https://github.com/ChristopherDavenport/cats-time?
given Show[LocalDateTime] = Show.show(_.format(ISO_LOCAL_DATE_TIME))
