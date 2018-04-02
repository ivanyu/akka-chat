package me.ivanyu.akkachat

import akka.http.scaladsl.testkit.WSProbe

import org.scalatest.FlatSpecLike

import me.ivanyu.akkachat.clientserverprotocol.ClientServerProtocol._

class ChatTestMessageAck extends FlatSpecLike with ChatTest {

  "Chat" should "ack messages with increasing sequence number and timestamps" in {
    val id1 = "1"
    val id2 = "2"
    val text1 = "hi there, it's Alice"
    val text2 = "hello-o"

    val wsClient = WSProbe()
    connect(wsClient)

    wsClient.sendMessage(AuthRequest("alice", "alice"))
    wsClient.expectServerMessage(AuthResponse(true))

    wsClient.expectServerMessage[UserJoinedOrLeft]()

    wsClient.sendMessage(ClientToServerMessage(id1, text1))
    wsClient.sendMessage(ClientToServerMessage(id2, text2))

    val ack1 = wsClient.expectServerMessage[MessageAck]()
    val ack2 = wsClient.expectServerMessage[MessageAck]()

    ack1 should matchPattern { case MessageAck(`id1`, _, _) if id1.nonEmpty => }
    ack2 should matchPattern {
      case MessageAck(`id2`, seqN, ts) if id2.nonEmpty && seqN > ack1.seqN && ts.compareTo(ack1.timestamp) >= 0 =>
    }
  }
}
