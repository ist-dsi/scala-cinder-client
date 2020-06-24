package pt.tecnico.dsi.cinder

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationInt
import scala.concurrent.{ExecutionContext, Future}
import cats.effect.{ContextShift, IO, Timer}
import cats.instances.list._
import cats.syntax.traverse._
import org.http4s.Uri
import org.http4s.client.Client
import org.http4s.client.blaze.BlazeClientBuilder
import org.log4s._
import org.scalatest._
import org.scalatest.exceptions.TestFailedException
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AsyncWordSpec
import pt.tecnico.dsi.keystone.KeystoneClient
import pt.tecnico.dsi.keystone.models.Scope.Project
import pt.tecnico.dsi.keystone.models.{CatalogEntry, Interface, ScopedSession}

abstract class Utils extends AsyncWordSpec with Matchers with BeforeAndAfterAll {
  val logger: Logger = getLogger

  implicit override def executionContext = ExecutionContext.global

  implicit val timer: Timer[IO] = IO.timer(executionContext)
  implicit val cs: ContextShift[IO] = IO.contextShift(executionContext)

  val (_httpClient, finalizer) = BlazeClientBuilder[IO](global)
    .withResponseHeaderTimeout(20.seconds)
    .withCheckEndpointAuthentication(false)
    .resource.allocated.unsafeRunSync()

  override protected def afterAll(): Unit = finalizer.unsafeRunSync()

  implicit val httpClient: Client[IO] = _httpClient

  val keystoneClient: IO[KeystoneClient[IO]] = KeystoneClient.fromEnvironment()

  val client: IO[CinderClient[IO]] = for {
    keystone <- keystoneClient
    // Keystone should be smarter and not require us to do this cast
    session = keystone.session.asInstanceOf[ScopedSession]
    cinderUrlOpt = session.catalog.collectFirst { case entry @ CatalogEntry("volumev3", _, _, _) => entry.urlOf(sys.env("OS_REGION_NAME"), Interface.Public) }
    cinderUrl <- IO.fromEither(cinderUrlOpt.flatten.toRight(new Throwable("Could not find \"volumev3\" service in the catalog")))
    // Since we performed a scoped authentication to the admin project openstack tries to be clever and returns the cinder public url already
    // scoped to that project. That is: instead of returning "https://somehost.com:8776/v3", it returns "https://somehost.com:8776/v3/<admin-project-id>"
    // So we need to drop the admin-project-id
    adminProjectId = session.scope.asInstanceOf[Project].project.id.get
  } yield new CinderClient[IO](Uri.unsafeFromString(cinderUrl.stripSuffix(s"/$adminProjectId")), keystone.authToken)

  implicit class RichIO[T](io: IO[T]) {
    def value(test: T => Assertion): IO[Assertion] = io.map(test)

    def valueShouldBe(v: T): IO[Assertion] = value(_ shouldBe v)

    def idempotently(test: T => Assertion, repetitions: Int = 3): IO[Assertion] = {
      require(repetitions >= 2, "To test for idempotency at least 2 repetitions must be made")
      io.flatMap { firstResult =>
        // If this fails we do not want to mask its exception with "Operation is not idempotent".
        // Because failing in the first attempt means whatever is being tested in `test` is not implemented correctly.
        test(firstResult)
        (2 to repetitions).toList.traverse { _ =>
          io
        } map { results =>
          // And now we want to catch the exception because if `test` fails here it means it is not idempotent.
          try {
            results.foreach(test)
            succeed
          } catch {
            case e: TestFailedException =>
              val numberOfDigits = Math.floor(Math.log10(repetitions.toDouble)).toInt + 1
              val resultsString = (firstResult +: results).zipWithIndex
                .map { case (result, i) =>
                  s" %${numberOfDigits}d: %s".format(i + 1, result)
                }.mkString("\n")
              throw e.modifyMessage(_.map(message =>
                s"""Operation is not idempotent. Results:
                   |$resultsString
                   |$message""".stripMargin))
          }
        }
      }
    }

    def valueShouldIdempotentlyBe(value: T): IO[Assertion] = idempotently(_ shouldBe value)
  }

  import scala.language.implicitConversions
  implicit def io2Future[T](io: IO[T]): Future[T] = io.unsafeToFuture()

  private def ordinalSuffix(number: Int): String = {
    number % 100 match {
      case 1 => "st"
      case 2 => "nd"
      case 3 => "rd"
      case _ => "th"
    }
  }

  def idempotently(test: CinderClient[IO] => IO[Assertion], repetitions: Int = 3): Future[Assertion] = {
    require(repetitions >= 2, "To test for idempotency at least 2 repetitions must be made")

    // If the first run fails we do not want to mask its exception, because failing in the first attempt means
    // whatever is being tested in `test` is not implemented correctly.
    client.flatMap(test).unsafeToFuture().flatMap { _ =>
      // For the subsequent iterations we mask TestFailed with "Operation is not idempotent"
      Future.traverse(2 to repetitions) { repetition =>
        client.flatMap(test).unsafeToFuture().transform(identity, {
          case e: TestFailedException =>
            val text = s"$repetition${ordinalSuffix(repetition)}"
            e.modifyMessage(_.map(m => s"Operation is not idempotent. On $text repetition got:\n$m"))
          case e => e
        })
      } map (_ should contain only (Succeeded)) // Scalatest flatten :P
    }
  }
}
