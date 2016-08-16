package com.colobu.c1000k.testclient

import java.net._
import java.util.concurrent.{Executors, TimeUnit}
import javax.websocket._

import com.codahale.metrics.ConsoleReporter
import com.typesafe.scalalogging.LazyLogging
import org.eclipse.jetty.websocket.jsr356.ClientContainer


class SingleClient extends App  with LazyLogging{
  if (args.length >= 1) {
    Common.localIP = args(0)
  }


  val container = ContainerProvider.getWebSocketContainer().asInstanceOf[ClientContainer]
  container.setDefaultMaxSessionIdleTimeout(Common.conf.getLong("websocket.timeout"))
  container.getClient.setBindAddress(new InetSocketAddress(Common.localIP, 0))



  val batchSize = Common.batchSize
  val setupInterval = Common.setupInterval


  val threadPool = Executors.newFixedThreadPool(Common.conf.getInt("setup.threadpool.size"))

  new Thread() {
    override def run(): Unit = {
      var t = 0d
      var count = 0L
      while (count < Common.totalClients) {
        t = System.currentTimeMillis()
        for (i <- 1 to batchSize if (count < Common.totalClients)) {
          threadPool.submit(new Runnable {
            override def run(): Unit = {
              try {
                container.connectToServer(new TestClient(), new URI(Common.serverIP))
              } catch {
                case e : Exception => e.printStackTrace()
              }
            }
          })
          count += 1
          Common.setupRate.mark()
        }
        t = setupInterval + t - System.currentTimeMillis()
        logger.debug(s"$Common.name: send $count websockets and sleep $t ms")
        if (t > 0) {
          Thread.sleep(t.toLong)
        }
      }
    }
  }.start()




}


@ClientEndpoint
class TestClient() extends LazyLogging {
  @OnMessage
  def onMessage(session: Session, message: String): Unit = {
    Common.messageRate.mark()
    val timestamp = message.toLong
    Common.messageLatency.update(System.currentTimeMillis() - timestamp)
    logger.debug(s"received $message")

  }

  @OnOpen
  def onOpen(session: Session, config: EndpointConfig): Unit = {
    Common.activeWebSockets.inc()
  }

  @OnClose
  def onClose(session: Session, reason: CloseReason): Unit = {
    Common.activeWebSockets.dec()
    logger.info(s"closed because of $reason")
  }

  @OnError
  def onError(session: Session, t: Throwable): Unit = {
    Common.websocketError.inc()
    logger.error(s"$t")
  }
}

