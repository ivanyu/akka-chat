package me.ivanyu.akkachat.sessions

import scala.collection.mutable
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.util.{Failure, Success, Try}

import akka.actor.ActorRef
import akka.pattern.ask
import akka.stream.{Attributes, BidiShape, Inlet, Outlet}
import akka.stream.stage._
import akka.util.Timeout

import me.ivanyu.akkachat.AppConfig
import me.ivanyu.akkachat.clientserverprotocol.ClientServerProtocol._
import me.ivanyu.akkachat.chat.ChatActorProtocol._

/**
  * The graph stage for user authentication.
  *
  * Waits for [[AuthRequest]] from the user, does authentication with [[chatActor]].
  * If the authentication is successful:
  *  - replies with the positive [[AuthResponse]] to the client;
  *  - sends [[ToSessionStreamElement.Authenticated]] event to the session;
  *  - starts passing messages through itself freely.
  * If the authentication is successful:
  *  - replies with the negative [[AuthResponse]] to the client;
  *  - closes the connection.
  * It also closes the connection in case of a protocol violation
  * during the authentication phase.
  */
private class AuthenticationStage(config: AppConfig, chatActor: ActorRef)
    extends GraphStage[BidiShape[ToSessionStreamElement, ToSessionStreamElement, FromServer, FromServer]] {

  import AuthenticationStage._

  val inFromClient: Inlet[ToSessionStreamElement] = Inlet("InFromClient")
  val outToServer: Outlet[ToSessionStreamElement] = Outlet("OutToServer")

  val inFromServer: Inlet[FromServer] = Inlet("InFromServer")
  val outToClient: Outlet[FromServer] = Outlet("OutToClient")

  override val shape: BidiShape[ToSessionStreamElement, ToSessionStreamElement, FromServer, FromServer] =
    BidiShape(inFromClient, outToServer, inFromServer, outToClient)

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic = new StageLogic

  /**
    * The stage logic operates in three phases:
    *  1) initial;
    *  2) authentication-in-progress;
    *  3) authenticated.
    *
    * Transition 1->2 happens on [[AuthRequest]].
    * Transition 2->3 happens on [[chatActor]] response (if positive).
    */
  private class StageLogic extends GraphStageLogic(shape) with StageLogging {
    graphStage =>

    private var authCallback: AsyncCallback[Try[(Boolean, String)]] = _

    private val toServerQueue = new mutable.Queue[ToSessionStreamElement]
    private val toClientQueue = new mutable.Queue[FromServer]

    override def preStart(): Unit = {
      super.preStart()

      pull(inFromClient)

      authCallback = getAsyncCallback[Try[(Boolean, String)]] {
        case Success((true, username)) =>
          log.debug("Authentication successful: {}", username)

          goToAuthenticatedState()

          pull(inFromServer)

          val authenticatedMsg = ToSessionStreamElement.Authenticated(username)
          if (isAvailable(outToServer)) {
            push(outToServer, authenticatedMsg)
          } else {
            toServerQueue.enqueue(authenticatedMsg)
          }

          val authResponse = AuthResponse(true)
          if (isAvailable(outToClient)) {
            push(outToClient, authResponse)
          } else {
            toClientQueue.enqueue(authResponse)
          }

        case Success((false, username)) =>
          log.debug("Authentication failed: {}", username)
          emit(outToClient, AuthResponse(false))
          close()

        case Failure(e) =>
          log.info("Error when authenticating {}", e)
          emit(outToClient, Error("Internal error"))
          close()
      }
    }

    /* Initial setup - Unauthenticated state */
    // Server demands
    setHandler(outToServer, emptyOutHandler)
    // Client demands
    setHandler(outToClient, emptyOutHandler)
    // Server provides
    setHandler(inFromServer, neverCalledInHandler)
    // Client provides
    setHandler(
      inFromClient,
      new InHandler {
        override def onPush(): Unit = {
          grab(inFromClient) match {
            case ToSessionStreamElement.FromClientWrapper(AuthRequest(username, password)) =>
              implicit val timeout: Timeout = config.AskTimeout
              implicit val ec: ExecutionContext = graphStage.materializer.executionContext
              (chatActor ? Authenticate(username, password))
                .mapTo[AuthenticateResult]
                .map(ar => (ar.success, username))
                .onComplete(authCallback.invoke)
              goToAuthenticationInProgressState()

            case other =>
              log.warning("Received {} from client, Authentication request expected. Stopping", other)
              emit(outToClient, Error("Protocol violation: Authentication request is expected"))
              close()
          }
        }
      }
    )

    private def goToAuthenticationInProgressState(): Unit = {
      // Client provides
      setHandler(
        inFromClient,
        new InHandler {
          override def onPush(): Unit = {
            val fromClient = grab(inFromClient)
            log.warning("Received {} from client, nothing is expected now. Stopping", fromClient)
            emit(outToClient, Error("Protocol violation: Nothing is expected"))
            close()
          }
        }
      )
    }

    // scalastyle:off method.length
    private def goToAuthenticatedState(): Unit = {
      // Server demands
      setHandler(
        outToServer,
        new OutHandler {
          override def onPull(): Unit = {
            log.debug(s"Server demands, queue ${toServerQueue.toList}")
            if (toServerQueue.nonEmpty) {
              push(outToServer, toServerQueue.dequeue())
            } else {
              if (!hasBeenPulled(inFromClient)) {
                pull(inFromClient)
              }
            }
          }
        }
      )

      // Client demands
      setHandler(
        outToClient,
        new OutHandler {
          override def onPull(): Unit = {
            log.debug(s"Client demands, queue ${toClientQueue.toList}")
            if (toClientQueue.nonEmpty) {
              push(outToClient, toClientQueue.dequeue())
            } else {
              if (!hasBeenPulled(inFromServer)) {
                pull(inFromServer)
              }
            }
          }
        }
      )

      // Server provides
      setHandler(
        inFromServer,
        new InHandler {
          override def onPush(): Unit = {
            val fromServer = grab(inFromServer)
            if (isAvailable(outToClient)) {
              push(outToClient, fromServer)
            } else {
              toClientQueue.enqueue(fromServer)
            }
          }

          override def onUpstreamFinish(): Unit = {
            emitMultiple(outToClient, toClientQueue.toIterator)
          }
        }
      )

      setHandler(
        inFromClient,
        new InHandler {
          override def onPush(): Unit = {
            val fromClient = grab(inFromClient)
            if (isAvailable(outToServer)) {
              push(outToServer, fromClient)
            } else {
              toServerQueue.enqueue(fromClient)
            }
          }

          override def onUpstreamFinish(): Unit = {
            emitMultiple(outToServer, toServerQueue.toIterator)
          }
        }
      )
    }
    // scalastyle:on method.length

    private def close(): Unit = {
      complete(outToClient)
    }
  }
}

object AuthenticationStage {

  private val neverCalledInHandler = new InHandler {
    override def onPush(): Unit = ???
  }

  private val emptyOutHandler = new OutHandler {
    override def onPull(): Unit = {}
  }

}
