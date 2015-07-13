package com.colobu.webtest.undertow

import java.util.UUID
import java.util.concurrent.{Executors, TimeUnit}

import com.typesafe.scalalogging.LazyLogging
import io.undertow.Handlers._
import io.undertow.Undertow
import io.undertow.websockets.WebSocketConnectionCallback
import io.undertow.websockets.core._
import io.undertow.websockets.spi.WebSocketHttpExchange

object WebServer extends App with LazyLogging{
  if (!Common.onlyTestConnect) {
    Executors.newScheduledThreadPool(1).scheduleAtFixedRate(new Runnable {
      override def run(): Unit = {
        val flag = UUID.randomUUID().toString
        if (Common.clients.size < Common.totalSize) {
          logger.info(s"current channels: ${Common.clients.size} for $flag")
        } else {
          logger.info(s"send msg to channels for $flag")
          Common.clients.foreach(c => {
            WebSockets.sendText(System.currentTimeMillis().toString, c, null)
          })
          logger.info(s"sent msg to channels for $flag. current websockets: ${Common.clients.size}")
        }
      }
    }, Common.delay, Common.interval, TimeUnit.MILLISECONDS)
  }

  val server = Undertow.builder()
    .addHttpListener(Common.port, Common.serverIP)
    .setHandler(path()
    .addPrefixPath("/", websocket(new WebSocketConnectionCallback() {
    override def onConnect(exchange: WebSocketHttpExchange, channel: WebSocketChannel): Unit = {
      if (!Common.onlyTestConnect) {
        Common.clients += channel
      }
      channel.getReceiveSetter().set(new AbstractReceiveListener() {
        override def onFullTextMessage(channel: WebSocketChannel, message: BufferedTextMessage): Unit = {}

        override def onCloseMessage(cm: CloseMessage, channel: WebSocketChannel): Unit = {
          if (!Common.onlyTestConnect) {
            Common.clients -= channel
          }
        }
      })
      channel.resumeReceives();
    }
  }))).build

  logger.info("started")
  server.start

}
