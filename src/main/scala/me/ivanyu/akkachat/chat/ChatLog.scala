package me.ivanyu.akkachat.chat

import java.time.ZonedDateTime

private class ChatLog(maxChatLogElements: Int) {
  import me.ivanyu.akkachat.chat.ChatLog._

  private var elements: List[(Long, LogEntry)] = List.empty

  def append(seqN: Long, logEntry: LogEntry, pruneOld: Boolean): Unit = {
    elements = (seqN, logEntry) :: elements
    if (pruneOld) {
      prune()
    }
  }

  /**
    * Prunes the log to be maximum [[maxChatLogElements]] length, taking the latest.
    */
  def prune(): Unit = {
    elements = elements.take(maxChatLogElements)
  }

  def takeEntries(beforeSeqN: Option[Long], takeMax: Int): List[(Long, LogEntry)] = {
    val elementSubset = beforeSeqN match {
      case Some(b) =>
        elements.dropWhile { case (seqN, _) => seqN >= b }
      case _ =>
        elements
    }

    elementSubset.take(takeMax)
  }
}

private object ChatLog {

  /** Possible entities in the chat log. */
  sealed trait LogEntry

  final case class UserJoinedOrLeftLog(
      username: String,
      joined: Boolean,
      timestamp: ZonedDateTime
  ) extends LogEntry

  final case class UserMessageLog(
      username: String,
      timestamp: ZonedDateTime,
      text: String
  ) extends LogEntry
}
