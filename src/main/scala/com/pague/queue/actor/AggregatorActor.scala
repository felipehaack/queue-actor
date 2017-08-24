package com.pague.queue.actor

import java.util.concurrent.atomic.AtomicLong

import akka.actor.{Actor, ActorLogging, ActorRef, Props, Stash}
import com.pague.queue.actor.AggregatorActor.MessageAggregate
import com.pague.queue.exception.{NothingToDoException, QueueException}

import scala.collection.mutable.ListBuffer

class AggregatorActor(
                       originRef: ActorRef,
                       requests: Int
                     ) extends Actor with ActorLogging with Stash {

  private val counter = new AtomicLong(0)

  private val shipments = new ListBuffer[ResolverType.Shipment]()
  private val tracks = new ListBuffer[ResolverType.Track]()
  private val prices = new ListBuffer[ResolverType.Price]()

  def receive: Receive = {
    case m: RequestApiActor.Shipment =>
      shipments.append(m.shipment)
      processRequest()
    case m: RequestApiActor.Track =>
      tracks.append(m.track)
      processRequest()
    case m: RequestApiActor.Price =>
      prices.append(m.price)
      processRequest()
    case _: QueueException =>
      originRef ! QueueException.systemError("impossible retrieve request")
      context.stop(self)
    case _ => sender ! NothingToDoException
  }

  private def processRequest() = {
    counter.incrementAndGet()

    if (counter.get() == requests) {
      val response = MessageAggregate(shipments.toList, tracks.toList, prices.toList)
      originRef ! response
      context.stop(self)
    }
  }
}

object AggregatorActor {

  def props(originRef: ActorRef, requests: Int) = Props(new AggregatorActor(originRef, requests))

  case class MessageAggregate(
                               shipment: List[ResolverType.Shipment] = List.empty,
                               track: List[ResolverType.Track] = List.empty,
                               price: List[ResolverType.Price] = List.empty
                             )

}