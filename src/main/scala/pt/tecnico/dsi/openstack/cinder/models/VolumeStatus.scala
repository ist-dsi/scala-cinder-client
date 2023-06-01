package pt.tecnico.dsi.openstack.cinder.models

import cats.Show
import cats.derived.derived
import io.circe.derivation.renaming
import io.circe.{Decoder, Encoder}

object VolumeStatus:
  private def transformName(name: String) = renaming.kebabCase(name).replace("error-", "error_")

  given Encoder[VolumeStatus] = Encoder[String].contramap(status => transformName(status.toString))

  given Decoder[VolumeStatus] =
    lazy val kebabedNames = VolumeStatus.values.map(status => transformName(status.toString) -> status).toMap
    Decoder[String].emap(s => kebabedNames.get(s).toRight(s"'$s' is not a member of enum VolumeStatus"))

enum VolumeStatus derives Show {
  case Available
  case Attaching
  case AwaitingTransfer
  case BackingUp
  case Creating
  case Deleting
  case Detaching
  case Downloading
  case InUse
  case Error
  case ErrorBackingUp
  case ErrorDeleting
  case ErrorExtending
  case ErrorRestoring
  case Extending
  case Maintenance
  case Reserved
  case RestoringBackup
  case Retyping
  case Uploading
}