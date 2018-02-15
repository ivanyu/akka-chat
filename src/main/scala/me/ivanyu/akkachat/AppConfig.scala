package me.ivanyu.akkachat

import scala.concurrent.duration.FiniteDuration

import com.typesafe.config.{Config, ConfigFactory}
import AppConfig._

final case class AppConfig(
  AskTimeout: FiniteDuration,
  Http: HttpConfig,
  Session: SessionConfig,
  Chat: ChatConfig
)

object AppConfig {

  /**
    * The HTTP configuration.
    */
  final case class HttpConfig(Host: String, Port: Int)

  /**
    * The session configuration.
    *
    * @param StreamBufferSize The size of a session stream incoming and outgoing buffer.
    * @param PingPeriod The period of sending pings to a client.
    * @param ClientInactivityTimeout The period of client inactivity
    *                                (not responding with pong) that the server tolerates.
    */
  final case class SessionConfig(
    StreamBufferSize: Int,
    PingPeriod: FiniteDuration,
    ClientInactivityTimeout: FiniteDuration)

  /**
    * The chat configuration.
    *
    * @param RequestLogElementCount The number of the chat log elements to return
    *                               when requested.
    * @param UserActorIdleTimeout For how long a user actor with no sessions attached
    *                             should stay before being stopped.
    * @param MaxChatLogLengthInMemory How many chat elements (max)
    *                                 must be stored in memory.
    * @param MaxDurationOfRecentlyAcceptedMessages How long information about recently
    *                                              accepted messages should stay in memory.
    * @param SnapshotInterval The number of persisted elements of the chat log
    *                         after which a snapshot must be taken.
    */
  final case class ChatConfig(
    RequestLogElementCount: Int,
    UserActorIdleTimeout: FiniteDuration,
    MaxChatLogLengthInMemory: Int,
    MaxDurationOfRecentlyAcceptedMessages: FiniteDuration,
    SnapshotInterval: Int)


  def apply(): AppConfig = {
    val config: Config = ConfigFactory.load()

    AppConfig(
      AskTimeout = config.getDuration("application.ask-timeout").asScalaFinite,

      Http = HttpConfig(
        Host = config.getString("application.http.host"),
        Port = config.getInt("application.http.port")
      ),

      Session = SessionConfig(
        StreamBufferSize =
          config.getInt("application.session.stream-buffer-size"),
        PingPeriod =
          config.getDuration("application.session.ping-period").asScalaFinite,
        ClientInactivityTimeout =
          config.getDuration("application.session.client-inactivity-timeout").asScalaFinite
      ),

      Chat = ChatConfig(
        RequestLogElementCount =
          config.getInt("application.chat.request-log-element-count"),
        UserActorIdleTimeout =
          config.getDuration("application.chat.user-actor-idle-timeout").asScalaFinite,
        MaxChatLogLengthInMemory =
          config.getInt("application.chat.max-chat-log-length-in-memory"),
        MaxDurationOfRecentlyAcceptedMessages = config.getDuration(
          "application.chat.max-duration-of-recently-accepted-messages").asScalaFinite,
        SnapshotInterval =
          config.getInt("application.chat.snapshot-interval")
      )
    )
  }

  private implicit class RichJavaDuration(duration: java.time.Duration) {
    def asScalaFinite: FiniteDuration = {
      scala.concurrent.duration.Duration.fromNanos(duration.toNanos)
    }
  }

}
