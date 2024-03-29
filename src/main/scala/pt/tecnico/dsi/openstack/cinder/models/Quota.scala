package pt.tecnico.dsi.openstack.cinder.models

import cats.instances.either.*
import cats.instances.list.*
import cats.syntax.traverse.*
import cats.Id
import cats.derived.derived
import cats.derived.ShowPretty
import io.circe.syntax.*
import io.circe.*
import io.circe.derivation.ConfiguredEncoder
import squants.information.Information
import squants.information.InformationConversions.*

object Quota:
  object Update:
    given Encoder.AsObject[Update] = (quota: Update) => {
      val base = Map(
        "volumes" -> quota.volumes.asJson,
        "snapshots" -> quota.snapshots.asJson,
        "backups" -> quota.backups.asJson,
        "groups" -> quota.groups.asJson,
        "per_volume_gigabytes" -> quota.maxVolumeSize.asJson,
        "gigabytes" -> quota.volumesStorage.asJson,
        "backup_gigabytes" -> quota.backupsStorage.asJson,
      )
      val volumesPerType = quota.volumesPerType.map { case (tpe, value) => s"volumes_$tpe" -> value.asJson }
      val snapshotsPerType = quota.snapshotsPerType.map { case (tpe, value) => s"snapshots_$tpe" -> value.asJson }
      val gigabytesPerType = quota.volumesStoragePerType.map { case (tpe, value) => s"gigabytes_$tpe" -> value.asJson }
      JsonObject.fromMap(base ++ volumesPerType ++ snapshotsPerType ++ gigabytesPerType)
    }
    val zero: Update = Update(
      volumes = Some(0),
      volumesPerType = Map.empty,
      snapshots = Some(0),
      snapshotsPerType = Map.empty,
      backups = Some(0),
      groups = Some(0),
      maxVolumeSize = Some(0.bytes),
      backupsStorage = Some(0.bytes),
      volumesStorage = Some(0.bytes),
      volumesStoragePerType = Map.empty,
    )
  final case class Update(
    volumes: Option[Int] = None,
    volumesPerType: Map[String, Int] = Map.empty,
    snapshots: Option[Int] = None,
    snapshotsPerType: Map[String, Int] = Map.empty,
    backups: Option[Int] = None,
    groups: Option[Int] = None,
    maxVolumeSize: Option[Information] = None,
    backupsStorage: Option[Information] = None,
    volumesStorage: Option[Information] = None,
    volumesStoragePerType: Map[String, Information] = Map.empty,
  ) derives ShowPretty:
    lazy val needsUpdate: Boolean =
      val aOptionIsDefined = List(volumes, snapshots, backups, groups, maxVolumeSize, backupsStorage, volumesStorage).exists(_.isDefined)
      val aMapIsNonEmpty = List(volumesPerType, snapshotsPerType, volumesStoragePerType).exists(_.nonEmpty)
      aOptionIsDefined || aMapIsNonEmpty
  
  val zero: Quota = Quota(0, Map.empty, 0, Map.empty, 0, 0, 0.gibibytes, 0.gibibytes, 0.gibibytes, Map.empty)
  
  // Its better to have this slightly uglier than to repeat it for the QuotaUsage.
  private[models] def decoder[F[_], T](f: (F[Int], Map[String, F[Int]], F[Int], Map[String, F[Int]], F[Int], F[Int], F[Information], F[Information], F[Information], Map[String, F[Information]]) => T)
    (using dFInt: Decoder[F[Int]], dFInformation: Decoder[F[Information]]): Decoder[T] = (cursor: HCursor) => {
    val allKeys = cursor.keys.map(_.toList).getOrElse(List.empty)
    
    def extractPerType[R: Decoder](prefix: String): Either[DecodingFailure, Map[String, R]] =
      allKeys.filter(_.startsWith(prefix)).traverse { key =>
        cursor.get[R](key).map(key.stripPrefix(prefix) -> _)
      }.map(_.toMap)
    
    for
      volumes <- cursor.get[F[Int]]("volumes")
      volumesPerType <- extractPerType[F[Int]]("volumes_")
      snapshots <- cursor.get[F[Int]]("snapshots")
      snapshotsPerType <- extractPerType[F[Int]]("snapshots_")
      backups <- cursor.get[F[Int]]("backups")
      groups <- cursor.get[F[Int]]("groups")
      maxVolumeSize <- cursor.get[F[Information]]("per_volume_gigabytes")
      backupsStorage <- cursor.get[F[Information]]("backup_gigabytes")
      volumesStorage <- cursor.get[F[Information]]("gigabytes")
      volumesStoragePerType <- extractPerType[F[Information]]("gigabytes_")
    yield f(volumes, volumesPerType, snapshots, snapshotsPerType, backups, groups, maxVolumeSize, backupsStorage, volumesStorage, volumesStoragePerType)
  }
  given Decoder[Quota] = decoder[Id, Quota](Quota.apply)

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
final case class Quota(
  volumes: Int,
  volumesPerType: Map[String, Int] = Map.empty,
  snapshots: Int,
  snapshotsPerType: Map[String, Int] = Map.empty,
  backups: Int,
  groups: Int,
  maxVolumeSize: Information,
  backupsStorage: Information,
  volumesStorage: Information,
  volumesStoragePerType: Map[String, Information] = Map.empty,
) derives ConfiguredEncoder, ShowPretty