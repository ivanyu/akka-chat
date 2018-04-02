package me.ivanyu.akkachat

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer

import me.ivanyu.akkachat.http.HttpSubsystem
import me.ivanyu.akkachat.sessions.SessionsSubsystem
import me.ivanyu.akkachat.chat.ChatSubsystem

object Main extends App {
  implicit val actorSys: ActorSystem = ActorSystem("akkachat")
  implicit val mat: ActorMaterializer = ActorMaterializer()

  val config = AppConfig()

  val chatSubsystem = new ChatSubsystem(config)
  val sessionsSubsystem = new SessionsSubsystem(config, chatSubsystem)
  val httpSubsystem = new HttpSubsystem(config, sessionsSubsystem)
  httpSubsystem.run()
}
