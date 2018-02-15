package me.ivanyu.akkachat.clientserverprotocol

import java.time.ZonedDateTime

sealed trait ClientServerProtocol {
  def msgType: String
}

// scalastyle:off number.of.types
object ClientServerProtocol {

  type MessageId = String

  private[clientserverprotocol] val MSG_TYPE_FIELD = "msgType"

  /** A message that come from clients. */
  sealed trait FromClient extends ClientServerProtocol

  /** A message that come from the server. */
  sealed trait FromServer extends ClientServerProtocol


  ////
  // From a client to the server.
  ////

  /** A pong reply from a client to the server's [[Ping]]. */
  case object Pong extends FromClient {
    override def msgType: String = "pong"
  }

  /**
    * An authentication request from a client to the server.
    */
  final case class AuthRequest(username: String, password: String) extends FromClient {
    override def msgType: String = AuthRequest.msgType
  }

  object AuthRequest {
    val msgType = "authRequest"
  }

  /** A request for users in the chat. */
  case object GetUsersInChat extends FromClient {
    override def msgType: String = "getUsersInChat"
  }

  /**
    * A message from a client to the chat.
    *
    * @param clientSideId the client-side ID.
    * @param text the text of the message.
    */
  final case class ClientToServerMessage(
    clientSideId: MessageId,
    text: String) extends FromClient {

    override def msgType: String = ClientToServerMessage.msgType
  }

  object ClientToServerMessage {
    val msgType = "clientToServerMessage"
  }

  /**
    * A request for elements of the chat log.
    *
    * @param before optional sequence number from before which elements are requested.
    */
  final case class GetChatLogElements(before: Option[Long]) extends FromClient {
    override def msgType: String = GetChatLogElements.msgType
  }

  object GetChatLogElements {
    val msgType = "getChatLogElements"
  }


  ////
  // From the server to a client.
  ////

  /** A ping from the server to a client. */
  case object Ping extends FromServer {
    override def msgType: String = "ping"
  }

  /**
    * An error sent from the server to a client.
    *
    * Rationale: Akka HTTP gives no better way to communicate
    * errors to WS clients.
    */
  final case class Error(reason: String) extends FromServer {
    override def msgType: String = Error.msgType
  }

  object Error {
    val msgType = "error"
  }

  /**
    * A response from the server for authentication request sent by a client [[AuthRequest]].
    */
  final case class AuthResponse(success: Boolean) extends FromServer {
    override def msgType: String = AuthResponse.msgType
  }

  object AuthResponse {
    val msgType = "authResponse"
  }

  /** Users in the chat (response to [[GetUsersInChat]]). */
  final case class UsersInChat(users: Seq[UsersInChat.User]) extends FromServer {
    override def msgType: String = UsersInChat.msgType
  }

  object UsersInChat {
    val msgType = "usersInChat"

    final case class User(username: String, online: Boolean)
  }

  /**
    * An acknowledgement for a client message.
    *
    * @param clientSideId the client-side ID.
    * @param seqN the sequence number in the chat log.
    * @param timestamp the timestamp in the chat log.
    */
  final case class MessageAck(
    clientSideId: MessageId,
    seqN: Long,
    timestamp: ZonedDateTime) extends FromServer {

    override def msgType: String = MessageAck.msgType
  }

  object MessageAck {
    val msgType = "messageAck"
  }

  /**
    * An indication for a client that another client has joined or left the chat.
    *
    * @param seqN the sequence number in the chat log.
    * @param username the username of the user who has joined or left.
    * @param joined `true` if the user has joined; `false` otherwise.
    * @param timestamp the timestamp in the chat log.
    */
  final case class UserJoinedOrLeft(
    seqN: Long,
    username: String,
    joined: Boolean,
    timestamp: ZonedDateTime) extends FromServer {

    override def msgType: String = UserJoinedOrLeft.msgType
  }

  object UserJoinedOrLeft {
    val msgType = "userJoinedOrLeft"
  }

  /**
    * Used for distribution of messages from the server to client.
    *
    * Sent out to other users when some user has sent a message.
    *
    * @param seqN the sequence number in the chat log.
    * @param username the usernmame of the user who has sent a message.
    * @param timestamp the timestamp in the chat log.
    * @param text the text of the message.
    */
  final case class ServerToClientMessage(
    seqN: Long,
    username: String,
    timestamp: ZonedDateTime,
    text: String) extends FromServer {

    override def msgType: String = ServerToClientMessage.msgType
  }

  object ServerToClientMessage {
    val msgType = "serverToClientMessage"
  }


  /** An element of the chat log. */
  sealed trait ChatLogElement {
    /** The sequence number with which this element was accepted to the log. */
    val seqN: Long

    /** The date and time when this element was accepted to the log. */
    val timestamp: ZonedDateTime
  }

  object ChatLogElement {
    final case class Message(seqN: Long,
      username: String,
      timestamp: ZonedDateTime,
      text: String) extends ChatLogElement

    final case class UserJoinedOrLeft(
      seqN: Long,
      username: String,
      joined: Boolean,
      timestamp: ZonedDateTime) extends ChatLogElement
  }

  /**
    * A response for a user's request of latest chat log elements,
    * [[GetChatLogElements]].
    */
  final case class ChatLogElements(
    elements: Seq[ChatLogElement]) extends FromServer {

    override def msgType: String = ChatLogElements.msgType
  }

  object ChatLogElements {
    val msgType = "chatLogElements"
  }

}
// scalastyle:off number.of.types
