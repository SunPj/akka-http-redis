package com.example

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer
import akka.util.ByteString
import redis.RedisClient

import scala.concurrent.Future
import scala.io.StdIn

object Server {
  def main(args: Array[String]) {
    implicit val system = ActorSystem("my-system")
    implicit val materializer = ActorMaterializer()
    // needed for the future flatMap/onComplete in the end
    implicit val executionContext = system.dispatcher

    val redis = RedisClient()

    val route: Route =
      get {
        pathPrefix("get" / LongNumber) { key =>
          // there might be no item for a given id
          val maybeItem: Future[Option[ByteString]] = redis.get(key.toString)

          onSuccess(maybeItem) {
            case Some(item) => complete(HttpEntity(ContentTypes.`application/json`, s"""{"key": $key, "value": "${item.utf8String}"}"""))
            case None       => complete(StatusCodes.NoContent)
          }
        }
      }

    val bindingFuture = Http().bindAndHandle(route, "0.0.0.0", 8888)

    println(s"Server online at http://localhost:8888/\nPress RETURN to stop...")
    StdIn.readLine() // let it run until user presses return
    bindingFuture
      .flatMap(_.unbind()) // trigger unbinding from the port
      .onComplete(_ => system.terminate()) // and shutdown when done
  }
}