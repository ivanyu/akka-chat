package me.ivanyu.akkachat.chat

import java.time.ZonedDateTime

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.pattern.{ask, pipe}
import akka.persistence.{PersistentActor, RecoveryCompleted, SaveSnapshotFailure, SaveSnapshotSuccess, SnapshotOffer}
import akka.util.{Helpers, Timeout}

import me.ivanyu.akkachat.AppConfig
import me.ivanyu.akkachat.clientserverprotocol.ClientServerProtocol._
import me.ivanyu.akkachat.util.{SequentialCounter, WithReplyTo}

/**
  * Owns the chat log, persists it; holds user actors, attaches sessions to user actors.
  */
class ChatActor(config: AppConfig) extends Actor with PersistentActor with ActorLogging {

  // TODO shutdown inactive user actors

  import ChatLog._
  import ChatActorProtocol._

  override val persistenceId: String = "chat"

  /** The number of the chat log elements to return when requested. */
  private val requestLogElementCount = config.Chat.RequestLogElementCount

  private val userManager = context.actorOf(UserManager.props(config), "user-manager")

  private val userActorHolder = new UserActorHolder

  private var chatLog = new ChatLog(config.Chat.MaxChatLogLengthInMemory)

  override def receiveRecover: Receive = {
    case logEntry: LogEntry =>
      chatLog.append(lastSequenceNr, logEntry, pruneOld = false)

    case SnapshotOffer(_, snapshot) =>
      chatLog = snapshot.asInstanceOf[ChatLog]

    case RecoveryCompleted =>
      chatLog.prune()
      log.debug("Recovery completed")
  }

  // scalastyle:off cyclomatic.complexity
  override def receiveCommand: Receive = {
    case auth: Authenticate =>
      handleAuthenticate(auth)

    case attachSession: AttachSession =>
      handleAttachSession(attachSession)

    case FirstSessionOpened(username) =>
      handleFirstSessionOpened(username)

    case MessageFromUser(username, id, text) =>
      handleMessageFromUser(username, id, text)

    case LastSessionClosed(username) =>
      handleLastSessionClosed(username)

    case GetUsersInChat =>
      handleGetUsersInChat()

    case GetChatLogElements(beforeSeqN) =>
      handleGetLastChatLogElements(beforeSeqN)

    ////
    // User manager
    ////
    case WithReplyTo(UserManagerProtocol.AllUsers(usernames), replyTo) =>
      handleAllUsersReply(usernames, replyTo)

    ////
    // Snapshotting events
    ////
    case _: SaveSnapshotSuccess =>
      log.debug("Save snapshot success")

    case SaveSnapshotFailure(_, cause) =>
      log.warning("Save snapshot failure: {}", cause)
  }
  // scalastyle:on cyclomatic.complexity

  private def handleAuthenticate(auth: Authenticate): Unit = {
    userManager.forward(auth)
  }

  private def handleAttachSession(attachSession: AttachSession): Unit = {
    userActorHolder.getOrCreateUserActor(attachSession.username) ! attachSession
  }

  private def handleFirstSessionOpened(username: String): Unit = {
    val userJoinedAt = ZonedDateTime.now()

    val userJoinedLog = UserJoinedOrLeftLog(username, joined = true, userJoinedAt)
    persist(userJoinedLog) { _ =>
      chatLog.append(lastSequenceNr, userJoinedLog, pruneOld = true)

      saveSnapshotIfNeeded()

      userActorHolder.dispatchToUsers(
        UserJoinedOrLeft(lastSequenceNr, username, joined = true, userJoinedAt)
      )
    }
    ()
  }

  private def handleMessageFromUser(username: String, id: String, text: String): Unit = {
    val messageAcceptedAt = ZonedDateTime.now()

    val userMessageLog = UserMessageLog(username, messageAcceptedAt, text)
    persist(userMessageLog) { _ =>
      chatLog.append(lastSequenceNr, userMessageLog, pruneOld = true)

      saveSnapshotIfNeeded()

      userActorHolder.dispatchToUsers(
        MessageFromUserAccepted(username, id, text, lastSequenceNr, messageAcceptedAt)
      )
    }
  }

  private def handleLastSessionClosed(username: String): Unit = {
    val userLeftAt = ZonedDateTime.now()

    val userLeftLog = UserJoinedOrLeftLog(username, joined = false, userLeftAt)
    persist(userLeftLog) { _ =>
      chatLog.append(lastSequenceNr, userLeftLog, pruneOld = true)

      saveSnapshotIfNeeded()

      userActorHolder.dispatchToUsers(
        UserJoinedOrLeft(lastSequenceNr, username, joined = false, userLeftAt)
      )
      userActorHolder.shutdownUserActor(username)
    }
    ()
  }

  /**
    * @note flow continues in [[handleAllUsersReply]]
    */
  private def handleGetUsersInChat(): Unit = {
    implicit val timeout: Timeout = config.AskTimeout
    import this.context.dispatcher

    val originalSender = sender()
    (userManager ? UserManagerProtocol.GetAllUsers)
      .mapTo[UserManagerProtocol.AllUsers]
      .map(x => WithReplyTo(x, originalSender))
      .pipeTo(self)
    ()
  }

  private def handleAllUsersReply(usernames: List[String], replyTo: ActorRef): Unit = {
    val usersList = usernames.sorted.map { username =>
      val online = userActorHolder.userExists(username)
      UsersInChat.User(username, online)
    }
    replyTo ! UsersInChat(usersList)
  }

  private def handleGetLastChatLogElements(beforeSeqN: Option[Long]): Unit = {
    val elements = chatLog.takeEntries(beforeSeqN, requestLogElementCount).reverse.map {
      case (seqN, UserJoinedOrLeftLog(username, joined, timestamp)) =>
        ChatLogElement.UserJoinedOrLeft(seqN, username, joined, timestamp)

      case (seqN, UserMessageLog(username, timestamp, text)) =>
        ChatLogElement.Message(seqN, username, timestamp, text)
    }
    sender() ! ChatLogElements(elements)
  }

  private def saveSnapshotIfNeeded(): Unit = {
    val needToSaveSnapshot =
      lastSequenceNr != 0 && lastSequenceNr % config.Chat.SnapshotInterval == 0
    if (needToSaveSnapshot) {
      saveSnapshot(chatLog)
    }
  }

  private class UserActorHolder {

    /** The map between usernames and users sessions. */
    private var users: Map[String, ActorRef] = Map.empty

    /** The increasing counter of users. */
    private val userCnt = new SequentialCounter

    def userExists(username: String): Boolean = {
      users.contains(username)
    }

    def getOrCreateUserActor(username: String): ActorRef = {
      if (!users.contains(username)) {
        users += username -> context.actorOf(
          UserActor.props(config, username),
          username + "-" + Helpers.base64(userCnt.next())
        )
      }
      users(username)
    }

    /**
      * Sends a message to all user actors.
      */
    def dispatchToUsers(message: AnyRef): Unit = {
      users.values.foreach { userActor =>
        userActor ! message
      }
    }

    def shutdownUserActor(username: String): Unit = {
      context.stop(users(username))
      users -= username
    }
  }
}

object ChatActor {
  def props(config: AppConfig): Props = {
    Props(classOf[ChatActor], config)
  }
}

object ChatActorProtocol {

  ////
  // Public
  ////

  final case class Authenticate(username: String, password: String)

  final case class AuthenticateResult(success: Boolean)

  /**
    * Attach [[session]] to the user identified by [[username]].
    */
  final case class AttachSession(username: String, session: ActorRef)

  /**
    * A session attaching result.
    */
  final case class SessionAttached(userActor: ActorRef)

  ////
  // Private for Chat subsystem
  ////

  /**
    * The first session for the user identified by [[username]] has been opened.
    */
  private[chat] final case class FirstSessionOpened(username: String)

  /**
    * The last session for the user identified by [[username]] has been closed.
    */
  private[chat] final case class LastSessionClosed(username: String)

  /**
    * Incoming message from a user.
    *
    * @param username the username.
    * @param id the client-side ID of the message.
    * @param text the text of the message.
    */
  private[chat] final case class MessageFromUser(
      username: String,
      id: String,
      text: String
  )

  /**
    * Incoming message from a user.
    *
    * @param username the username.
    * @param id the client-side ID of the message.
    * @param text the text of the message.
    * @param seqN the sequence number in the chat log with which it was accepted.
    * @param timestamp the timestamp in the chat log.
    */
  private[chat] final case class MessageFromUserAccepted(
      username: String,
      id: String,
      text: String,
      seqN: Long,
      timestamp: ZonedDateTime
  )

}
