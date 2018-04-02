package me.ivanyu.akkachat.util

import akka.actor.ActorRef

final case class WithReplyTo[P](payload: P, replyTo: ActorRef)
