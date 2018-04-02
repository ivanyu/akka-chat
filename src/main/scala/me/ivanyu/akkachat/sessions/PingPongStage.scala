package me.ivanyu.akkachat.sessions

import akka.stream.{Attributes, BidiShape, Inlet, Outlet}
import akka.stream.stage._

import me.ivanyu.akkachat.AppConfig
import me.ivanyu.akkachat.clientserverprotocol.ClientServerProtocol._

/**
  * The graph stage for ping-pong (keepalive) exchange with the client.
  *
  * The ping-pong exchange starts when the client has been authenticated.
  * It starts two timers:
  *  - one for sending Ping messages to the client;
  *  - one for expecting Pong messages from the client.
  *
  * If the client hasn't replied with Pong in the set period,
  * the connection is closed.
  */
private class PingPongStage(config: AppConfig)
    extends GraphStage[BidiShape[ToSessionStreamElement, ToSessionStreamElement, FromServer, FromServer]] {

  val inFromClient: Inlet[ToSessionStreamElement] = Inlet("InFromClient")
  val outToServer: Outlet[ToSessionStreamElement] = Outlet("OutToServer")

  val inFromServer: Inlet[FromServer] = Inlet("InFromServer")
  val outToClient: Outlet[FromServer] = Outlet("OutToClient")

  override val shape: BidiShape[ToSessionStreamElement, ToSessionStreamElement, FromServer, FromServer] =
    BidiShape(inFromClient, outToServer, inFromServer, outToClient)

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic = new StageLogic

  private class StageLogic extends TimerGraphStageLogic(shape) with StageLogging {

    private case object SendPingTimer

    private val sendPingPeriod = config.Session.PingPeriod

    private case object ClientTimeoutTimer

    private val clientTimeout = config.Session.ClientInactivityTimeout

    private var needToSendPing = false

    setHandler(outToServer, new OutHandler {
      override def onPull(): Unit = {
        pull(inFromClient)
      }
    })

    setHandler(
      outToClient,
      new OutHandler {
        override def onPull(): Unit = {
          if (needToSendPing) {
            log.debug("Sending PING")
            push(outToClient, Ping)
            if (!hasBeenPulled(inFromClient)) {
              pull(inFromClient)
            }
            scheduleSendPing()
          } else {
            if (!hasBeenPulled(inFromServer)) {
              pull(inFromServer)
            }
          }
        }
      }
    )

    setHandler(inFromServer, new InHandler {
      override def onPush(): Unit = {
        push(outToClient, grab(inFromServer))
      }
    })

    setHandler(
      inFromClient,
      new InHandler {
        override def onPush(): Unit = {
          grab(inFromClient) match {
            case auth @ ToSessionStreamElement.Authenticated(_) =>
              scheduleSendPing()
              scheduleClientTimeout()
              push(outToServer, auth)

            case ToSessionStreamElement.FromClientWrapper(Pong) =>
              log.debug("Got PONG")
              if (!hasBeenPulled(inFromClient)) {
                pull(inFromClient)
              }
              scheduleClientTimeout()

            case fromClient =>
              push(outToServer, fromClient)
          }
        }
      }
    )

    private def scheduleSendPing(): Unit = {
      needToSendPing = false
      cancelTimer(SendPingTimer)
      scheduleOnce(SendPingTimer, sendPingPeriod)
    }

    private def scheduleClientTimeout(): Unit = {
      cancelTimer(ClientTimeoutTimer)
      scheduleOnce(ClientTimeoutTimer, clientTimeout)
    }

    override protected def onTimer(timerKey: Any): Unit = timerKey match {
      case SendPingTimer =>
        if (isAvailable(outToClient)) {
          log.debug("Sending PING")
          push(outToClient, Ping)
          if (!hasBeenPulled(inFromClient)) {
            pull(inFromClient)
          }
          scheduleSendPing()
        } else {
          needToSendPing = true
        }

      case ClientTimeoutTimer =>
        log.debug("Client inactivity timeout. Closing")
        close()
    }

    private def close(): Unit = {
      complete(outToClient)
    }
  }
}
