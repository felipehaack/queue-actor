package com.pague.queue.actor.queue

import akka.actor.Props

class QueuePriceActor extends AbstractQueueActor {

}

object QueuePriceActor {
  val props = Props[QueuePriceActor]
}
