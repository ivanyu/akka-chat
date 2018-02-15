package me.ivanyu.akkachat.sessions

import akka.actor.{Actor, ActorRef, Props}
import akka.util.Helpers

import me.ivanyu.akkachat.AppConfig
import me.ivanyu.akkachat.util.SequentialCounter

private class SessionManager(config: AppConfig, chatActor: ActorRef) extends Actor {

  import SessionManagerProtocol._

  private val sessionCnt = new SequentialCounter

  override def receive: Receive = {
    case CreateSession =>
      val session = context.actorOf(
        SessionActor.props(config, chatActor),
        "session-" + Helpers.base64(sessionCnt.next())
      )
      sender() ! CreateSessionResult(session)
  }
}

private object SessionManager {
  def props(config: AppConfig, chatActor: ActorRef): Props = {
    Props(classOf[SessionManager], config, chatActor)
  }
}

private object SessionManagerProtocol {

  case object CreateSession

  final case class CreateSessionResult(session: ActorRef)

  ////
  // Session stream protocol.
  ////
  final case class RegisterOutActor(outActor: ActorRef)

  case object SessionStreamInit

  case object SessionStreamAck

  case object SessionStreamComplete

}
