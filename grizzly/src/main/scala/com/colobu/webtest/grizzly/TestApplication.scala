package com.colobu.webtest.grizzly

import java.util

import com.typesafe.scalalogging.LazyLogging
import org.glassfish.grizzly.websockets.{DataFrame, WebSocket, WebSocketApplication}


class TestApplication extends WebSocketApplication  with LazyLogging{
  override def onError(webSocket: WebSocket, t: Throwable): Boolean = super.onError(webSocket, t)

  override def onConnect(socket: WebSocket): Unit = super.onConnect(socket)

  override def onClose(socket: WebSocket, frame: DataFrame): Unit = super.onClose(socket, frame)

  override def onMessage(socket: WebSocket, text: String): Unit = super.onMessage(socket, text)

  override def getWebSockets: util.Set[WebSocket] = super.getWebSockets
}
