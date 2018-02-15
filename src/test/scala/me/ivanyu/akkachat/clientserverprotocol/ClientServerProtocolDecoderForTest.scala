package me.ivanyu.akkachat.clientserverprotocol

import io.circe._
import io.circe.java8.time._
import io.circe.Decoder.Result
import io.circe.generic.semiauto.deriveDecoder
import me.ivanyu.akkachat.clientserverprotocol.ClientServerProtocol.{Error, _}

object ClientServerProtocolDecoderForTest {

  private val fromServerDecoder: Decoder[FromServer] = new Decoder[FromServer] {
    private val ELEMENT_TYPE_FIELD = "elementType"

    implicit val errorDecoder: Decoder[Error] =
      deriveDecoder[Error]
    implicit val authResponseDecoder: Decoder[AuthResponse] =
      deriveDecoder[AuthResponse]

    private implicit val usersInChatUserDecoder: Decoder[UsersInChat.User] =
      deriveDecoder[UsersInChat.User]
    implicit val usersInChatDecoder: Decoder[UsersInChat] =
      deriveDecoder[UsersInChat]

    implicit val messageAckDecoder: Decoder[MessageAck] =
      deriveDecoder[MessageAck]
    implicit val serverToClientMessageDecode: Decoder[ServerToClientMessage] =
      deriveDecoder[ServerToClientMessage]
    implicit val userJoinedOrLeftDecoder: Decoder[UserJoinedOrLeft] =
      deriveDecoder[UserJoinedOrLeft]

    implicit val chatLogElementDecoder: Decoder[ChatLogElement] = new Decoder[ChatLogElement] {

      override def apply(c: HCursor): Result[ChatLogElement] = {
        implicit val userJoinedOrLeftElementDecoder: Decoder[ChatLogElement.UserJoinedOrLeft] =
          deriveDecoder[ChatLogElement.UserJoinedOrLeft]
        implicit val messageElementDecoder: Decoder[ChatLogElement.Message] =
          deriveDecoder[ChatLogElement.Message]

        c.downField(ELEMENT_TYPE_FIELD).as[String].flatMap {
          case "message" =>
            messageElementDecoder.tryDecode(c)

          case "userJoinedOrLeft" =>
            userJoinedOrLeftElementDecoder.tryDecode(c)

          case unknownMsgType: Any =>
            Left(DecodingFailure(s"""Unknown msgType "$unknownMsgType"""", c.history))
        }
      }
    }

    implicit val latestChatLogElementsDecoder: Decoder[ChatLogElements] =
      deriveDecoder[ChatLogElements]

    override def apply(c: HCursor): Decoder.Result[FromServer] = {
      c.downField(MSG_TYPE_FIELD).as[String].flatMap {
        case Error.`msgType` =>
          errorDecoder.tryDecode(c)

        case AuthResponse.`msgType` =>
          authResponseDecoder.tryDecode(c)

        case MessageAck.`msgType` =>
          messageAckDecoder.tryDecode(c)

        case ServerToClientMessage.`msgType` =>
          serverToClientMessageDecode.tryDecode(c)

        case UserJoinedOrLeft.`msgType` =>
          userJoinedOrLeftDecoder.tryDecode(c)

        case ChatLogElements.`msgType` =>
          latestChatLogElementsDecoder.tryDecode(c)

        case UsersInChat.`msgType` =>
          usersInChatDecoder.tryDecode(c)

        case unknownMsgType: Any =>
          Left(DecodingFailure(s"""Unknown msgType "$unknownMsgType"""", c.history))
      }
    }
  }

  def decodeFromServer(content: String): Either[io.circe.Error, FromServer] = {
    io.circe.parser.decode[FromServer](content)(fromServerDecoder)
  }
}
