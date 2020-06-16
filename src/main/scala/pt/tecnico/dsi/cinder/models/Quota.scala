package pt.tecnico.dsi.cinder.models

import cats.effect.Sync
import io.circe.Codec
import io.circe.derivation.{deriveCodec, renaming}
import pt.tecnico.dsi.cinder.CinderClient

object Quota {
  implicit val codec: Codec.AsObject[Quota] = deriveCodec(renaming.snakeCase, false, None)

  def apply(name: String, description: String, domainId: String,
            isDomain: Boolean = false, enabled: Boolean = true): Quota =
    Quota(name, description, domainId, isDomain, enabled, List.empty)

  implicit class WithIdQuotaExtensions[H[_]](quota: WithId[Quota])(implicit client: CinderClient[H], H: Sync[H]) {

  }
}

case class Quota private[cinder](
  name: String,
  description: String,
  domainId: String,
  isDomain: Boolean,
  enabled: Boolean,
  tags: List[String]
)