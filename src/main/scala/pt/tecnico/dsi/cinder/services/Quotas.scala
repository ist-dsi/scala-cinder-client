package pt.tecnico.dsi.cinder.services

import cats.effect.Sync
import org.http4s.client.Client
import org.http4s.{Header, Uri}
import org.http4s.Method.GET
import pt.tecnico.dsi.cinder.models.{Quota, WithId}

final class Quotas[F[_]: Sync: Client](baseUri: Uri, authToken: Header) extends BaseService[F](authToken) {
  import dsl._

  override val uri: Uri = baseUri / "os-quota-sets"
  private val name = "quota-set"

  /**
    * Shows quotas for a project.
    * @param projectId The UUID of the project.
    */
  def get(projectId: String): F[WithId[Quota]] = super.get(name, projectId)

  def getDefaults(projectId: String): F[WithId[Quota]] = unwrap(GET(uri / projectId / "defaults", authToken), name)

  /**
    * Updates quotas for a project.
    * @param projectId The UUID of the project.
    */
  def update(projectId: String, quotas: Quota): F[WithId[Quota]] = super.update(name, projectId, quotas)

  /**
    * Deletes quotas for a project so the quotas revert to default values.
    * @param projectId
    */
  override def delete(projectId: String): F[Unit] = super.delete(projectId)
}