package pt.tecnico.dsi.openstack.cinder

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationInt
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Random
import cats.effect.{ContextShift, IO, Resource, Timer}
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
import pt.tecnico.dsi.openstack.common.models.WithId
import pt.tecnico.dsi.openstack.keystone.KeystoneClient
import pt.tecnico.dsi.openstack.keystone.models.{CatalogEntry, Interface, Project, Scope}

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

  //import org.http4s.client.middleware.Logger
  //implicit val httpClient: Client[IO] = Logger(logBody = true, logHeaders = true)(_httpClient)
  implicit val httpClient: Client[IO] = _httpClient

  val keystone: KeystoneClient[IO] = KeystoneClient.fromEnvironment().unsafeRunSync()

  val cinder: CinderClient[IO] = {
    val cinderUrl = keystone.session.catalog.collectFirst {
      case entry @ CatalogEntry("volumev3", _, _, _) => entry.urlOf(sys.env("OS_REGION_NAME"), Interface.Public)
    }.flatten.getOrElse(throw new Exception("Could not find \"volumev3\" service in the catalog"))
    // Since we performed a scoped authentication to the admin project openstack tries to be clever and returns the cinder public url already
    // scoped to that project. That is: instead of returning "https://somehost.com:8776/v3", it returns "https://somehost.com:8776/v3/<admin-project-id>"
    // So we need to drop the admin-project-id
    val adminProjectId = keystone.session.scope.asInstanceOf[Scope.Project].id
    new CinderClient[IO](Uri.unsafeFromString(cinderUrl.stripSuffix(s"/$adminProjectId")), keystone.authToken)
  }

  // Not very purely functional :(
  val random = new Random()
  def randomName(): String = random.alphanumeric.take(10).mkString.dropWhile(_.isDigit).toLowerCase
  def withRandomName[T](f: String => IO[T]): IO[T] = IO.delay(randomName()).flatMap(f)

  val withStubProject: Resource[IO, WithId[Project]] = {
    val create = withRandomName(name => keystone.projects.create(Project(name, "dummy project", "default")))
    Resource.make(create)(project => keystone.projects.delete(project))
  }

  val adminProject: IO[WithId[Project]] = keystone.projects.get("admin", keystone.session.user.domainId)

  implicit class RichIO[T](io: IO[T]) {
    def idempotently(test: T => Assertion, repetitions: Int = 3): IO[Assertion] = {
      require(repetitions >= 2, "To test for idempotency at least 2 repetitions must be made")
      io.flatMap { firstResult =>
        // If this fails we do not want to mask its exception with "Operation is not idempotent".
        // Because failing in the first attempt means whatever is being tested in `test` is not implemented correctly.
        test(firstResult)
        (2 to repetitions).toList.traverse(_ => io) map { results =>
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
  }

  import scala.language.implicitConversions
  implicit def ioAssertion2FutureAssertion(io: IO[Assertion]): Future[Assertion] = io.unsafeToFuture()
}
