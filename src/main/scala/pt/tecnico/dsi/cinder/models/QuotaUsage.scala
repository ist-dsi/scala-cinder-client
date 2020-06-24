package pt.tecnico.dsi.cinder.models

import io.circe.Decoder
import squants.information.Information

object QuotaUsage {
  implicit val decoder: Decoder[QuotaUsage] = Quota.decoder[Usage, QuotaUsage](QuotaUsage.apply)
}
/**
  * A value of -1 means no limit.
  * @param volumes number of volumes that are allowed for each project.
  * @param volumesPerType number of volumes that are allowed for each project and the specified volume type.
  * @param snapshots number of snapshots that are allowed for each project.
  * @param snapshotsPerType number of snapshots per volume type that are allowed for each project.
  * @param backups number of backups that are allowed for each project.
  * @param groups number of groups that are allowed for each project.
  * @param maxVolumeSize max size of a volume.
  * @param backupsStorage size of backups that are allowed for each project.
  * @param volumesStorage size of volumes and snapshots that are allowed for each project.
  * @param volumesStoragePerType size of volumes and snapshots per volume type that are allowed for each project.
  */
case class QuotaUsage(
  volumes: Usage[Int],
  volumesPerType: Map[String, Usage[Int]],
  snapshots: Usage[Int],
  snapshotsPerType: Map[String, Usage[Int]],
  backups: Usage[Int],
  groups: Usage[Int],
  maxVolumeSize: Usage[Information],
  backupsStorage: Usage[Information],
  volumesStorage: Usage[Information],
  volumesStoragePerType: Map[String, Usage[Information]]
)