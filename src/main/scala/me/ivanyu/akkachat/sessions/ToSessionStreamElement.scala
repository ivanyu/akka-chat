package me.ivanyu.akkachat.sessions

import me.ivanyu.akkachat.clientserverprotocol.ClientServerProtocol.FromClient

/** Element that go through incoming stream to a session actor. */
private sealed trait ToSessionStreamElement

private object ToSessionStreamElement {

  /** A wrapper for a client message. */
  final case class FromClientWrapper(fromClient: FromClient) extends ToSessionStreamElement

  /** A signal that the user [[username]] is authenticated */
  final case class Authenticated(username: String) extends ToSessionStreamElement

}
