package me.ivanyu.akkachat

import akka.http.scaladsl.testkit.WSProbe

import me.ivanyu.akkachat.clientserverprotocol.ClientServerProtocol._
import org.scalatest.FlatSpecLike

class ChatTestDisconnection extends FlatSpecLike with ChatTest {

  "Chat" should "handle disconnection" in {
    val usernameAlice = "alice"
    val passwordAlice = "alice"
    val usernameBob = "bob"
    val passwordBob = "bob"

    val wsClientA1 = WSProbe()
    val wsClientA2 = WSProbe()
    val wsClientB = WSProbe()

    connect(wsClientA1)
    wsClientA1.sendMessage(AuthRequest(usernameAlice, passwordAlice))
    wsClientA1.expectServerMessage(AuthResponse(true))
    wsClientA1.expectServerMessage[UserJoinedOrLeft]()

    connect(wsClientA2)
    wsClientA2.sendMessage(AuthRequest(usernameAlice, passwordAlice))
    wsClientA2.expectServerMessage(AuthResponse(true))

    connect(wsClientB)
    wsClientB.sendMessage(AuthRequest(usernameBob, passwordBob))
    wsClientB.expectServerMessage(AuthResponse(true))
    wsClientB.expectServerMessage[UserJoinedOrLeft]()
    wsClientA1.expectServerMessage[UserJoinedOrLeft]()
    wsClientA2.expectServerMessage[UserJoinedOrLeft]()

    wsClientA1.sendCompletion()
    wsClientB.expectNoMessage()
    wsClientA2.expectNoMessage()

    wsClientA2.sendCompletion()
    wsClientB.expectServerMessage[UserJoinedOrLeft]() should matchPattern {
      case UserJoinedOrLeft(3L, `usernameAlice`, false, _) =>
    }
  }
}
