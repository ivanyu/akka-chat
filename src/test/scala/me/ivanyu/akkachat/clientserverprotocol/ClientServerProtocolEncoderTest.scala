package me.ivanyu.akkachat.clientserverprotocol

import java.time.ZonedDateTime

import io.circe.Json
import org.scalatest.{FlatSpec, Matchers}

import me.ivanyu.akkachat.clientserverprotocol.ClientServerProtocol._
import me.ivanyu.akkachat.clientserverprotocol.ClientServerProtocolEncoder.encodeTopLevel

class ClientServerProtocolEncoderTest extends FlatSpec with Matchers {

  "ClientServerProtocolDecoder" should "encode Error" in {
    val error = Error("some reason")
    encodeTopLevel(error) shouldBe s"""{"msgType":"error","reason":"${error.reason}"}"""
  }

  it should "encode AuthResponse" in {
    val authResp = AuthResponse(true)
    encodeTopLevel(authResp) shouldBe s"""{"msgType":"authResponse","success":${authResp.success}}"""
  }

  it should "encode MessageAck" in {
    val dt = ZonedDateTime.now()
    val msgAck = MessageAck("aaa", 12, dt)
    val expected = s"""{"msgType":"messageAck","clientSideId":"aaa","seqN":12,"timestamp":${encodeDateTime(dt)}}"""
    encodeTopLevel(msgAck) shouldBe expected
  }

  it should "encode ServerToClientMessage" in {
    val dt = ZonedDateTime.now()
    val im = ServerToClientMessage(12, "alice", dt, "sometext")
    val expected =
      """{"msgType":"serverToClientMessage","seqN":12,"username":"alice","timestamp":""" +
        s"""${encodeDateTime(dt)},"text":"sometext"}"""
    encodeTopLevel(im) shouldBe expected
  }

  it should "encode UserJoinedOrLeft" in {
    val dt = ZonedDateTime.now()
    val ujol = UserJoinedOrLeft(12, "alice", true, dt)
    val expected =
      """{"msgType":"userJoinedOrLeft","seqN":12,"username":"alice","joined":true,""" +
        s""""timestamp":${encodeDateTime(dt)}}"""
    encodeTopLevel(ujol) shouldBe expected
  }

  it should "encode LatestChatLogElements" in {
    val dt = ZonedDateTime.now()
    val d = ChatLogElements(
      elements = List(
        ChatLogElement.UserJoinedOrLeft(1L, "alice", joined = true, dt),
        ChatLogElement.Message(2L, "alice", dt, "hey")
      )
    )
    val expected =
      """{"msgType":"chatLogElements","elements":[""" +
        s"""{"elementType":"userJoinedOrLeft","seqN":1,"username":"alice","joined":true,"timestamp":${encodeDateTime(dt)}},""" +
        s"""{"elementType":"message","seqN":2,"username":"alice","timestamp":${encodeDateTime(dt)},"text":"hey"}""" +
        """]}"""
    encodeTopLevel(d) shouldBe expected
  }

  it should "encode UsersInChat" in {
    val usersInChat = UsersInChat(
      List(
        UsersInChat.User("alice", true),
        UsersInChat.User("bob", false),
        UsersInChat.User("charlie", true)
      )
    )
    val expected =
      """{"msgType":"usersInChat",""" +
        """"users":[{"username":"alice","online":true},{"username":"bob","online":false},{"username":"charlie","online":true}]}"""
    encodeTopLevel(usersInChat) shouldBe expected
  }

  private def encodeDateTime(dt: ZonedDateTime): Json = {
    import io.circe.java8.time._
    import io.circe.syntax._
    dt.asJson
  }
}
