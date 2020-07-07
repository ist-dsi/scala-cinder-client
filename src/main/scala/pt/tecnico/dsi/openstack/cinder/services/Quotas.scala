package pt.tecnico.dsi.openstack.cinder.services

import cats.effect.Sync
import io.circe.Encoder
import org.http4s.client.Client
import org.http4s.{Header, Uri}
import pt.tecnico.dsi.openstack.cinder.models.{Quota, QuotaUsage}
import pt.tecnico.dsi.openstack.common.models.WithId
import pt.tecnico.dsi.openstack.common.services.Service

final class Quotas[F[_]: Sync: Client](baseUri: Uri, authToken: Header) extends Service[F](authToken) {
  val uri: Uri = baseUri / "os-quota-sets"
  val name = "quota_set"

  /**
    * Shows quotas for a project.
    * @param projectId The UUID of the project.
    */
  def get(projectId: String): F[WithId[Quota]] = super.get(wrappedAt = Some(name), uri / projectId)

  /**
    * Shows quota usage for a project.
    * @param projectId The UUID of the project.
    */
  def getUsage(projectId: String): F[WithId[QuotaUsage]] = super.get(wrappedAt = Some(name), (uri / projectId).+?("usage", true))

  /**
    * Gets default quotas for a project.
    * @param projectId The UUID of the project.
    */
  def getDefaults(projectId: String): F[WithId[Quota]] = super.get(wrappedAt = Some(name), uri / projectId / "defaults")

  /**
    * Updates quotas for a project.
    * @param projectId The UUID of the project.
    */
  def update(projectId: String, quotas: Quota.Update)(implicit encoder: Encoder[Quota.Update]): F[Quota] =
    super.put(wrappedAt = Some(name), quotas, uri / projectId)

  /**
    * Deletes quotas for a project so the quotas revert to default values.
    * @param projectId The UUID of the project.
    */
  def delete(projectId: String): F[Unit] = super.delete(uri / projectId)
}