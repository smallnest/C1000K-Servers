package main.scala.com.colobu.webtest.netty

import java.util.UUID
import java.util.concurrent.{Executors, TimeUnit}
import scala.collection.JavaConverters._

import com.colobu.webtest.grizzly.TestApplication
import com.typesafe.scalalogging.LazyLogging
import org.glassfish.grizzly.http.server.HttpServer
import org.glassfish.grizzly.websockets.{WebSocketAddOn, WebSocketEngine}

object WebServer extends App with LazyLogging{
  val server = HttpServer.createSimpleServer(".", Common.port)
  // Register the WebSockets add on with the HttpServer. Name of the default listener is "grizzly"
  server.getListener("grizzly").registerAddOn(new WebSocketAddOn())
  // initialize websocket chat application
  val app = new TestApplication()
  // register the application
  WebSocketEngine.getEngine().register("", "/", app)

  if (!Common.onlyTestConnect) {
    Executors.newScheduledThreadPool(1).scheduleAtFixedRate(new Runnable {
      override def run(): Unit = {
        val flag = UUID.randomUUID().toString
        if (app.getWebSockets.size() < Common.totalSize) {
          logger.info(s"current channels: ${app.getWebSockets.size()} for $flag")
        } else {
          logger.info(s"send msg to channels for $flag")
          //            Common.clients.write(new TextWebSocketFrame(System.currentTimeMillis().toString))
          app.getWebSockets.asScala.foreach(ws => {
            ws.send(System.currentTimeMillis().toString)
          })
          logger.info(s"sent msg to channels for $flag. current channels: ${app.getWebSockets.size}")
        }
      }
    }, Common.delay, Common.interval, TimeUnit.MINUTES)
  }

  try {
    server.start()
    logger.info("server started")
    synchronized {  wait() }
  } finally {
    // stop the server
    server.shutdownNow()
  }
}