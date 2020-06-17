package pt.tecnico.dsi.cinder.models

import cats.instances.either._
import cats.instances.list._
import cats.syntax.traverse._
import io.circe.Decoder.Result
import io.circe.syntax._
import io.circe.{Decoder, Encoder, HCursor, DecodingFailure, Codec, JsonObject}
import squants.information.Information
import squants.information.InformationConversions._

object Quota {
  implicit val encoder: Encoder.AsObject[Quota] = (quota: Quota) => {
    val base = Map(
      "volume" -> quota.volumes.asJson,
      "snapshots" -> quota.snapshots.asJson,
      "backups" -> quota.backups.asJson,
      "groups" -> quota.groups.asJson,
      "per_volume_gigabytes" -> quota.maxVolumeSize.toGigabytes.ceil.toInt.asJson,
      "gigabytes" -> quota.volumesStorage.toGigabytes.ceil.toInt.asJson,
      "backup_gigabytes" -> quota.backupsStorage.toGigabytes.ceil.toInt.asJson,
    )
    val volumesPerType = quota.volumesPerType.map { case (tpe, value) => s"volumes_$tpe" -> value.asJson }
    val snapshotsPerType = quota.snapshotsPerType.map { case (tpe, value) => s"snapshots_$tpe" -> value.asJson }
    val gigabytesPerType = quota.volumesStoragePerType.map { case (tpe, value) => s"gigabytes_$tpe" -> value.toGigabytes.ceil.toInt.asJson }
    JsonObject.fromMap(base ++ volumesPerType ++ snapshotsPerType ++ gigabytesPerType)
  }
  implicit val decoder: Decoder[Quota] = new Decoder[Quota] {
    def extractPerType(cursor: HCursor, allKeys: List[String], prefix: String): Either[DecodingFailure, Map[String, Int]] =
      allKeys.filter(_.startsWith(prefix)).traverse { key =>
        cursor.get[Int](key).map(key.stripPrefix(prefix) -> _)
      }.map(_.toMap)

    override def apply(cursor: HCursor): Result[Quota] = {
      val allKeys = cursor.keys.map(_.toList).getOrElse(List.empty)
      for {
        volumes <- cursor.get[Int]("volumes")
        volumesPerType <- extractPerType(cursor, allKeys, "volumes_")
        snapshots <- cursor.get[Int]("snapshots")
        snapshotsPerType <- extractPerType(cursor, allKeys, "snapshots_")
        backups <- cursor.get[Int]("backups")
        groups <- cursor.get[Int]("groups")
        maxVolumeSize <- cursor.get[Int]("perVolume").map(_.gigabytes)
        backupsStorage <- cursor.get[Int]("backup").map(_.gigabytes)
        volumesStorage <- cursor.get[Int]("gigabytes").map(_.gigabytes)
        volumesStoragePerTypeInt <- extractPerType(cursor, allKeys, "gigabytes_")
        volumesStoragePerType = volumesStoragePerTypeInt.view.mapValues(_.gigabytes).toMap
      } yield Quota(volumes, volumesPerType, snapshots, snapshotsPerType, backups, groups, maxVolumeSize, backupsStorage, volumesStorage, volumesStoragePerType)
    }
  }

  implicit val codec: Codec.AsObject[Quota] = Codec.AsObject.from(decoder, encoder)
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
case class Quota(
  volumes: Int,
  volumesPerType: Map[String, Int],
  snapshots: Int,
  snapshotsPerType: Map[String, Int],
  backups: Int,
  groups: Int,
  maxVolumeSize: Information,
  backupsStorage: Information,
  volumesStorage: Information,
  volumesStoragePerType: Map[String, Information]
)