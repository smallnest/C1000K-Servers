package com.colobu.webtest.vertx

import java.util.UUID

import com.typesafe.scalalogging.Logger
import org.vertx.java.core.impl.DefaultVertxFactory
import org.vertx.java.core.{Vertx => JVertx}
import org.slf4j.LoggerFactory
import org.vertx.scala.core.Vertx
import org.vertx.scala.core.buffer.Buffer
import org.vertx.scala.core.http.{HttpServerRequest, ServerWebSocket}
import scala.concurrent.duration._

object WebServer extends App {
  lazy val log: Logger = Logger(LoggerFactory.getLogger(getClass.getName))

  val vertx = Vertx(new DefaultVertxFactory().createVertx())
  vertx.createHttpServer().websocketHandler({ ws: ServerWebSocket =>
    if (ws.path().equals("/")) {
      Common.serverWebSockets += ws
      ws.endHandler(Common.serverWebSockets -= ws)
      ws.dataHandler({ data: Buffer =>
        //ws.writeTextFrame(data.toString())
      })
    } else {
      ws.reject()
    }
  }).requestHandler({ req: HttpServerRequest =>
    if (req.path().equals("/ws")) req.response().sendFile("websockets/ws.html") // Serve the html
  }).listen(Common.port)


  if (!Common.onlyTestConnect) {
    vertx.setTimer(Common.delay.minutes.toMillis, id => {
      vertx.setPeriodic(Common.interval.minutes.toMillis, id => {
        val flag = UUID.randomUUID().toString
        if (Common.serverWebSockets.size < Common.totalSize) {
          log.info(s"current websockets: ${Common.serverWebSockets.size} for $flag")
        } else {
          log.info(s"send msg to websockets for $flag")
          Common.serverWebSockets.par.foreach(s => {
            s.writeTextFrame(System.currentTimeMillis().toString)
          })
          log.info(s"sent msg to websockets for $flag. current websockets: ${Common.serverWebSockets.size}")
        }
      })
    })
  }
synchronized {  wait() }

}
