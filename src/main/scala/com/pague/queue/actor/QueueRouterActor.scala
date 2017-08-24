package com.pague.queue.actor

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import com.pague.queue.actor.QueueRouterActor.MessageApi
import com.pague.queue.actor.queue.QueueActor.MessageQueue
import com.pague.queue.actor.queue._
import com.pague.queue.exception.{NothingToDoException, QueueException}

class QueueRouterActor extends Actor with ActorLogging {

  private val shipmentActor = context.actorOf(QueueShipmentActor.props)
  private val trackActor = context.actorOf(QueueTrackActor.props)
  private val priceActor = context.actorOf(QueuePriceActor.props)

  def receive: Receive = {
    case m: QueueRouterActor.Message => redirectEachMessage(sender, m.messages)
    case _ => sender ! NothingToDoException
  }

  private def redirectEachMessage(originRef: ActorRef, messages: List[MessageApi]) = {
    val aggregatorActor = createAggregatorActor(originRef, messages.length)
    messages.foreach { message =>
      val messageQueue = MessageQueue(aggregatorActor, message.identifier, message.query)
      message.identifier match {
        case RequestIdentifierStatus.Shipment => shipmentActor ! messageQueue
        case RequestIdentifierStatus.Track => trackActor ! messageQueue
        case RequestIdentifierStatus.Price => priceActor ! messageQueue
        case _ => QueueException.notFound[RequestIdentifierStatus.Value]
      }
    }
  }

  private def createAggregatorActor(originRef: ActorRef, requests: Int): ActorRef = {
    context.actorOf(AggregatorActor.props(originRef, requests))
  }
}

object QueueRouterActor {

  val props = Props[QueueRouterActor]

  case class Message(messages: List[MessageApi])

  case class MessageApi(identifier: RequestIdentifierStatus.Value, query: List[String])

}