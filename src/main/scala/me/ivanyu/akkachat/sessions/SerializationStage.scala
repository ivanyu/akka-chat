package me.ivanyu.akkachat.sessions

import akka.stream.{Attributes, BidiShape, Inlet, Outlet}
import akka.stream.stage._

import me.ivanyu.akkachat.AppConfig
import me.ivanyu.akkachat.clientserverprotocol.ClientServerProtocol._
import me.ivanyu.akkachat.clientserverprotocol.ClientServerProtocolDecoder.decodeFromClient
import me.ivanyu.akkachat.clientserverprotocol.ClientServerProtocolEncoder.encodeTopLevel

/**
  * The graph stage for deserialization of the user's messages
  * and serialization of the server's messages.
  *
  * It sends an error message to the client and closes the connection
  * in case of deserialization problem.
  */
private class SerializationStage(config: AppConfig)
    extends GraphStage[BidiShape[String, ToSessionStreamElement, FromServer, String]] {

  val inFromClient: Inlet[String] = Inlet("InFromClient")
  val outToServer: Outlet[ToSessionStreamElement] = Outlet("OutToServer")

  val inFromServer: Inlet[FromServer] = Inlet("InFromServer")
  val outToClient: Outlet[String] = Outlet("OutToClient")

  override val shape: BidiShape[String, ToSessionStreamElement, FromServer, String] =
    BidiShape(inFromClient, outToServer, inFromServer, outToClient)

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic = new StageLogic

  private class StageLogic extends GraphStageLogic(shape) with StageLogging {

    setHandler(outToServer, new OutHandler {
      override def onPull(): Unit = {
        pull(inFromClient)
      }
    })

    setHandler(outToClient, new OutHandler {
      override def onPull(): Unit = {
        pull(inFromServer)
      }
    })

    setHandler(
      inFromClient,
      new InHandler {
        override def onPush(): Unit = {
          val messageStr = grab(inFromClient)

          decodeFromClient(messageStr) match {
            case Right(m) =>
              push(outToServer, ToSessionStreamElement.FromClientWrapper(m))

            case Left(decodeError) =>
              log.warning("Received bad JSON from client: {}. Stopping", decodeError.getMessage)
              val errorMessage = encodeTopLevel(Error("Bad JSON"))
              emit(outToClient, errorMessage)

              // safe to call immediately after emit
              complete(outToClient)
          }
        }
      }
    )

    setHandler(
      inFromServer,
      new InHandler {
        override def onPush(): Unit = {
          grab(inFromServer) match {
            case fromServer: FromServer =>
              val fromServerMessage = encodeTopLevel(fromServer)
              push(outToClient, fromServerMessage)
          }
        }
      }
    )
  }
}
