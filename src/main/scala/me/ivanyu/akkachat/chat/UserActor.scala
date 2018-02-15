package me.ivanyu.akkachat.chat

import java.time.{Duration, ZonedDateTime}

import scala.collection.immutable.Queue
import scala.concurrent.duration._

import akka.actor.{Actor, ActorLogging, ActorRef, Props, Terminated}

import me.ivanyu.akkachat.AppConfig
import me.ivanyu.akkachat.clientserverprotocol.ClientServerProtocol._

/**
  * Represents a user connected to the chat.
  * A child of [[ChatActor]].
  * User sessions are attached to it.
  */
private class UserActor(config: AppConfig, username: String) extends Actor with ActorLogging {

  import ChatActorProtocol._
  import UserActor._

  import context.dispatcher

  private val attachedSessions = new AttachedSessionHolder

  private val recentlyAcceptedMessages = new RecentlyAcceptedMessagesHolder(
    config.Chat.MaxDurationOfRecentlyAcceptedMessages)

  /**
    * Messages that wait to be accepted by the chat actor.
    * MessageId -> set of sessions from which this message came.
    * Must be a set of sessions to handle client reconnects.
    */
  private var messagesWaitingToBeAccepted: Map[MessageId, Set[ActorRef]] = Map.empty

  override def preStart(): Unit = {
    val interval = 5.minutes
    context.system.scheduler.schedule(interval, interval, self, CleanOldAcceptedMessagesTick)
    ()
  }

  override def receive: Receive = {
    ////
    // From sessions
    ////
    case GetUsersInChat =>
      context.parent.forward(GetUsersInChat)

    case ClientToServerMessage(id, text) =>
      handleClientToServerMessage(id, text)

    case getChatLogElements: GetChatLogElements =>
      context.parent.forward(getChatLogElements)

    case Terminated(sessionActor) =>
      handleSessionTerminated(sessionActor)

    ////
    // From the chat actor
    ////
    case AttachSession(_, sessionActor) =>
      handleAttachSession(sessionActor)

    case userJoinedOrLeft: UserJoinedOrLeft =>
      attachedSessions.sendToAll(userJoinedOrLeft)
//      attachedSessions.foreach(_ ! userJoinedOrLeft)

    case MessageFromUserAccepted(author, id, text, seqN, timestamp) =>
      handleMessageFromUserAccepted(author, id, text, seqN, timestamp)

    ////
    // From self
    ////
    case CleanOldAcceptedMessagesTick =>
      recentlyAcceptedMessages.deleteOld()
  }

  private def handleClientToServerMessage(id: MessageId, text: String): Unit = {
    recentlyAcceptedMessages.get(id) match {
      case None =>
        if (!messagesWaitingToBeAccepted.contains(id)) {
          context.parent ! ChatActorProtocol.MessageFromUser(username, id, text)
        } else {
          // Do nothing, the chat has been already notified about this message.
        }

        messagesWaitingToBeAccepted +=
          id -> (messagesWaitingToBeAccepted.getOrElse(id, Set.empty) + sender())

      case Some(messageAck) =>
        sender() ! messageAck
    }
  }

  private def handleSessionTerminated(sessionActor: ActorRef): Unit = {
    attachedSessions.detach(sessionActor)

    if (attachedSessions.isEmpty) {
      log.debug("Last session for \"{}\" closed", username)
      context.parent ! ChatActorProtocol.LastSessionClosed(username)
    }
  }

  private def handleAttachSession(sessionActor: ActorRef): Unit = {
    context.watch(sessionActor)

    val isFirstSession = attachedSessions.isEmpty

    attachedSessions.attach(sessionActor)
    sessionActor ! ChatActorProtocol.SessionAttached(self)

    if (isFirstSession) {
      log.debug("First session for \"{}\" opened", username)
      context.parent ! ChatActorProtocol.FirstSessionOpened(username)
    }
  }

  private def handleMessageFromUserAccepted(
      author: String,
      id: String,
      text: String,
      seqN: Long,
      timestamp: ZonedDateTime
  ): Unit = {

    // Don't send ServerToClientMessage to the sessions from which this message came.
    var messageOriginSessions: Set[ActorRef] = Set.empty

    if (username == author) {
      messagesWaitingToBeAccepted.get(id) match {
        case Some(sessions) =>
          messagesWaitingToBeAccepted -= id
          messageOriginSessions = sessions

        case None =>
          log.warning("Received MessageFromUserAccepted for id = {}, " +
                        "but message not registered in waiting for acceptation",
                      id)
      }
    }

    val messageAck = MessageAck(id, seqN, timestamp)

    recentlyAcceptedMessages.add(id, messageAck)

    // Send MessageAck to the session from which the message came.
    messageOriginSessions.foreach { session =>
      session ! messageAck
    }

    val serverToClientMessage = ServerToClientMessage(seqN, author, timestamp, text)

    attachedSessions.sendToAllButSome(serverToClientMessage, messageOriginSessions)
  }
}

private object UserActor {

  private case object CleanOldAcceptedMessagesTick

  def props(config: AppConfig, username: String): Props = {
    Props(classOf[UserActor], config, username)
  }

}
