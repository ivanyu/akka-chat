package me.ivanyu.akkachat.http

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer
import me.ivanyu.akkachat.AppConfig
import me.ivanyu.akkachat.sessions.SessionsSubsystem

/**
  * The HTTP subsystem.
  */
class HttpSubsystem(config: AppConfig, sessionsSubsystem: SessionsSubsystem)(implicit val actorSys: ActorSystem,
                                                                             implicit val mat: ActorMaterializer) {

  val routes: Route = {
    (path("websocket") & get & extractUpgradeToWebSocket) { upgradeToWebSocket =>
      onSuccess(sessionsSubsystem.createWSFlow()) { flow =>
        complete(upgradeToWebSocket.handleMessages(flow))
      }
    }
  }

  def run(): Unit = {
    Http().bindAndHandle(routes, config.Http.Host, config.Http.Port)
    ()
  }
}
