package me.ivanyu.akkachat

import akka.http.scaladsl.testkit.{ScalatestRouteTest, WSProbe}

import org.scalamock.scalatest.MockFactory
import org.scalatest.{FlatSpecLike, Matchers}

import me.ivanyu.akkachat.clientserverprotocol.ClientServerProtocol._

class ChatTestGetChatLogElements
    extends FlatSpecLike
    with Matchers
    with ScalatestRouteTest
    with ChatTest
    with MockFactory {

  override protected def config: AppConfig = super.config.copy(
    Chat = super.config.Chat.copy(
      RequestLogElementCount = 4
    )
  )

  "Chat" should "send log elements on request" in {
    val usernameA = "alice"
    val passwordA = "alice"
    val usernameB = "bob"
    val passwordB = "bob"
    val usernameC = "charlie"
    val passwordC = "charlie"

    val wsClientA = WSProbe()
    val wsClientB = WSProbe()
    val wsClientC = WSProbe()

    connect(wsClientA)
    wsClientA.sendMessage(AuthRequest(usernameA, passwordA))
    wsClientA.expectServerMessage(AuthResponse(true))
    wsClientA.expectServerMessage[UserJoinedOrLeft]()

    val aliceText1 = "No one here..."
    wsClientA.sendMessage(ClientToServerMessage("a1", aliceText1))
    wsClientA.expectServerMessage[MessageAck]()

    connect(wsClientB)
    wsClientB.sendMessage(AuthRequest(usernameB, passwordB))
    wsClientB.expectServerMessage(AuthResponse(true))
    wsClientB.expectServerMessage[UserJoinedOrLeft]()
    wsClientA.expectServerMessage[UserJoinedOrLeft]()

    val bobText1 = "Hi Alice"
    wsClientB.sendMessage(ClientToServerMessage("b1", bobText1))
    wsClientB.expectServerMessage[MessageAck]()
    wsClientA.expectServerMessage[ServerToClientMessage]()

    val aliceText2 = "Oh, hi Bob"
    wsClientA.sendMessage(ClientToServerMessage("a2", aliceText2))
    wsClientA.expectServerMessage[MessageAck]()
    wsClientB.expectServerMessage[ServerToClientMessage]()

    val aliceText3 = "But I need to go"
    wsClientA.sendMessage(ClientToServerMessage("a3", aliceText3))
    wsClientA.expectServerMessage[MessageAck]()
    wsClientB.expectServerMessage[ServerToClientMessage]()

    wsClientA.sendCompletion()
    wsClientB.expectServerMessage[UserJoinedOrLeft]()

    connect(wsClientC)
    wsClientC.sendMessage(AuthRequest(usernameC, passwordC))
    wsClientC.expectServerMessage(AuthResponse(true))
    wsClientC.expectServerMessage[UserJoinedOrLeft]()
    wsClientB.expectServerMessage[UserJoinedOrLeft]()

    wsClientC.sendMessage(GetChatLogElements(None))
    val ChatLogElements(chatLogElements1) = wsClientC.expectServerMessage[ChatLogElements]()

    chatLogElements1 should have length 4

    chatLogElements1.sliding(2).foreach(pair => pair.head.timestamp.compareTo(pair(1).timestamp) should be <= 0)
    chatLogElements1.sliding(2).foreach(pair => pair.head.seqN should be < pair(1).seqN)

    chatLogElements1(0) shouldBe a[ChatLogElement.Message]
    chatLogElements1(0).asInstanceOf[ChatLogElement.Message].seqN shouldBe 5L
    chatLogElements1(0).asInstanceOf[ChatLogElement.Message].username shouldBe usernameA
    chatLogElements1(0).asInstanceOf[ChatLogElement.Message].text shouldBe aliceText2

    chatLogElements1(1) shouldBe a[ChatLogElement.Message]
    chatLogElements1(1).asInstanceOf[ChatLogElement.Message].seqN shouldBe 6L
    chatLogElements1(1).asInstanceOf[ChatLogElement.Message].username shouldBe usernameA
    chatLogElements1(1).asInstanceOf[ChatLogElement.Message].text shouldBe aliceText3

    chatLogElements1(2) shouldBe a[ChatLogElement.UserJoinedOrLeft]
    chatLogElements1(2).asInstanceOf[ChatLogElement.UserJoinedOrLeft].seqN shouldBe 7L
    chatLogElements1(2).asInstanceOf[ChatLogElement.UserJoinedOrLeft].username shouldBe usernameA
    chatLogElements1(2).asInstanceOf[ChatLogElement.UserJoinedOrLeft].joined shouldBe false

    chatLogElements1(3) shouldBe a[ChatLogElement.UserJoinedOrLeft]
    chatLogElements1(3).asInstanceOf[ChatLogElement.UserJoinedOrLeft].seqN shouldBe 8L
    chatLogElements1(3).asInstanceOf[ChatLogElement.UserJoinedOrLeft].username shouldBe usernameC
    chatLogElements1(3).asInstanceOf[ChatLogElement.UserJoinedOrLeft].joined shouldBe true

    val earliestSeqNSoFar = chatLogElements1.map(_.seqN).min

    wsClientC.sendMessage(GetChatLogElements(Some(earliestSeqNSoFar)))
    val ChatLogElements(chatLogElements2) = wsClientC.expectServerMessage[ChatLogElements]()

    chatLogElements2 should have length 4

    chatLogElements2(0) shouldBe a[ChatLogElement.UserJoinedOrLeft]
    chatLogElements2(0).asInstanceOf[ChatLogElement.UserJoinedOrLeft].seqN shouldBe 1L
    chatLogElements2(0).asInstanceOf[ChatLogElement.UserJoinedOrLeft].username shouldBe usernameA
    chatLogElements2(0).asInstanceOf[ChatLogElement.UserJoinedOrLeft].joined shouldBe true

    chatLogElements2(1) shouldBe a[ChatLogElement.Message]
    chatLogElements2(1).asInstanceOf[ChatLogElement.Message].seqN shouldBe 2L
    chatLogElements2(1).asInstanceOf[ChatLogElement.Message].username shouldBe usernameA
    chatLogElements2(1).asInstanceOf[ChatLogElement.Message].text shouldBe aliceText1

    chatLogElements2(2) shouldBe a[ChatLogElement.UserJoinedOrLeft]
    chatLogElements2(2).asInstanceOf[ChatLogElement.UserJoinedOrLeft].seqN shouldBe 3L
    chatLogElements2(2).asInstanceOf[ChatLogElement.UserJoinedOrLeft].username shouldBe usernameB
    chatLogElements2(2).asInstanceOf[ChatLogElement.UserJoinedOrLeft].joined shouldBe true

    chatLogElements2(3) shouldBe a[ChatLogElement.Message]
    chatLogElements2(3).asInstanceOf[ChatLogElement.Message].seqN shouldBe 4L
    chatLogElements2(3).asInstanceOf[ChatLogElement.Message].username shouldBe usernameB
    chatLogElements2(3).asInstanceOf[ChatLogElement.Message].text shouldBe bobText1
  }
}
