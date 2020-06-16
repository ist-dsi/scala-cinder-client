package pt.tecnico.dsi.cinder

import cats.Applicative
import cats.effect.Sync
import io.circe.{Decoder, Encoder, Printer}
import org.http4s.{EntityDecoder, EntityEncoder, circe}

package object services {
  val jsonPrinter: Printer = Printer.noSpaces.copy(dropNullValues = true)
  implicit def jsonEncoder[F[_]: Applicative, A: Encoder]: EntityEncoder[F, A] = circe.jsonEncoderWithPrinterOf[F, A](jsonPrinter)
  implicit def jsonDecoder[F[_]: Sync, A: Decoder]: EntityDecoder[F, A] = circe.accumulatingJsonOf[F, A]

  // Without this decoding to Unit wont work. This makes the EntityDecoder[F, Unit] defined in EntityDecoder companion object
  // have a higher priority than the jsonDecoder defined above. https://github.com/http4s/http4s/issues/2806
  implicit def void[F[_]: Sync]: EntityDecoder[F, Unit] = EntityDecoder.void
}
