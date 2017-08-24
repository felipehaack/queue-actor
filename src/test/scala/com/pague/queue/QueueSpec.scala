package com.pague.queue

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import com.pague.queue.actor.QueueRouterActor.Message
import com.pague.queue.actor.{AggregatorActor, QueueRouterActor, RequestIdentifierStatus}

import scala.concurrent.duration._

class QueueSpec extends TestKit(ActorSystem("system"))
  with ImplicitSender with SpecTest {

  private val shipment = RequestIdentifierStatus.Shipment
  private val track = RequestIdentifierStatus.Track
  private val price = RequestIdentifierStatus.Price

  private val shipmentQuery = List("109347263", "123456891")
  private val trackQuery = List("109347263", "123456891")
  private val priceQuery = List("NL", "CN")

  private val shipmentMap = Map(
    "109347263" -> List("envelope", "box", "pallet"),
    "123456891" -> List("envelope")
  )
  private val trackMap = Map(
    "109347263" -> "NEW",
    "123456891" -> "COLLECTING"
  )
  private val priceMap = Map(
    "NL" -> 14.242090605778,
    "CN" -> 20.503467806384
  )
  private val shipmentMessage = QueueRouterActor.MessageApi(shipment, shipmentQuery)
  private val trackMessage = QueueRouterActor.MessageApi(track, trackQuery)
  private val priceMessage = QueueRouterActor.MessageApi(price, priceQuery)

  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }

  "Queue Actor" when {
    "message" should {
      "request data from Shipment API" in {
        val actorRef = system.actorOf(QueueRouterActor.props)

        val requests = List.fill(5)(shipmentMessage)
        actorRef ! Message(requests)
        actorRef ! Message(List(shipmentMessage))

        val shipments = List.fill(5)(shipmentMap)
        val message = AggregatorActor.MessageAggregate(shipments)
        expectMsg(message)
      }

      "request data from Track API" in {
        val actorRef = system.actorOf(QueueRouterActor.props)

        val requests = List.fill(5)(trackMessage)
        actorRef ! Message(requests)
        actorRef ! Message(List(trackMessage))

        val tracks = List.fill(5)(trackMap)
        val message = AggregatorActor.MessageAggregate(track = tracks)
        expectMsg(message)
      }

      "request data from Price API" in {
        val actorRef = system.actorOf(QueueRouterActor.props)

        val requests = List.fill(5)(priceMessage)
        actorRef ! Message(requests)
        actorRef ! Message(List(priceMessage))

        val prices = List.fill(5)(priceMap)
        val message = AggregatorActor.MessageAggregate(price = prices)
        expectMsg(message)
      }

      "request data from All APIs" in {
        val actorRef = system.actorOf(QueueRouterActor.props)

        val shipmentRequest = List.fill(5)(shipmentMessage)
        val trackRequest = List.fill(5)(trackMessage)
        val priceRequest = List.fill(5)(priceMessage)

        actorRef ! Message(shipmentRequest ++ trackRequest ++ priceRequest)
        actorRef ! Message(List(shipmentMessage, trackMessage, priceMessage))

        val shipments = List.fill(5)(shipmentMap)
        val tracks = List.fill(5)(trackMap)
        val prices = List.fill(5)(priceMap)

        val message = AggregatorActor.MessageAggregate(shipments, tracks, prices)
        expectMsg(message)
      }

      "request data from Shipment API and wait to schedule" in {
        val probe = TestProbe()
        val actorRef = system.actorOf(QueueRouterActor.props)

        val requests = List.fill(1)(shipmentMessage)
        probe.send(actorRef, Message(requests))

        val shipments = List.fill(1)(shipmentMap)
        val message = AggregatorActor.MessageAggregate(shipments)
        probe.expectMsg(6 seconds, message)
      }

      "request data from Track API and wait to schedule" in {
        val probe = TestProbe()
        val actorRef = system.actorOf(QueueRouterActor.props)

        val requests = List.fill(1)(trackMessage)
        probe.send(actorRef, Message(requests))

        val tracks = List.fill(1)(trackMap)
        val message = AggregatorActor.MessageAggregate(track = tracks)
        probe.expectMsg(6 seconds, message)
      }

      "request data from Price API and wait to schedule" in {
        val probe = TestProbe()
        val actorRef = system.actorOf(QueueRouterActor.props)

        val requests = List.fill(1)(priceMessage)
        probe.send(actorRef, Message(requests))

        val prices = List.fill(1)(priceMap)
        val message = AggregatorActor.MessageAggregate(price = prices)
        probe.expectMsg(6 seconds, message)
      }

      "request data from All APIs and wait to schedule" in {
        val probe = TestProbe()
        val actorRef = system.actorOf(QueueRouterActor.props)

        val shipmentRequest = List.fill(3)(shipmentMessage)
        val trackRequest = List.fill(2)(trackMessage)
        val priceRequest = List.fill(1)(priceMessage)

        probe.send(actorRef, Message(shipmentRequest ++ trackRequest ++ priceRequest))

        val shipments = List.fill(3)(shipmentMap)
        val tracks = List.fill(2)(trackMap)
        val prices = List.fill(1)(priceMap)

        val message = AggregatorActor.MessageAggregate(shipments, tracks, prices)
        probe.expectMsg(6 seconds, message)
      }
    }
  }
}

