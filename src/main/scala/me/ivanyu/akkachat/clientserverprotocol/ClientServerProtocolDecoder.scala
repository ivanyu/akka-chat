package me.ivanyu.akkachat.clientserverprotocol

import io.circe._
import io.circe.generic.semiauto.deriveDecoder

import me.ivanyu.akkachat.clientserverprotocol.ClientServerProtocol._

/**
  * Decoder for client-to-server messages.
  */
object ClientServerProtocolDecoder {
  private val fromClientDecoder: Decoder[FromClient] = new Decoder[FromClient] {
    val pongDecoder: Decoder[Pong.type] =
      deriveDecoder[Pong.type]
    val authRequestDecoder: Decoder[AuthRequest] =
      deriveDecoder[AuthRequest]
    val getUsersDecoder: Decoder[GetUsersInChat.type] =
      deriveDecoder[GetUsersInChat.type]
    val clientToServerMessageDecoder: Decoder[ClientToServerMessage] =
      deriveDecoder[ClientToServerMessage]
    val getChatLogElementsDecoder: Decoder[GetChatLogElements] =
      deriveDecoder[GetChatLogElements]

    override def apply(c: HCursor): Decoder.Result[FromClient] = {
      c.downField(MSG_TYPE_FIELD).as[String].flatMap {
        case msgType if msgType == Pong.msgType =>
          pongDecoder.tryDecode(c)

        case AuthRequest.`msgType` =>
          authRequestDecoder.tryDecode(c)

        case msgType if msgType == GetUsersInChat.msgType =>
          getUsersDecoder.tryDecode(c)

        case ClientToServerMessage.`msgType` =>
          clientToServerMessageDecoder.tryDecode(c)

        case GetChatLogElements.`msgType` =>
          getChatLogElementsDecoder.tryDecode(c)

        case unknownMsgType =>
          Left(DecodingFailure(s"""Unknown msgType "$unknownMsgType"""", c.history))
      }
    }
  }

  def decodeFromClient(content: String): Either[io.circe.Error, FromClient] = {
    io.circe.parser.decode[FromClient](content)(fromClientDecoder)
  }
}
