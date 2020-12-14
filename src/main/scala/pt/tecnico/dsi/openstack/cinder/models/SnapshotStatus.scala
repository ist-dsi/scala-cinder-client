package pt.tecnico.dsi.openstack.cinder.models

import cats.Show
import enumeratum.{Enum, EnumEntry}
import io.circe.{Decoder, Encoder}
import io.circe.derivation.renaming

sealed trait SnapshotStatus extends EnumEntry
case object SnapshotStatus extends Enum[SnapshotStatus] { self =>
  // Openstack developers never heard about consistent naming
  private def transformName(name: String) = renaming.kebabCase(name).replace("error-", "error_")
  implicit val circeEncoder: Encoder[SnapshotStatus] = Encoder.encodeString.contramap(status => transformName(status.entryName))
  implicit val circeDecoder: Decoder[SnapshotStatus] = {
    lazy val kebabedNames = namesToValuesMap.map { case (key, value) => transformName(key) -> value }
    Decoder.decodeString.emap(s => kebabedNames.get(s).toRight(s"'$s' is not a member of enum $self"))
  }

  case object Available extends SnapshotStatus
  case object BackingUp extends SnapshotStatus
  case object Creating extends SnapshotStatus
  case object Deleting extends SnapshotStatus
  case object Error extends SnapshotStatus
  case object ErrorDeleting extends SnapshotStatus
  case object Restoring extends SnapshotStatus
  case object Unmanaging extends SnapshotStatus

  val values = findValues

  implicit val show: Show[SnapshotStatus] = Show.fromToString
}