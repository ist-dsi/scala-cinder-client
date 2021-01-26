package pt.tecnico.dsi.openstack.cinder.services

import cats.effect.Concurrent
import io.circe.Encoder
import org.http4s.Uri
import org.http4s.client.Client
import pt.tecnico.dsi.openstack.cinder.models.{Quota, QuotaUsage}
import pt.tecnico.dsi.openstack.common.services.Service
import pt.tecnico.dsi.openstack.keystone.models.Session

final class Quotas[F[_]: Concurrent: Client](baseUri: Uri, session: Session) extends Service[F](baseUri, "quota_set", session.authToken) {
  override val uri: Uri = baseUri / "os-quota-sets"
  
  private val wrappedAt: Option[String] = Some(name)
  
  /**
    * Shows quotas for a project.
    * Cinder always returns a Quota even if the project does not exist. That is why there is no method called `get`.
    * @param projectId The UUID of the project.
    */
  def apply(projectId: String): F[Quota] = super.get(wrappedAt, uri / projectId)
  
  /**
    * Shows quota usage for a project.
    * Cinder always returns a Quota even if the project does not exist. That is why there is no method called `getUsage`.
    * @param projectId The UUID of the project.
    */
  def applyUsage(projectId: String): F[QuotaUsage] = super.get(wrappedAt, (uri / projectId).withQueryParam("usage", true))
  
  /**
    * Gets default quotas for a project.
    * Cinder always returns a Quota even if the project does not exist. That is why there is no method called `getDefaults`.
    * @param projectId The UUID of the project.
    */
  def applyDefaults(projectId: String): F[Quota] = super.get(wrappedAt, uri / projectId / "defaults")
  
  /**
    * Updates quotas for a project.
    * @param projectId The UUID of the project.
    */
  def update(projectId: String, quotas: Quota.Update)(implicit encoder: Encoder[Quota.Update]): F[Quota] =
    super.put(wrappedAt, quotas, uri / projectId)
  
  /**
    * Deletes quotas for a project so the quotas revert to default values.
    * @param projectId The UUID of the project.
    */
  def delete(projectId: String): F[Unit] = super.delete(uri / projectId)
}