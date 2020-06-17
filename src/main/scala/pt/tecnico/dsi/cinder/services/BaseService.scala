package pt.tecnico.dsi.cinder.services

import cats.effect.Sync
import cats.syntax.flatMap._
import cats.syntax.functor._
import fs2.{Chunk, Stream}
import io.circe.{Decoder, Encoder, HCursor}
import org.http4s.Status.{NotFound, Successful}
import org.http4s.circe.decodeUri
import org.http4s.client.dsl.Http4sClientDsl
import org.http4s.client.{Client, UnexpectedStatus}
import org.http4s.Method.{GET, PATCH, DELETE, POST}
import org.http4s.{Header, Query, Request, Uri}
import pt.tecnico.dsi.cinder.models.WithId

abstract class BaseService[F[_]](protected val authToken: Header)(implicit protected val client: Client[F], protected val F: Sync[F]) {
  val uri: Uri
  protected val dsl = new Http4sClientDsl[F] {}
  import dsl._

  protected def unwrap[T: Decoder](request: F[Request[F]], name: String): F[WithId[T]] =
    client.expect[Map[String, WithId[T]]](request).map(_.apply(name))
  protected def wrap[T](value: T, name: String): Map[String, T] = Map(name -> value)

  protected def get[T: Decoder](name: String, id: String): F[WithId[T]] =
    unwrap(GET(uri / id, authToken), name)

  protected def update[T: Decoder, R: Encoder](name: String, id: String, value: R): F[WithId[T]] =
    unwrap(PATCH(wrap(value, name), uri / id, authToken), name)

  protected def create[T: Decoder, R: Encoder](name: String, value: R): F[WithId[T]] =
    unwrap(POST(wrap(value, name), uri, authToken), name)

  protected def list[R: Decoder](baseKey: String, uri: Uri, query: Query = Query.empty): Stream[F, R] = {
    implicit val paginatedDecoder: Decoder[(Option[Uri], List[R])] = (c: HCursor) => for {
      links <- c.downField("links").get[Option[Uri]]("next")
      objectList <- c.downField(baseKey).as[List[R]]
    } yield (links, objectList)

    Stream.unfoldChunkEval[F, Option[Uri], R](Some(uri)) {
      case Some(uri) =>
        for {
          request <- GET(uri.copy(query = uri.query ++ query.pairs), authToken)
          (next, entries) <- client.expect[(Option[Uri], List[R])](request)
        } yield Some((Chunk.iterable(entries), next))
      case None => F.pure(None)
    }
  }

  protected def delete(id: String): F[Unit] =
    client.fetch(DELETE(uri / id, authToken)) {
      case Successful(_) | NotFound(_) => F.pure(())
      case response => F.raiseError(UnexpectedStatus(response.status))
    }
}