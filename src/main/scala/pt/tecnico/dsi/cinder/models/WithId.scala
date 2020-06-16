package pt.tecnico.dsi.cinder.models

import cats.effect.Sync
import cats.syntax.flatMap._
import fs2.Stream
import io.circe.Decoder.Result
import io.circe.syntax._
import io.circe.{Codec, HCursor, Json}
import org.http4s.Uri
import org.http4s.circe.decodeUri
import pt.tecnico.dsi.cinder.CinderClient

object WithId {
  implicit def codec[T: Codec]: Codec[WithId[T]] = new Codec[WithId[T]] {
    override def apply(a: WithId[T]): Json = a.model.asJson.mapObject(_.add("id", a.id.asJson))

    override def apply(c: HCursor): Result[WithId[T]] =
      for {
        id <- c.get[String]("id")
        link <- c.downField("links").get[Option[Uri]]("self")
        model <- c.as[T]
      } yield WithId(id, model, link)
  }

  import scala.language.implicitConversions
  implicit def toModel[T](modelWithId: WithId[T]): T = modelWithId.model
}
// All Openstack IDs are strings, 99% are random UUIDs
case class WithId[T](id: String, model: T, link: Option[Uri])

trait IdFetcher[T <: IdFetcher[T]] {
  def getWithId[F[_]: Sync](implicit client: CinderClient[F]): F[WithId[T]]

  def withId[F[_]: Sync: CinderClient, R](f: WithId[T] => F[R]): F[R] = getWithId.flatMap(f)
  def withId[F[_]: Sync: CinderClient, R](f: WithId[T] => Stream[F, R]): Stream[F, R] = Stream.eval(getWithId).flatMap(f)
}