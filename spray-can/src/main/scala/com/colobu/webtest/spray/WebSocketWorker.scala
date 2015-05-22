package com.colobu.webtest.spray

import akka.actor.{ActorRef, Props}
import spray.can.websocket.FrameCommandFailed
import spray.can.websocket.frame.{BinaryFrame, TextFrame}
import spray.can.{Http, websocket}
import spray.http._
import spray.routing.HttpServiceActor

object WebSocketWorker {
  def props(serverConnection: ActorRef) = Props(classOf[WebSocketWorker], serverConnection)
}

class WebSocketWorker(val serverConnection: ActorRef) extends HttpServiceActor with websocket.WebSocketServerWorker {
  override def receive = handshaking  orElse closeLogic

  override def closeLogic: Receive = {
    case ev: Http.ConnectionClosed =>
      if (!Common.onlyTestConnect) {
        Common.workers -= self
      }
      context.stop(self)
      log.debug("Connection closed on event: {}", ev)
  }

  def businessLogic: Receive = {
    case "send" => send(TextFrame(System.currentTimeMillis().toString))
    case x@(_: BinaryFrame | _: TextFrame) =>  //sender() ! x
    case x: FrameCommandFailed =>
      log.error("frame command failed", x)
    case x: HttpRequest =>

  }

}
