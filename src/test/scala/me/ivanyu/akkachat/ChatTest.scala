package me.ivanyu.akkachat

import akka.http.scaladsl.model.ws.TextMessage
import akka.http.scaladsl.testkit.{ScalatestRouteTest, WSProbe}
import org.scalatest.{Matchers, Suite}
import me.ivanyu.akkachat.clientserverprotocol.ClientServerProtocol._
import me.ivanyu.akkachat.clientserverprotocol.ClientServerProtocolDecoderForTest.decodeFromServer
import me.ivanyu.akkachat.clientserverprotocol.ClientServerProtocolEncoderForTest.encodeTopLevel
import me.ivanyu.akkachat.chat.ChatSubsystem
import me.ivanyu.akkachat.http.HttpSubsystem
import me.ivanyu.akkachat.sessions.SessionsSubsystem

import scala.reflect.ClassTag

trait ChatTest extends ScalatestRouteTest with Matchers {
  _: Suite =>

  protected def config: AppConfig = AppConfig()

  val chatSubsystem = new ChatSubsystem(config)
  val sessionsSubsystem = new SessionsSubsystem(config, chatSubsystem)
  val httpSubsystem = new HttpSubsystem(config, sessionsSubsystem)

  protected implicit class ChatProbe(wsProbe: WSProbe) {
    def sendMessage(fromClient: FromClient): Unit = {
      wsProbe.sendMessage(encodeTopLevel(fromClient))
    }

    def expectServerMessage(expected: FromServer): Unit = {
      val text = wsProbe.expectMessage().asInstanceOf[TextMessage.Strict].text
      decodeFromServer(text) shouldBe Right(expected)
      ()
    }

    def expectServerMessage[A <: FromServer: ClassTag](): A = {
      val text = wsProbe.expectMessage().asInstanceOf[TextMessage.Strict].text
      val decoded = decodeFromServer(text)
      decoded shouldBe a[Right[_, _]]
      decoded.right.get shouldBe a[A]
      decoded.right.get.asInstanceOf[A]
    }
  }

  protected def connect(wsProbe: WSProbe): Unit = {
    WS("/websocket", wsProbe.flow) ~> httpSubsystem.routes ~> check {
      isWebSocketUpgrade shouldEqual true
    }
    ()
  }
}
