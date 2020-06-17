package pt.tecnico.dsi.cinder.services

import cats.effect.Sync
import cats.syntax.functor._
import fs2.Stream
import io.circe.Codec
import org.http4s.Method.POST
import org.http4s.Status.{Conflict, Successful}
import org.http4s.client.{Client, UnexpectedStatus}
import org.http4s.{Header, Query, Response, Uri}
import pt.tecnico.dsi.cinder.models.WithId

abstract class CRUDService[F[_]: Sync: Client, T: Codec](baseUri: Uri, val name: String, authToken: Header)
  extends BaseService[F](authToken) {

  import dsl._

  val pluralName = s"${name}s"
  override val uri: Uri = baseUri / pluralName

  def list(): Stream[F, WithId[T]] = super.list[WithId[T]](pluralName, uri, Query.empty)
  def list(query: Query): Stream[F, WithId[T]] = super.list[WithId[T]](pluralName, uri, query)

  def create(value: T): F[WithId[T]] = super.create(name, value)

  protected def createHandleConflict(value: T)(onConflict: Response[F] => F[WithId[T]]): F[WithId[T]] =
    client.fetch(POST(wrap[T](value, name), uri, authToken)){
      case Successful(response) => response.as[Map[String, WithId[T]]].map(_.apply(name))
      case Conflict(response) => onConflict(response)
      case response => F.raiseError(UnexpectedStatus(response.status))
    }

  def get(id: String): F[WithId[T]] = super.get(name, id)

  def update(value: WithId[T]): F[WithId[T]] = update(value.id, value.model)
  def update(id: String, value: T): F[WithId[T]] = super.update(name, id, value)

  def delete(value: WithId[T]): F[Unit] = delete(value.id)
  override def delete(id: String): F[Unit] = super.delete(id)
}