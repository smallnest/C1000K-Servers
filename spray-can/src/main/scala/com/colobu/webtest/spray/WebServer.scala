package com.colobu.webtest.spray

import java.util.UUID

import akka.actor._
import akka.io.IO
import akka.kernel.Bootable
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.LazyLogging
import spray.can.Http
import spray.can.server.UHttp

import scala.collection.mutable
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.language.postfixOps


class WebServer extends Bootable with LazyLogging{

  implicit val system = ActorSystem()
  val totalSize = Common.totalSize

  override def startup(): Unit = {
    if (!Common.onlyTestConnect) {
      system.scheduler.schedule(system.settings.config.getLong("app.sendingDelay") minutes, system.settings.config.getLong("app.sendingInterval") minutes, new Runnable {
        override def run(): Unit = {
          val uuid = UUID.randomUUID().toString
          if (Common.workers.size >= totalSize) {

            logger.info(s"send msg to workers for $uuid")
            Common.workers.foreach(w => w ! "send")

            logger.info(s"sent msg to workers for $uuid. current workers: ${Common.workers.size}")
          } else {
            logger.info(s"current workers: ${Common.workers.size} for $uuid")
          }

        }
      })
    }
    // the handler actor replies to incoming HttpRequests
    val manager = system.actorOf(Props[WebSocketServer], name = "websocket-server")
    val interface = system.settings.config.getString("app.interface")
    val port = system.settings.config.getInt("app.port")
    IO(UHttp) ! Http.Bind(manager, interface, port)
  }

  override def shutdown(): Unit = {
    system.shutdown()
  }
}

object WebSocketServer {
  def props() = Props(classOf[WebSocketServer])
}
class WebSocketServer extends Actor with ActorLogging {
  def receive = {
    case Http.Connected(remoteAddress, localAddress) =>
      val serverConnection = sender()
      val conn = context.actorOf(WebSocketWorker.props(serverConnection))

      serverConnection ! Http.Register(conn)
      if (!Common.onlyTestConnect) {
        Common.workers += conn
      }
  }
}

object Common {
  val conf = ConfigFactory.load()
  val onlyTestConnect = conf.getBoolean("app.onlyTestConnect")
  val totalSize =conf.getInt("app.totalSize")
  var workers = new mutable.HashSet[ActorRef]() with mutable.SynchronizedSet[ActorRef]
}