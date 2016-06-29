package com.example

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer
import akka.util.ByteString
import com.typesafe.config.ConfigFactory
import redis.RedisClient

import scala.concurrent.Future
import scala.io.StdIn

object Server {
  private val config =  ConfigFactory.load()

  private val redisConfig = config.getConfig("redis")
  private val redisHost = redisConfig.getString("host")
  private val redisPort = redisConfig.getInt("port")
  private val redisPassword = if (redisConfig.hasPath("password")) Some(redisConfig.getString("password")) else None

  private val httpConfig = config.getConfig("http")
  private val interface = httpConfig.getString("interface")
  private val port = httpConfig.getInt("port")

  def main(args: Array[String]) {
    implicit val system = ActorSystem("my-system")
    implicit val materializer = ActorMaterializer()
    // needed for the future flatMap/onComplete in the end
    implicit val executionContext = system.dispatcher

    val redis = RedisClient(redisHost, redisPort, redisPassword)

    val route: Route =
      get {
        pathPrefix("get" / Remaining) { key =>
          // there might be no item for a given id
          val maybeItem: Future[Option[ByteString]] = redis.get(key)

          onSuccess(maybeItem) {
            case Some(item) => complete(HttpEntity(ContentTypes.`application/json`, s"""{"key": $key, "value": "${item.utf8String}"}"""))
            case None       => complete(StatusCodes.NoContent)
          }
        }
      }

    val bindingFuture = Http().bindAndHandle(route, interface, port)

    println(s"Server online at http://$interface:$port/\nPress RETURN to stop...")
    StdIn.readLine() // let it run until user presses return
    bindingFuture
      .flatMap(_.unbind()) // trigger unbinding from the port
      .onComplete(_ => system.terminate()) // and shutdown when done
  }
}