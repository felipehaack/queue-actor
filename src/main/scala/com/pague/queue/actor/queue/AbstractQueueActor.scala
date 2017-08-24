package com.pague.queue.actor.queue

import java.util.concurrent.atomic.{AtomicLong, AtomicReference}

import akka.actor.{Actor, ActorLogging, ActorRef, Cancellable, Stash}
import com.pague.queue.actor.RequestApiActor.Request
import com.pague.queue.actor.queue.QueueActor.{MessageQueue, ReleaseAll}
import com.pague.queue.actor.{RequestApiActor, RequestIdentifierStatus}
import com.pague.queue.exception.NothingToDoException

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

abstract class AbstractQueueActor extends Actor with Stash with ActorLogging {

  private val requestApiActor = context.actorOf(RequestApiActor.props)

  private val maxTime = 5
  private val capacity = 5
  private val counter = new AtomicLong(0)
  private val schedule = new AtomicReference[Cancellable]

  def receive: Receive = {
    case m if counter.get() == capacity || m == ReleaseAll =>
      releaseAll()
      cancelSchedule()
    case _: MessageQueue =>
      stash()
      counter.incrementAndGet()
      if (counter.get() == 1)
        registerSchedule()
    case _ => sender ! NothingToDoException
  }

  private def releaseAll() = {
    unstashAll()
    context.become({
      case m: MessageQueue => send(m)
    })
    counter.set(0)
  }

  private def registerSchedule() = {
    schedule.set(context.system.scheduler.scheduleOnce(maxTime.seconds, self, ReleaseAll))
  }

  private def cancelSchedule() = {
    schedule.get().cancel()
  }

  private def send(message: MessageQueue) = {
    requestApiActor ! Request(message.aggregatorRef, message.identifier, message.query)
  }
}

object QueueActor {

  case class MessageQueue(aggregatorRef: ActorRef, identifier: RequestIdentifierStatus.Value, query: List[String])

  case object ReleaseAll

}