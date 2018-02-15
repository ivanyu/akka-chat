package me.ivanyu.akkachat.chat

import akka.actor.{ActorContext, ActorRef}
import akka.util.Helpers

import me.ivanyu.akkachat.AppConfig
import me.ivanyu.akkachat.util.SequentialCounter

private class UserActorHolder(config: AppConfig) {

  /** The map between usernames and users sessions. */
  private var users: Map[String, ActorRef] = Map.empty

  /** The increasing counter of users. */
  private val userCnt = new SequentialCounter

  def getOrCreateUserActor(username: String, context: ActorContext): ActorRef = {
    if (!users.contains(username)) {
      users += username -> context.actorOf(
        UserActor.props(config, username),
        username + "-" + Helpers.base64(userCnt.next())
      )
    }
    users(username)
  }
}
