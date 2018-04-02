package me.ivanyu.akkachat.clientserverprotocol

import io.circe.syntax._
import io.circe.java8.time._
import io.circe.{Encoder, Json, JsonObject}
import io.circe.generic.semiauto.deriveEncoder
import io.circe.Json.JString

import me.ivanyu.akkachat.clientserverprotocol.ClientServerProtocol._

/**
  * Encoder for server-to-client messages.
  */
object ClientServerProtocolEncoder {
  private implicit val pingEncoder: Encoder[Ping.type] =
    deriveEncoder[Ping.type]
  private implicit val errorEncoder: Encoder[Error] =
    deriveEncoder[Error]
  private implicit val authResponseEncoder: Encoder[AuthResponse] =
    deriveEncoder[AuthResponse]

  private implicit val usersInChatUserEncoder: Encoder[UsersInChat.User] =
    deriveEncoder[UsersInChat.User]
  private implicit val usersInChatEncoder: Encoder[UsersInChat] =
    deriveEncoder[UsersInChat]

  private implicit val userJoinedOrLeftEncoder: Encoder[UserJoinedOrLeft] =
    deriveEncoder[UserJoinedOrLeft]
  private implicit val messageAckEncoder: Encoder[MessageAck] =
    deriveEncoder[MessageAck]
  private implicit val serverToClientMessageEncoder: Encoder[ServerToClientMessage] =
    deriveEncoder[ServerToClientMessage]

  private implicit val chatLogElementEncoder: Encoder[ChatLogElement] =
    new Encoder[ChatLogElement] {
      private val ELEMENT_TYPE_FIELD = "elementType"

      private implicit val userJoinedOrLeftElementEncoder: Encoder[ChatLogElement.UserJoinedOrLeft] =
        deriveEncoder[ChatLogElement.UserJoinedOrLeft]
      private implicit val messageElementEncoder: Encoder[ChatLogElement.Message] =
        deriveEncoder[ChatLogElement.Message]

      override def apply(chatLogElement: ChatLogElement): Json = chatLogElement match {
        case x: ChatLogElement.UserJoinedOrLeft =>
          x.asJson.mapObject { jsonObject =>
            (ELEMENT_TYPE_FIELD -> Json.fromString("userJoinedOrLeft")) +: jsonObject
          }

        case x: ChatLogElement.Message =>
          x.asJson.mapObject { jsonObject =>
            (ELEMENT_TYPE_FIELD -> Json.fromString("message")) +: jsonObject
          }
      }
    }

  private implicit val latestChatLogElementsEncoder: Encoder[ChatLogElements] =
    deriveEncoder[ChatLogElements]

  private def encodeTopLevelJson(fromServer: FromServer): Json = {
    val result = fromServer match {
      case Ping =>
        Ping.asJson
      case x: Error =>
        x.asJson
      case x: AuthResponse =>
        x.asJson
      case x: UsersInChat =>
        x.asJson
      case x: UserJoinedOrLeft =>
        x.asJson
      case x: MessageAck =>
        x.asJson
      case x: ServerToClientMessage =>
        x.asJson
      case x: ChatLogElements =>
        x.asJson
    }

    result.mapObject { json: JsonObject =>
      (MSG_TYPE_FIELD, Json.fromString(fromServer.msgType)) +: json
    }
  }

  def encodeTopLevel(fromServer: FromServer): String = {
    encodeTopLevelJson(fromServer).noSpaces
  }
}
