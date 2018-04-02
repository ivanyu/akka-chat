package me.ivanyu.akkachat.sessions

import scala.concurrent.{ExecutionContext, Future}

import akka.NotUsed
import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.model.ws.{BinaryMessage, Message, TextMessage}
import akka.pattern.ask
import akka.stream._
import akka.stream.scaladsl.{Flow, GraphDSL, Sink, Source}
import akka.util.Timeout

import me.ivanyu.akkachat.AppConfig
import me.ivanyu.akkachat.chat.ChatSubsystem
import me.ivanyu.akkachat.clientserverprotocol.ClientServerProtocol._
import me.ivanyu.akkachat.sessions.SessionManagerProtocol._

/**
  * The sessions subsystem.
  *
  * Owns all sessions.
  */
class SessionsSubsystem(config: AppConfig, chatSubsystem: ChatSubsystem)(implicit val actorSys: ActorSystem,
                                                                         val mat: Materializer) {

  private implicit val ec: ExecutionContext = actorSys.dispatcher

  private val sessionManager = actorSys.actorOf(SessionManager.props(config, chatSubsystem.chatActor), "sessions")

  private val bufferSize = config.Session.StreamBufferSize

  def createWSFlow(): Future[Flow[Message, Message, Any]] = {
    implicit val askTimeout: Timeout = config.AskTimeout

    (sessionManager ? CreateSession)
      .mapTo[CreateSessionResult]
      .map(csr => createWSFlow(csr.session))
  }

  // In case of too much backpressure fail the stream.
  // This is needed to avoid the connection hanging if the server is too slow.
  private val inBuffer = Flow[Message].buffer(bufferSize, OverflowStrategy.fail)

  // Discard binary messages,
  // linearise chunked text messages.
  private val inWSMessageToString: Flow[Message, String, NotUsed] =
    Flow[Message].flatMapConcat {
      case tm: TextMessage =>
        tm.textStream
      case bm: BinaryMessage =>
        bm.dataStream.runWith(Sink.ignore)
        throw new Exception("Binary messages not supported")
    }

  private val outStringToWSMessage: Flow[String, Message, NotUsed] =
    Flow[String].map(s => TextMessage(s))

  private def inSink(session: ActorRef): Sink[ToSessionStreamElement, NotUsed] = {
    Sink.actorRefWithAck[ToSessionStreamElement](session, SessionStreamInit, SessionStreamAck, SessionStreamComplete)
  }

  // Fail the outgoing stream if there's too much backpressure.
  // This is needed to avoid the connection hanging if the server is too slow.
  private def outSource(session: ActorRef): Source[FromServer, Any] =
    Source
      .actorRef[FromServer](bufferSize, OverflowStrategy.fail)
      .mapMaterializedValue { sourceActor =>
        session ! RegisterOutActor(sourceActor)
      }

  /**
    * Incoming: WS -> inBuffer -> inWSMessageToString -> serialization -> auth -> inSink -> sessionActor
    * Outgoing: sessionActor -> outSource -> auth -> serialization -> outStringToWSMessage -> WS
    */
  private def createWSFlow(session: ActorRef): Flow[Message, Message, Any] = {
    Flow.fromGraph(GraphDSL.create() { implicit b =>
      import GraphDSL.Implicits._

      val inBufferShape: FlowShape[Message, Message] = b.add(inBuffer)
      val inWSMessageToStringShape: FlowShape[Message, String] = b.add(inWSMessageToString)
      val inSinkShape: SinkShape[ToSessionStreamElement] = b.add(inSink(session))

      val serialization: BidiShape[String, ToSessionStreamElement, FromServer, String] =
        b.add(new SerializationStage(config))

      val authentication: BidiShape[ToSessionStreamElement, ToSessionStreamElement, FromServer, FromServer] =
        b.add(new AuthenticationStage(config, chatSubsystem.chatActor))

      val outSourceShape: SourceShape[FromServer] = b.add(outSource(session))
      val outStringToWSMessageShape: FlowShape[String, Message] = b.add(outStringToWSMessage)

      inBufferShape.out ~> inWSMessageToStringShape ~> serialization.in1
      serialization.out1 ~> authentication.in1
      authentication.out1 ~> inSinkShape.in

      outSourceShape.out ~> authentication.in2
      authentication.out2 ~> serialization.in2
      serialization.out2 ~> outStringToWSMessageShape.in

      FlowShape(inBufferShape.in, outStringToWSMessageShape.out)
    })
  }
}
