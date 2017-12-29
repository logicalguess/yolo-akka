package logicalguess.image.rest

import java.io.File
import java.nio.file.{Files, Paths}
import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.unmarshalling.PredefinedFromEntityUnmarshallers
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.FileIO
import akka.util.Timeout
import com.typesafe.scalalogging.Logger
import de.heikoseeberger.akkahttpjson4s.Json4sSupport
import org.json4s.{DefaultFormats, Formats, native}

import scala.sys.process.Process
import scala.util.{Failure, Success}

trait RestInterface extends Json4sSupport {


  implicit val system = ActorSystem("Image")
  implicit val materializer = ActorMaterializer()
  implicit val executionContext = system.dispatcher

  val logger: Logger

  implicit val serialization = native.Serialization
  implicit val stringUnmarshallers = PredefinedFromEntityUnmarshallers.stringUnmarshaller

  implicit def json4sFormats: Formats = DefaultFormats

  implicit val timeout = Timeout(5, TimeUnit.SECONDS)

  val DARKNET_PATH = "../darknet"

  val routes =
    get {
      path("ping") {
        complete("Pong from image")
      } ~
        path("test") {
          val p = Process("./darknet detector test cfg/coco.data cfg/yolo.cfg yolo.weights data/person.jpg",
            new File(DARKNET_PATH))
          //complete("" + p.!!)
          p.!
          val byteArray = Files.readAllBytes(Paths.get(DARKNET_PATH + "/predictions.png"))
          complete(HttpResponse(StatusCodes.OK, entity = HttpEntity(ContentType(MediaTypes.`image/png`), byteArray)))
        }
    } ~
      post {
        path("predict") {
          fileUpload("image") {
            case (fileInfo, fileStream) =>
              val sink = FileIO.toPath(Paths.get("/tmp") resolve fileInfo.fileName)
              val writeResult = fileStream.runWith(sink)
              onSuccess(writeResult) { result =>
                result.status match {
                  case Success(_) =>
                    val p = Process(s"./darknet detector test cfg/coco.data cfg/yolo.cfg yolo.weights /tmp/${fileInfo.fileName}",
                      new File(DARKNET_PATH))
                    p.!
                    val byteArray = Files.readAllBytes(Paths.get(DARKNET_PATH + "/predictions.png"))
                    complete(HttpResponse(StatusCodes.OK, entity = HttpEntity(ContentType(MediaTypes.`image/png`),
                      byteArray)))

                  case Failure(e) => throw e
                }
              }
          }
        }
      }
}

// curl --form "image=@person.jpg" http://localhost:9000/predict > result.png
// docker build -t yolo:latest .
// docker run --rm -it -p 9000:9000 yolo:latest


