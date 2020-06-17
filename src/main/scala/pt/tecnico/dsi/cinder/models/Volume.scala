package pt.tecnico.dsi.cinder.models

import cats.effect.Sync
import io.circe.Codec
import io.circe.derivation.{deriveCodec, renaming}
import pt.tecnico.dsi.cinder.CinderClient

object Volume {
  implicit val codec: Codec.AsObject[Volume] = deriveCodec(renaming.snakeCase)

  implicit class WithIdVolumeExtensions[F[_]](volume: WithId[Volume])(implicit client: CinderClient[F], F: Sync[F]) {

  }
}

case class Volume(name: String, description: String, domainId: String) {

}