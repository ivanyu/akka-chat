package me.ivanyu.akkachat.sessions

import java.time.ZonedDateTime

import akka.Done
import akka.actor.{Actor, ActorLogging, ActorRef, Props, Stash, Status}

import me.ivanyu.akkachat.AppConfig
import me.ivanyu.akkachat.chat.ChatActorProtocol
import me.ivanyu.akkachat.clientserverprotocol.ClientServerProtocol._
import me.ivanyu.akkachat.sessions.SessionManagerProtocol.SessionStreamComplete

private class SessionActor(config: AppConfig, protected val chatActor: ActorRef)
    extends Actor
    with Stash
    with ActorLogging
    with SessionInitialization
    with SessionWaitingForAuthentication
    with SessionWaitingForAttaching
    with NormalOperation {

  private val sessionStarted = ZonedDateTime.now()

  override def receive: Receive = initialization(outActorOpt = None, streamActorOpt = None)
}

/**
  * Contains behavior that starts after all preparations.
  */
private trait NormalOperation extends SessionBase {
  _: Actor with ActorLogging =>

  protected def normalOperation(
      username: String,
      streamActor: ActorRef,
      outActor: ActorRef,
      userActor: ActorRef
  ): Receive =
    streamProtocol orElse {
      ////
      // Messages from the client side
      ////
      case ToSessionStreamElement.FromClientWrapper(GetUsersInChat) =>
        log.debug("Received GetUsersInChat from {}", username)
        userActor ! GetUsersInChat

      case ToSessionStreamElement.FromClientWrapper(om: ClientToServerMessage) =>
        log.debug("Message from {}: {}", username, om)
        userActor ! om

      case ToSessionStreamElement.FromClientWrapper(getChatLogElements: GetChatLogElements) =>
        log.debug("Received {} from {}", getChatLogElements, username)
        userActor ! getChatLogElements

      ////
      // Messages from the chat side
      ////
      case usersInChat: UsersInChat =>
        acknowledgeStreamMessage(streamActor)
        sendNormalMessageToClient(outActor, usersInChat)

      case messageAck: MessageAck =>
        acknowledgeStreamMessage(streamActor)
        sendNormalMessageToClient(outActor, messageAck)

      case latestMessages: ChatLogElements =>
        acknowledgeStreamMessage(streamActor)
        sendNormalMessageToClient(outActor, latestMessages)

      case userJoinedOrLeft: UserJoinedOrLeft =>
        sendNormalMessageToClient(outActor, userJoinedOrLeft)

      case serverToClientMessage: ServerToClientMessage =>
        sendNormalMessageToClient(outActor, serverToClientMessage)

      case other: Any =>
        log.warning("Received {} from client, protocol violation. Stopping", other)
        sendErrorToClient(outActor, "Protocol violation", close = true)
    }
}

/**
  * Contains behavior of waiting for this session to be attached
  * to the user actor.
  */
private trait SessionWaitingForAttaching extends SessionBase {
  _: Actor with Stash with NormalOperation with ActorLogging =>

  protected def waitingForAttaching(username: String, streamActor: ActorRef, outActor: ActorRef): Receive =
    streamProtocol orElse {
      case ChatActorProtocol.SessionAttached(userActor) =>
        log.debug("Attached to \"{}\"", username)
        context.become(normalOperation(username, streamActor, outActor, userActor))
        unstashAll()

      case _ =>
        stash()
    }
}

/**
  * Contains behavior of waiting for the client to be authenticated,
  * i.e., [[ToSessionStreamElement.Authenticated]] that comes by the incoming stream.
  */
private trait SessionWaitingForAuthentication extends SessionBase {
  _: Actor with SessionWaitingForAttaching with ActorLogging =>
  protected val chatActor: ActorRef

  protected def waitingForAuthentication(streamActor: ActorRef, outActor: ActorRef): Receive =
    streamProtocol orElse {
      case ToSessionStreamElement.Authenticated(username) =>
        chatActor ! ChatActorProtocol.AttachSession(username, self)
        acknowledgeStreamMessage(streamActor)
        context.become(waitingForAttaching(username, streamActor, outActor))

      case other: Any =>
        log.warning("Received {} from client, Authentication request expected. Stopping", other)
        sendErrorToClient(outActor, "Protocol violation: Authentication request is expected", close = true)
    }
}

/**
  * Contains behavior of initialization:
  * waiting for the registration of the out actor
  * and [[me.ivanyu.akkachat.sessions.SessionManagerProtocol.SessionStreamInit]] from the stream actor.
  */
private trait SessionInitialization extends SessionBase {
  _: Actor with ActorLogging with SessionWaitingForAuthentication =>

  import SessionManagerProtocol._

  /**
    * Wait for both [[RegisterOutActor]] and [[SessionStreamInit]].
    * Proceed when both have arrived exactly once.
    */
  protected def initialization(outActorOpt: Option[ActorRef], streamActorOpt: Option[ActorRef]): Receive =
    streamProtocol orElse {

      case RegisterOutActor(outActor) =>
        assert(outActorOpt.isEmpty)

        // if SessionStreamInit has been received already
        streamActorOpt match {
          case Some(initSender) =>
            finishInitialization(initSender, outActor)

          case None =>
            context.become(
              initialization(Some(outActor), streamActorOpt)
            )
        }

      case SessionStreamInit =>
        assert(streamActorOpt.isEmpty)

        // if RegisterOutActor has been received already
        outActorOpt match {
          case Some(outActor) =>
            finishInitialization(sender(), outActor)

          case None =>
            context.become(
              initialization(outActorOpt, streamActorOpt = Some(sender()))
            )
        }
    }

  private def finishInitialization(streamActor: ActorRef, outActor: ActorRef): Unit = {
    acknowledgeStreamMessage(streamActor) // stream: acknowledge initialization
    log.debug("Finish initialization, waiting for authentication request")
    context.become(waitingForAuthentication(streamActor, outActor))
  }
}

/**
  * Contains basic logic to the session actor.
  */
private trait SessionBase {
  _: Actor with ActorLogging =>

  /**
    * Stream source protocol.
    */
  val streamProtocol: Receive = {
    case Status.Failure(t) =>
      log.info("Session failed with {}", t)
      context.stop(self)

    case SessionStreamComplete =>
      log.debug("Session completed")
      context.stop(self)
  }

  protected def sendNormalMessageToClient(outActor: ActorRef, message: FromServer): Unit = {

    outActor ! message
  }

  protected def sendErrorToClient(outActor: ActorRef, error: String, close: Boolean): Unit = {

    outActor ! Error(error)
    if (close) {
      sendClose(outActor)
    }
  }

  protected def sendClose(outActor: ActorRef): Unit = {
    outActor ! akka.actor.Status.Success(Done)
  }

  protected def acknowledgeStreamMessage(streamActor: ActorRef): Unit = {
    streamActor ! SessionManagerProtocol.SessionStreamAck
  }
}

private object SessionActor {
  def props(config: AppConfig, chatActor: ActorRef): Props = {
    Props(classOf[SessionActor], config, chatActor)
  }
}
