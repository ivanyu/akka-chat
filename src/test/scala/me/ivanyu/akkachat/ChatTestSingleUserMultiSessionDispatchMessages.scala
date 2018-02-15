package me.ivanyu.akkachat

import akka.http.scaladsl.testkit.{ScalatestRouteTest, WSProbe}

import org.scalatest.{FlatSpecLike, Matchers}

import me.ivanyu.akkachat.clientserverprotocol.ClientServerProtocol._

class ChatTestSingleUserMultiSessionDispatchMessages
    extends FlatSpecLike
    with Matchers
    with ScalatestRouteTest
    with ChatTest {

  "Chat" should "dispatch message to same user sessions" in {
    val id1 = "1"
    val id2 = "2"
    val text1 = "hi there, it's Alice"
    val text2 = "hello-o"

    val username = "alice"
    val password = "alice"

    val wsClient1 = WSProbe()
    val wsClient2 = WSProbe()

    connect(wsClient1)

    wsClient1.sendMessage(AuthRequest(username, password))
    wsClient1.expectServerMessage(AuthResponse(true))
    wsClient1.expectServerMessage[UserJoinedOrLeft]() should matchPattern {
      case UserJoinedOrLeft(1L, `username`, true, _) =>
    }

    connect(wsClient2)
    wsClient2.sendMessage(AuthRequest(username, password))
    wsClient2.expectServerMessage(AuthResponse(true))

    wsClient1.sendMessage(ClientToServerMessage(id1, text1))
    wsClient1.expectServerMessage[MessageAck]()
    wsClient1.expectNoMessage()

    val im1 = wsClient2.expectServerMessage[ServerToClientMessage]()
    im1 should matchPattern { case ServerToClientMessage(2L, `username`, _, `text1`) => }

    wsClient2.sendMessage(ClientToServerMessage(id2, text2))
    wsClient2.expectServerMessage[MessageAck]()
    wsClient2.expectNoMessage()

    val im2 = wsClient1.expectServerMessage[ServerToClientMessage]()
    im2 should matchPattern { case ServerToClientMessage(3L, `username`, _, `text2`) => }
  }
}
