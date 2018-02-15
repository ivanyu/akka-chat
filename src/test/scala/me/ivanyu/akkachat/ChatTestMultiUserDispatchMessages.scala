package me.ivanyu.akkachat

import akka.http.scaladsl.testkit.{ScalatestRouteTest, WSProbe}

import org.scalatest.{FlatSpecLike, Matchers}

import me.ivanyu.akkachat.clientserverprotocol.ClientServerProtocol._

class ChatTestMultiUserDispatchMessages extends FlatSpecLike with Matchers with ScalatestRouteTest with ChatTest {

  "Chat" should "dispatch message to multiple users" in {
    val usernameAlice = "alice"
    val passwordAlice = "alice"
    val usernameBob = "bob"
    val passwordBob = "bob"

    val wsClientA = WSProbe()
    val wsClientB = WSProbe()

    connect(wsClientA)
    wsClientA.sendMessage(AuthRequest(usernameAlice, passwordAlice))
    wsClientA.expectServerMessage(AuthResponse(true))
    wsClientA.expectServerMessage[UserJoinedOrLeft]() should matchPattern {
      case UserJoinedOrLeft(1L, `usernameAlice`, true, _) =>
    }

    connect(wsClientB)
    wsClientB.sendMessage(AuthRequest(usernameBob, passwordBob))
    wsClientB.expectServerMessage(AuthResponse(true))
    wsClientB.expectServerMessage[UserJoinedOrLeft]() should matchPattern {
      case UserJoinedOrLeft(2L, `usernameBob`, true, _) =>
    }

    wsClientA.expectServerMessage[UserJoinedOrLeft]() should matchPattern {
      case UserJoinedOrLeft(2L, `usernameBob`, true, _) =>
    }

    val id1 = "1"
    val id2 = "2"
    val text1 = "hi there, it's Alice"
    val text2 = "hi Alice"

    wsClientA.sendMessage(ClientToServerMessage(id1, text1))
    wsClientA.expectServerMessage[MessageAck]()

    val imB = wsClientB.expectServerMessage[ServerToClientMessage]()
    imB should matchPattern { case ServerToClientMessage(3L, `usernameAlice`, _, `text1`) => }

    wsClientB.sendMessage(ClientToServerMessage(id2, text2))
    wsClientB.expectServerMessage[MessageAck]()

    val imA = wsClientA.expectServerMessage[ServerToClientMessage]()
    imA should matchPattern { case ServerToClientMessage(4L, `usernameBob`, _, `text2`) => }
  }
}
