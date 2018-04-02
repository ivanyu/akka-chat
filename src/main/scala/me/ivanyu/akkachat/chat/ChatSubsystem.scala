package me.ivanyu.akkachat.chat

import akka.actor.{ActorRef, ActorSystem}

import me.ivanyu.akkachat.AppConfig

class ChatSubsystem(config: AppConfig)(implicit val actorSys: ActorSystem) {

  val chatActor: ActorRef = actorSys.actorOf(ChatActor.props(config), "chat")

}
