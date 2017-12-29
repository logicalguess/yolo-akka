package logicalguess.image.server

import akka.http.scaladsl.Http
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.Logger
import logicalguess.image.rest.RestInterface

object Main extends App with RestInterface {

  val config = ConfigFactory.load()
  val logger = Logger("WebServer")

  Http().bindAndHandle(routes, config.getString("http.interface"), config.getInt("http.port"))
}


