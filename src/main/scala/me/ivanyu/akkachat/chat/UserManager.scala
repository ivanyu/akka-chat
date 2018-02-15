package me.ivanyu.akkachat.chat

import akka.actor.{Actor, ActorLogging, Props}

import me.ivanyu.akkachat.AppConfig
import me.ivanyu.akkachat.chat.ChatActorProtocol._
import me.ivanyu.akkachat.chat.UserManagerProtocol._

private[chat] class UserManager(config: AppConfig) extends Actor with ActorLogging {

  private val knownUsers = List(
    ("alice", "alice"),
    ("bob", "bob"),
    ("charlie", "charlie")
  )

  override val receive: Receive = {
    case Authenticate(username, password) =>
      knownUsers.collectFirst { case (`username`, `password`) => () } match {
        case Some(_) =>
          sender() ! AuthenticateResult(true)

        case _ =>
          sender() ! AuthenticateResult(false)
      }

    case GetAllUsers =>
      sender() ! AllUsers(knownUsers.map(_._1))
  }
}

private[chat] object UserManager {
  def props(config: AppConfig): Props = {
    Props(classOf[UserManager], config)
  }
}

private[chat] object UserManagerProtocol {
  case object GetAllUsers
  final case class AllUsers(usersnames: List[String])
}
