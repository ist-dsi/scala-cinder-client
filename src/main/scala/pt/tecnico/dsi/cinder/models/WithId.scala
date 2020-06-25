package pt.tecnico.dsi.cinder.models

import io.circe.syntax._
import io.circe.{Codec, Decoder, Encoder, HCursor}
import org.http4s.Uri
import org.http4s.circe.decodeUri

object WithId {
  implicit def decoder[T: Decoder]: Decoder[WithId[T]] = (cursor: HCursor) => for {
    id <- cursor.get[String]("id")
    link <- cursor.downField("links").get[Option[Uri]]("self")
    model <- cursor.as[T]
  } yield WithId(id, model, link)
  implicit def encoder[T: Encoder]: Encoder[WithId[T]] = (a: WithId[T]) => a.model.asJson.mapObject(_.add("id", a.id.asJson))
  implicit def codec[T: Codec]: Codec[WithId[T]] = Codec.from(decoder, encoder)

  import scala.language.implicitConversions
  implicit def toModel[T](withId: WithId[T]): T = withId.model
}
// All Openstack IDs are strings, 99% are random UUIDs
case class WithId[T](id: String, model: T, link: Option[Uri])