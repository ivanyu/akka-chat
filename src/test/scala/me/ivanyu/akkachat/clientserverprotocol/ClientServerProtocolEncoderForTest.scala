package me.ivanyu.akkachat.clientserverprotocol

import io.circe.syntax._
import io.circe.java8.time._
import io.circe.generic.semiauto.deriveEncoder
import io.circe.{Encoder, Json, JsonObject}
import me.ivanyu.akkachat.clientserverprotocol.ClientServerProtocol._

object ClientServerProtocolEncoderForTest {

  private implicit val pongEncoder: Encoder[Pong.type] =
    deriveEncoder[Pong.type]
  private implicit val authRequestEncoder: Encoder[AuthRequest] =
    deriveEncoder[AuthRequest]
  private implicit val getUsersEncoder: Encoder[GetUsersInChat.type] =
    deriveEncoder[GetUsersInChat.type]
  private implicit val clientToServerMessageEncoder: Encoder[ClientToServerMessage] =
    deriveEncoder[ClientToServerMessage]
  private implicit val getChatLogElementsEncoder: Encoder[GetChatLogElements] =
    deriveEncoder[GetChatLogElements]

  def encodeTopLevel(fromClient: FromClient): String = {
    val result = fromClient match {
      case Pong => Pong.asJson
      case x: AuthRequest => x.asJson
      case GetUsersInChat => GetUsersInChat.asJson
      case x: ClientToServerMessage => x.asJson
      case x: GetChatLogElements => x.asJson
    }

    result.mapObject { json: JsonObject =>
      (MSG_TYPE_FIELD, Json.fromString(fromClient.msgType)) +: json
    }
  }.noSpaces
}
