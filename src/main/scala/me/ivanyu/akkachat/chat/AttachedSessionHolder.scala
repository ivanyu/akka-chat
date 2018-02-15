package me.ivanyu.akkachat.chat

import akka.actor.ActorRef

import me.ivanyu.akkachat.clientserverprotocol.ClientServerProtocol.FromServer

private class AttachedSessionHolder {
  private var attachedSessions: Set[ActorRef] = Set.empty

  def attach(sessionActor: ActorRef): Unit = {
    attachedSessions += sessionActor
  }

  def detach(sessionActor: ActorRef): Unit = {
    attachedSessions -= sessionActor
  }

  def isEmpty: Boolean = {
    attachedSessions.isEmpty
  }

  def sendToAll(message: FromServer): Unit = {
    attachedSessions.foreach(_ ! message)
  }

  def sendToAllButSome(message: FromServer, notSendTo: Set[ActorRef]): Unit = {
    (attachedSessions -- notSendTo).foreach { session =>
      session ! message
    }
  }
}
