package me.ivanyu.akkachat

import akka.http.scaladsl.testkit.WSProbe

import org.scalatest.FlatSpecLike

import me.ivanyu.akkachat.clientserverprotocol.ClientServerProtocol._

class ChatTestSharedSystem extends FlatSpecLike with ChatTest {

  "Chat" should "disallow bad JSON" in {
    val wsClient = WSProbe()
    connect(wsClient)

    wsClient.sendMessage("blah")
    val expectedError = Error("Bad JSON")
    wsClient.expectServerMessage(expectedError)
    wsClient.expectCompletion()
  }

  it should "disallow messaging without authentication" in {
    val wsClient = WSProbe()
    connect(wsClient)

    wsClient.sendMessage(ClientToServerMessage("42", "bbb"))
    val expectedError = Error("Protocol violation: Authentication request is expected")
    wsClient.expectServerMessage(expectedError)
    wsClient.expectCompletion()
  }

  it should "check authentication (incorrect)" in {
    val wsClient = WSProbe()
    connect(wsClient)

    wsClient.sendMessage(AuthRequest("random", "guy"))
    val expectedAuthResponse = AuthResponse(false)
    wsClient.expectServerMessage(expectedAuthResponse)
    wsClient.expectCompletion()
  }

  it should "check authentication (correct)" in {
    val wsClient = WSProbe()
    connect(wsClient)

    wsClient.sendMessage(AuthRequest("alice", "alice"))
    val expectedAuthResponse = AuthResponse(true)
    wsClient.expectServerMessage(expectedAuthResponse)
    wsClient.sendCompletion()
  }
}
