package me.ivanyu.akkachat.chat

import java.time.{Duration, ZonedDateTime}

import scala.collection.immutable.Queue
import scala.concurrent.duration.FiniteDuration

import me.ivanyu.akkachat.clientserverprotocol.ClientServerProtocol.{MessageAck, MessageId}

// TODO test RecentlyAcceptedMessagesHolder
/**
  * Stores recently accepted messages and their [[MessageAck]]s,
  * supports deleting of items old enough.
  *
  * @param maxDuration the maximum duration of item to stay in the holder.
  */
private class RecentlyAcceptedMessagesHolder(maxDuration: FiniteDuration) {

  private val maxDurationJava = Duration.ofNanos(maxDuration.toNanos)

  private var map: Map[MessageId, MessageAck] = Map.empty

  private var queue: Queue[(MessageId, ZonedDateTime)] = Queue.empty

  def add(messageId: MessageId, ack: MessageAck): Unit = {
    if (!map.contains(messageId)) {
      map += messageId -> ack
      queue.enqueue(messageId -> ZonedDateTime.now())
    }
    ()
  }

  def get(messageId: MessageId): Option[MessageAck] = {
    map.get(messageId)
  }

  def deleteOld(): Unit = {
    val now = ZonedDateTime.now()

    def isHeadOldEnoughToDelete: Boolean = {
      val headTime = queue.head._2
      val elapsedSinceHead = Duration.between(headTime, now)
      elapsedSinceHead.compareTo(maxDurationJava) >= 0
    }

    while (queue.nonEmpty && isHeadOldEnoughToDelete) {
      val (_, newQueue) = queue.dequeue
      queue = newQueue
    }
  }
}
