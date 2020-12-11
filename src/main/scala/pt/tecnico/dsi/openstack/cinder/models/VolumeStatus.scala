package pt.tecnico.dsi.openstack.cinder.models

import cats.Show
import enumeratum.{Enum, EnumEntry}
import io.circe.derivation.renaming
import io.circe.{Decoder, Encoder}

sealed trait VolumeStatus extends EnumEntry
case object VolumeStatus extends Enum[VolumeStatus] { self =>
  // Openstack developers never heard about consistent naming
  private def transformName(name: String) = renaming.kebabCase(name).replace("error-", "error_")
  implicit val circeEncoder: Encoder[VolumeStatus] = Encoder.encodeString.contramap(status => transformName(status.entryName))
  implicit val circeDecoder: Decoder[VolumeStatus] = {
    lazy val kebabedNames = namesToValuesMap.map { case (key, value) => transformName(key) -> value }
    Decoder.decodeString.emap(s => kebabedNames.get(s).toRight(s"'$s' is not a member of enum $self"))
  }

  case object Available extends VolumeStatus
  case object Attaching extends VolumeStatus
  case object AwaitingTransfer extends VolumeStatus
  case object BackingUp extends VolumeStatus
  case object Creating extends VolumeStatus
  case object Deleting extends VolumeStatus
  case object Detaching extends VolumeStatus
  case object Downloading extends VolumeStatus
  case object InUse extends VolumeStatus
  case object Error extends VolumeStatus
  case object ErrorBackingUp extends VolumeStatus
  case object ErrorDeleting extends VolumeStatus
  case object ErrorExtending extends VolumeStatus
  case object ErrorRestoring extends VolumeStatus
  case object Extending extends VolumeStatus
  case object Maintenance extends VolumeStatus
  case object Reserved extends VolumeStatus
  case object RestoringBackup extends VolumeStatus
  case object Retyping extends VolumeStatus
  case object Uploading extends VolumeStatus
  
  val values = findValues
  
  implicit val show: Show[VolumeStatus] = Show.fromToString
}