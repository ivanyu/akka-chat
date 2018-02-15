package me.ivanyu.akkachat

import akka.http.scaladsl.testkit.WSProbe

import org.scalatest.FlatSpecLike

import me.ivanyu.akkachat.clientserverprotocol.ClientServerProtocol._

class ChatTestDuplicateMessages extends FlatSpecLike with ChatTest {

  "Chat" should "ack messages with duplicate ids with the same acks if send immediately one after another" in {
    val wsClient = WSProbe()
    connect(wsClient)

    wsClient.sendMessage(AuthRequest("alice", "alice"))
    wsClient.expectServerMessage(AuthResponse(true))

    wsClient.expectServerMessage[UserJoinedOrLeft]()

    // Send immediately one after another
    wsClient.sendMessage(ClientToServerMessage("someid1", "sometext"))
    wsClient.sendMessage(ClientToServerMessage("someid1", "sometext"))

    val ack = wsClient.expectServerMessage[MessageAck]()
    ack.clientSideId shouldBe "someid1"

    wsClient.expectNoMessage()
  }

  it should "ack messages with duplicate ids with the same acks if send and acked" in {
    val wsClient = WSProbe()
    connect(wsClient)

    wsClient.sendMessage(AuthRequest("bob", "bob"))
    wsClient.expectServerMessage(AuthResponse(true))

    wsClient.expectServerMessage[UserJoinedOrLeft]()

    wsClient.sendMessage(ClientToServerMessage("someid2", "sometext"))
    val ack1 = wsClient.expectServerMessage[MessageAck]()

    wsClient.sendMessage(ClientToServerMessage("someid2", "sometext"))
    val ack2 = wsClient.expectServerMessage[MessageAck]()

    ack2 should equal(ack1)
  }
}
