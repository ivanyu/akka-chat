package me.ivanyu.akkachat.clientserverprotocol

import org.scalatest.{FlatSpec, Matchers}

import me.ivanyu.akkachat.clientserverprotocol.ClientServerProtocol._
import me.ivanyu.akkachat.clientserverprotocol.ClientServerProtocolDecoder.decodeFromClient

class ClientServerProtocolDecoderTest extends FlatSpec with Matchers {

  "ClientServerProtocolDecoder" should "not decode when unknown msgType" in {
    val jsonStr =
      """
        |{
        |  "msgType": "unknown"
        |}""".stripMargin

    decodeFromClient(jsonStr) should be('left)
  }

  it should "decode correct AuthRequest" in {
    val jsonStr =
      """
        |{
        |  "msgType": "authRequest",
        |  "username": "alice",
        |  "password": "alice123"
        |}""".stripMargin

    decodeFromClient(jsonStr).right.get shouldBe AuthRequest("alice", "alice123")
  }

  it should "not decode incorrect AuthRequest 1" in {
    val jsonStr =
      """
        |{
        |  "msgType": "authRequest",
        |  "username": "alice"
        |}""".stripMargin

    decodeFromClient(jsonStr) should be('left)
  }

  it should "not decode incorrect AuthRequest 2" in {
    val jsonStr =
      """
        |{
        |  "msgType": "authRequest",
        |  "password": "alice123"
        |}""".stripMargin

    decodeFromClient(jsonStr) should be('left)
  }

  it should "not decode incorrect AuthRequest 3" in {
    val jsonStr =
      """
        |{
        |  "msgType": "authRequest",
        |  "username": 12,
        |  "password": "alice123",
        |}""".stripMargin

    decodeFromClient(jsonStr) should be('left)
  }

  it should "decode correct ClientToServerMessage" in {
    val text = "hello world"
    val jsonStr =
      s"""
         |{
         |  "msgType": "clientToServerMessage",
         |  "clientSideId": "42",
         |  "text": "$text"
         |}""".stripMargin

    decodeFromClient(jsonStr).right.get shouldBe ClientToServerMessage("42", "hello world")
  }

  it should "decode correct GetChatLogElements" in {
    val jsonStr =
      s"""
         |{
         |  "msgType": "getChatLogElements"
         |}""".stripMargin

    decodeFromClient(jsonStr).right.get shouldBe GetChatLogElements(None)
  }

  it should "decode correct Pong" in {
    val jsonStr =
      s"""
         |{
         |  "msgType": "pong"
         |}""".stripMargin

    decodeFromClient(jsonStr).right.get shouldBe Pong
  }

  it should "decode correct GetUsersInChat" in {
    val jsonStr =
      s"""
         |{
         |  "msgType": "getUsersInChat"
         |}""".stripMargin

    decodeFromClient(jsonStr).right.get shouldBe GetUsersInChat
  }
}
