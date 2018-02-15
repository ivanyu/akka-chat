package me.ivanyu.akkachat

import akka.http.scaladsl.testkit.{ScalatestRouteTest, WSProbe}

import org.scalatest.{FlatSpecLike, Matchers}

import me.ivanyu.akkachat.clientserverprotocol.ClientServerProtocol._

class ChatTestSingleUserRejoin extends FlatSpecLike
  with Matchers with ScalatestRouteTest with ChatTest {

  "Chat" should "correctly process user rejoining" in {
    val id1 = "1"
    val text1 = "hi there, it's Alice"

    val username = "alice"
    val password = "alice"

    val wsClient1 = WSProbe()

    connect(wsClient1)

    wsClient1.sendMessage(AuthRequest(username, password))
    wsClient1.expectServerMessage(AuthResponse(true))
    wsClient1.expectServerMessage[UserJoinedOrLeft]() should matchPattern {
      case UserJoinedOrLeft(1L, `username`, true, _) =>
    }

    wsClient1.sendMessage(ClientToServerMessage(id1, text1))
    wsClient1.expectServerMessage[MessageAck]()
    wsClient1.sendCompletion()

    val wsClient2 = WSProbe()
    connect(wsClient2)
    wsClient2.sendMessage(AuthRequest(username, password))
    wsClient2.expectServerMessage(AuthResponse(true))
    wsClient2.expectServerMessage[UserJoinedOrLeft]() should matchPattern {
      case UserJoinedOrLeft(4L, `username`, true, _) =>
    }
  }
}
