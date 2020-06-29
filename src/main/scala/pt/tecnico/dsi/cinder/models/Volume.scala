package pt.tecnico.dsi.cinder.models

import java.time.LocalDateTime
import io.circe.{Decoder, DecodingFailure, Encoder, HCursor, JsonObject}
import io.circe.derivation.deriveEncoder
import squants.information.Information

object Volume {
  implicit val decoder: Decoder[Volume] = (cursor: HCursor) => for {
    size <- cursor.get[Information]("size")
    status <- cursor.get[VolumeStatus]("status")
    userId <- cursor.get[String]("user_id")
    projectId <- cursor.get[Option[String]]("os-vol-tenant-attr:tenant_id")
    name <- cursor.get[Option[String]]("name")
    description <- cursor.get[Option[String]]("description")
    tpe <- cursor.get[Option[String]]("volume_type")
    createdAt <- cursor.get[LocalDateTime]("created_at")
    updatedAt <- cursor.get[Option[LocalDateTime]]("updated_at")
    availabilityZone <- cursor.get[String]("availability_zone")
    encrypted <- cursor.get[Boolean]("encrypted")
    multiAttach <- cursor.get[Boolean]("multiattach")
    // Yes, in the Json it is a boolean inside a string :facepalm:
    bootable <- cursor.get[String]("bootable").map(_.toLowerCase).flatMap {
      case "true" => Right(true)
      case "false" => Right(false)
      case b => Left(DecodingFailure(s"Cannot decode $b to a Boolean", cursor.downField("bootable").history))
    }
    snapshotId <- cursor.get[Option[String]]("snapshot_id")
    sourceVolumeId <- cursor.get[Option[String]]("source_volid")
    consistencyGroupId <- cursor.get[Option[String]]("consistencygroup_id")
    host <- cursor.get[Option[String]]("os-vol-host-attr:host")
    backendVolumeId <- cursor.get[Option[String]]("os-vol-mig-status-attr:name_id")
    replicationStatus <- cursor.get[Option[VolumeStatus]]("replication_status")
    // There are two fields for this: "migration_status" and "os-vol-mig-status-attr:migstat"
    // "migration_status" is only for admins, "os-vol-mig-status-attr:migstat" seems to always be present and have the
    // same value as the "migration_status"
    migrationStatus <- cursor.get[Option[VolumeStatus]]("os-vol-mig-status-attr:migstat")
    attachments <- cursor.get[List[Attachment]]("attachments")
    metadata <- cursor.get[JsonObject]("metadata")
    volumeImageMetadata <- cursor.get[Option[Map[String, String]]]("volume_image_metadata")
  } yield new Volume(size, status, userId, projectId, name, description, tpe, createdAt, updatedAt, availabilityZone,
    encrypted, multiAttach, bootable, snapshotId, sourceVolumeId, consistencyGroupId, host, backendVolumeId,
    replicationStatus, migrationStatus, attachments, metadata, volumeImageMetadata)

  object Create {
    implicit val encoder: Encoder[Create] = Encoder.forProduct12(
      "size", "availability_zone", "name", "description", "multiattach", "source_volid", "snapshot_id", "backup_id",
      "imageRef", "volume_type", "metadata", "consistencygroup_id")(unapply(_).get)
  }

  /**
    * @param size the size of the volume, in gibibytes (GiB).
    * @param availabilityZone the availability zone where the volume will be created.
    * @param name the volume name.
    * @param description the volume description.
    * @param multiAttach to enable this volume to be attached to more than one server set this value to true.
    * @param sourceVolumeId the UUID of the source volume. The API creates a new volume with the same size as the
    *                       source volume unless a larger size is requested.
    * @param snapshotId to create a volume from an existing snapshot, specify the UUID of the volume snapshot.
    *                   The volume is created in same availability zone and with same size as the snapshot.
    * @param backupId the UUID of the backup.
    * @param imageId the UUID of the image from which you want to create the volume. Required to create a bootable volume.
    * @param `type` The volume type (either name or ID). To create an environment with multiple-storage back ends,
    *               you must specify a volume type. Block Storage volume back ends are spawned as children to
    *               `cinder-volume`, and they are keyed from a unique queue. They are named `cinder-volume.HOST.BACKEND`.
    *               For example, `cinder-volume.ubuntu.lvmdriver`. When a volume is created, the scheduler chooses an
    *               appropriate back end to handle the request based on the volume type.
    * @param metadata metadata that will be associated with the volume.
    * @param consistencyGroupId the UUID of the consistency group.
    */
  case class Create(
    size: Information,
    availabilityZone: Option[String] = None,
    name: Option[String] = None,
    description: Option[String] = None,
    multiAttach: Boolean = false,
    sourceVolumeId: Option[String] = None,
    snapshotId: Option[String] = None,
    backupId: Option[String] = None,
    imageId: Option[String] = None,
    `type`: Option[String] = None,
    metadata: Map[String, String] = Map.empty,
    consistencyGroupId: Option[String] = None,
  )

  object Update {
    implicit val encoder: Encoder[Update] = deriveEncoder
  }

  /**
    * @param name the volume name.
    * @param description the volume description.
    * @param metadata metadata that will be associated with the volume.
    */
  case class Update(
    name: Option[String] = None,
    description: Option[String] = None,
    metadata: Map[String, String] = Map.empty,
  )
}

/**
  * @param name                the volume name.
  * @param `type`              the associated volume type for the volume.
  * @param status              the volume status.
  * @param size                the size of the volume, in gibibytes (GiB).
  * @param userId              the UUID of the user.
  * @param projectId           the project ID which the volume belongs to.
  * @param description         the volume description.
  * @param createdAt           the date and time when the resource was created.
  * @param updatedAt           the date and time when the resource was updated.
  * @param availabilityZone    the name of the availability zone.
  * @param encrypted           whether this volume is encrypted.
  * @param multiAttach         whether this volume can attach to more than one instance.
  * @param bootable            whether this volume has the bootable attribute set.
  * @param snapshotId          the snapshot id from which this volume was created.
  * @param sourceVolumeId      the volume id from which this volume was created.
  * @param consistencyGroupId  the consistency group id this volume belongs to.
  * @param host                current back-end of the volume. Host format is host@backend#pool.
  * @param backendVolumeId     the volume ID that this volume name on the back-end is based on.
  * @param replicationStatus   the volume replication status.
  * @param migrationStatus     the status of this volume migration.
  * @param attachments         instance attachment information.
  * @param metadata            metadata that is associated with the volume.
  * @param volumeImageMetadata list of image metadata entries. Only included for volumes that were created from an
  *                            image, or from a snapshot of a volume originally created from an image.
  */
case class Volume(
  size: Information,
  status: VolumeStatus,
  userId: String,
  projectId: Option[String] = None,
  name: Option[String] = None,
  description: Option[String] = None,
  `type`: Option[String] = None,
  createdAt: LocalDateTime,
  updatedAt: Option[LocalDateTime] = None,
  availabilityZone: String,
  encrypted: Boolean,
  multiAttach: Boolean = false,
  bootable: Boolean,
  snapshotId: Option[String] = None,
  sourceVolumeId: Option[String] = None,
  consistencyGroupId: Option[String] = None,
  host: Option[String] = None,
  backendVolumeId: Option[String] = None,
  replicationStatus: Option[VolumeStatus] = None,
  migrationStatus: Option[VolumeStatus] = None,
  attachments: List[Attachment] = List.empty,
  metadata: JsonObject = JsonObject.empty,
  volumeImageMetadata: Option[Map[String, String]] = None,
)