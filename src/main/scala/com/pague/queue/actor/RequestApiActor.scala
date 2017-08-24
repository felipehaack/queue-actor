package com.pague.queue.actor

import akka.actor.{Actor, ActorRef, Props}
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpResponse, StatusCodes}
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.ActorMaterializer
import com.pague.queue.exception.{NothingToDoException, QueueException}
import spray.json.DefaultJsonProtocol._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success}

class RequestApiActor extends Actor {

  private implicit val materialize = ActorMaterializer()

  def receive: Receive = {
    case m: RequestApiActor.Request =>
      m.identifier match {
        case RequestIdentifierStatus.Shipment =>
          requestShipment().onComplete {
            case Success(response) => unmarshalShipment(m, response)
            case Failure(_) => timeoutException(m.aggregatorRef)
          }
        case RequestIdentifierStatus.Track =>
          requestTrack().onComplete {
            case Success(response) => unmarshalTrack(m, response)
            case Failure(_) => timeoutException(m.aggregatorRef)
          }
        case RequestIdentifierStatus.Price =>
          requestPrice().onComplete {
            case Success(response) => unmarshalPrice(m, response)
            case Failure(_) => timeoutException(m.aggregatorRef)
          }
        case _ => sender ! QueueException.notFound[RequestIdentifierStatus.Value]
      }
    case _ => sender ! NothingToDoException
  }

  private def requestShipment() = Future {
    val trackMessage = """{"109347263":["envelope","box","pallet"],"123456891":["envelope"]}"""
    val httpEntity = HttpEntity(ContentTypes.`application/json`, trackMessage)
    val httpStatus = StatusCodes.OK
    val httpResponse = HttpResponse(httpStatus, entity = httpEntity)
    httpResponse
  }

  private def requestTrack() = Future {
    val trackMessage = """{"109347263":"NEW","123456891":"COLLECTING"}"""
    val httpEntity = HttpEntity(ContentTypes.`application/json`, trackMessage)
    val httpStatus = StatusCodes.OK
    val httpResponse = HttpResponse(httpStatus, entity = httpEntity)
    httpResponse
  }

  private def requestPrice() = Future {
    val trackMessage = """{"NL":14.242090605778,"CN":20.503467806384}"""
    val httpEntity = HttpEntity(ContentTypes.`application/json`, trackMessage)
    val httpStatus = StatusCodes.OK
    val httpResponse = HttpResponse(httpStatus, entity = httpEntity)
    httpResponse
  }


  private def unmarshalShipment(request: RequestApiActor.Request, httpResponse: HttpResponse) = {
    val unmarshal = Unmarshal(httpResponse).to[ResolverType.Shipment]
    unmarshal.onComplete {
      case Success(shipment) => request.aggregatorRef ! RequestApiActor.Shipment(shipment)
      case Failure(_) => request.aggregatorRef ! Failure(QueueException.systemError("impossible parse json"))
    }
  }

  private def unmarshalTrack(request: RequestApiActor.Request, httpResponse: HttpResponse) = {
    val unmarshal = Unmarshal(httpResponse).to[ResolverType.Track]
    unmarshal.onComplete {
      case Success(track) => request.aggregatorRef ! RequestApiActor.Track(track)
      case Failure(_) => request.aggregatorRef ! Failure(QueueException.systemError("impossible parse json"))
    }
  }

  private def unmarshalPrice(request: RequestApiActor.Request, httpResponse: HttpResponse) = {
    val unmarshal = Unmarshal(httpResponse).to[ResolverType.Price]
    unmarshal.onComplete {
      case Success(price) => request.aggregatorRef ! RequestApiActor.Price(price)
      case Failure(_) => request.aggregatorRef ! Failure(QueueException.systemError("impossible parse json"))
    }
  }

  private def timeoutException(aggregatorRef: ActorRef) = {
    aggregatorRef ! QueueException.systemError("timeout")
  }
}

object RequestApiActor {

  val props = Props[RequestApiActor]

  case class Request(aggregatorRef: ActorRef, identifier: RequestIdentifierStatus.Value, query: List[String])

  case class Shipment(shipment: ResolverType.Shipment)

  case class Track(track: ResolverType.Track)

  case class Price(price: ResolverType.Price)

}

object RequestIdentifierStatus extends Enumeration {
  val Shipment = Value(1, "shipment")
  val Track = Value(2, "track")
  val Price = Value(3, "price")
}

object ResolverType {
  type Shipment = Map[String, List[String]]
  type Track = Map[String, String]
  type Price = Map[String, Double]
}
