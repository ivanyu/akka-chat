package me.ivanyu.akkachat.util

/**
  * A simple sequential counter.
  */
class SequentialCounter {
  private var seqN = 0L

  /**
    * Returns the next value.
    */
  def next(): Long = {
    val r = seqN
    seqN += 1
    r
  }
}
