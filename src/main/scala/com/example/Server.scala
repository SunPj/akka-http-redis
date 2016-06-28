package com.example

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer
import eu.unicredit.reactive_aerospike.client.AerospikeClient
import eu.unicredit.reactive_aerospike.data.AerospikeKey
import eu.unicredit.reactive_aerospike.data.AerospikeValue.AerospikeStringConverter

import scala.io.StdIn

object Server {
  def main(args: Array[String]) {
    implicit val system = ActorSystem("my-system")
    implicit val materializer = ActorMaterializer()
    // needed for the future flatMap/onComplete in the end
    implicit val executionContext = system.dispatcher

    implicit val client = AerospikeClient("127.0.0.1", 3000)

    val route: Route =
      get {
        pathPrefix("get" / LongNumber) { key =>
          val res = TrackDAO.read(AerospikeKey(TrackDAO.namespace, TrackDAO.setName, key.toString))
          val opRes = res map (Some(_)) recover { case e => None }

          onSuccess(opRes) {
            case Some(track) => complete(HttpEntity(ContentTypes.`application/json`, s"""{"key": $key, "value": "${track.trackName}"}"""))
            case None => complete(StatusCodes.NoContent)
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