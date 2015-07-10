package com.colobu.webtest.grizzly

import java.util.UUID
import java.util.concurrent.{TimeUnit, Executors}
import javax.websocket._
import javax.websocket.server.ServerEndpoint

import com.typesafe.scalalogging.LazyLogging
import org.eclipse.jetty.server.{Server, ServerConnector}
import org.eclipse.jetty.servlet.ServletContextHandler
import org.eclipse.jetty.websocket.jsr356.server.deploy.WebSocketServerContainerInitializer

import scala.collection.mutable

object WebServer extends App with LazyLogging {
  val server = new Server()
  val connector = new ServerConnector(server)
  connector.setPort(Common.port)
  server.addConnector(connector)

  val context = new ServletContextHandler(ServletContextHandler.SESSIONS)
  context.setContextPath("/")
  server.setHandler(context)

  if (!Common.onlyTestConnect) {
    Executors.newScheduledThreadPool(1).scheduleAtFixedRate(new Runnable {
      override def run(): Unit = {
        val flag = UUID.randomUUID().toString
        if (Common.sessions.size < Common.totalSize) {
          logger.info(s"current sessions: ${Common.sessions.size} for $flag")
        } else {
          logger.info(s"send msg to sessions for $flag")
          Common.sessions.foreach(c => {
            c.getBasicRemote.sendText(System.currentTimeMillis().toString)
          })
          logger.info(s"sent msg to sessions for $flag. current websockets: ${Common.sessions.size}")
        }
      }
    }, Common.delay, Common.interval, TimeUnit.MINUTES)
  }

  try {
    // Initialize javax.websocket layer
    val wscontainer = WebSocketServerContainerInitializer.configureContext(context)
    // Add WebSocket endpoint to javax.websocket layer
    wscontainer.addEndpoint(classOf[WebSocketEndPoint])

    server.start()
    //server.dump(System.err)
    server.join()
  }
  catch {
    case e: Throwable => e.printStackTrace()
  }
}

@ServerEndpoint(value = "/")
class WebSocketEndPoint extends LazyLogging {
  @OnOpen
  def onWebSocketConnect(session: Session): Unit = {
    //println(s"Session opened:${Common.sessions.size}")
    Common.sessions += session
  }

  @OnMessage
  def onWebSocketText(message: String): Unit = {
    //println("Received TEXT message: " + message)
  }

  @OnClose
  def onWebSocketClose(session: Session, reason: CloseReason): Unit = {
    //System.out.println("Socket Closed: " + reason)
    Common.sessions -= session
  }

  @OnError
  def onWebSocketError(cause: Throwable): Unit = {
    //cause.printStackTrace(System.err)
  }
}
