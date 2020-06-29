package pt.tecnico.dsi.cinder.models

import io.circe.syntax._
import io.circe.{Codec, Decoder, Encoder, HCursor, Json, JsonObject}
import org.http4s.Uri
import org.http4s.circe.decodeUri

object WithId {
  implicit val linksDecoder: Decoder[Map[String, Uri]] = { cursor: HCursor =>
    // Openstack has two ways to represent links (because why not):
    // This one is mostly used in Keystone
    //   "links": {
    //     "self": "http://example.com/identity/v3/role_assignments",
    //     "previous": null,
    //     "next": null
    //   }
    // This one is mostly used everywhere else
    //   "links": [
    //     {
    //       "href": "http://127.0.0.1:33951/v3/89afd400-b646-4bbc-b12b-c0a4d63e5bd3/volumes/2b955850-f177-45f7-9f49-ecb2c256d161",
    //       "rel": "self"
    //     }, {
    //       "href": "http://127.0.0.1:33951/89afd400-b646-4bbc-b12b-c0a4d63e5bd3/volumes/2b955850-f177-45f7-9f49-ecb2c256d161",
    //       "rel": "bookmark"
    //     }
    cursor.value.dropNullValues.withArray { entries =>
      Json.fromFields(entries.flatMap(_.dropNullValues.asObject).flatMap(_.toList))
    }.as[Map[String, Uri]](Decoder.decodeMap)// Needs explicit instance, otherwise it will use the linksDecoder and cause stack overflow
  }

  implicit def decoder[T: Decoder]: Decoder[WithId[T]] = (cursor: HCursor) => for {
    id <- cursor.get[String]("id")
    link <- cursor.getOrElse[Map[String, Uri]]("links")(Map.empty)
    model <- cursor.as[T]
  } yield WithId(id, model, link)
  implicit def encoder[T: Encoder.AsObject]: Encoder.AsObject[WithId[T]] = (a: WithId[T]) => a.model.asJsonObject.add("id", a.id.asJson)
  implicit def codec[T: Codec.AsObject]: Codec.AsObject[WithId[T]] = Codec.AsObject.from(decoder, encoder)

  import scala.language.implicitConversions
  implicit def toModel[T](withId: WithId[T]): T = withId.model
}
// All Openstack IDs are strings, 99% are random UUIDs
case class WithId[T](id: String, model: T, links: Map[String, Uri] = Map.empty)