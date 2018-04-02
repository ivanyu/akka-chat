package me.ivanyu.akkachat

import akka.http.scaladsl.testkit.{ScalatestRouteTest, WSProbe}

import org.scalatest.{FlatSpecLike, Matchers}

import me.ivanyu.akkachat.clientserverprotocol.ClientServerProtocol._

class ChatTestUsersInChat extends FlatSpecLike with Matchers with ScalatestRouteTest with ChatTest {

  "Chat" should "send users" in {
    val usernameA = "alice"
    val passwordA = "alice"
    val usernameC = "charlie"
    val passwordC = "charlie"

    val wsClientA = WSProbe()
    val wsClientC = WSProbe()

    connect(wsClientA)
    wsClientA.sendMessage(AuthRequest(usernameA, passwordA))
    wsClientA.expectServerMessage(AuthResponse(true))
    wsClientA.expectServerMessage[UserJoinedOrLeft]()

    connect(wsClientC)
    wsClientC.sendMessage(AuthRequest(usernameC, passwordC))
    wsClientC.expectServerMessage(AuthResponse(true))
    wsClientC.expectServerMessage[UserJoinedOrLeft]()
    wsClientA.expectServerMessage[UserJoinedOrLeft]()

    wsClientA.sendMessage(GetUsersInChat)
    val usersInChat = wsClientA.expectServerMessage[UsersInChat]()
    usersInChat.users should have length 3
    usersInChat.users.head shouldBe UsersInChat.User("alice", online = true)
    usersInChat.users(1) shouldBe UsersInChat.User("bob", online = false)
    usersInChat.users(2) shouldBe UsersInChat.User("charlie", online = true)
  }
}
