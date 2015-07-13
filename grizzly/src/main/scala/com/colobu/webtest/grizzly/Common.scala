package main.scala.com.colobu.webtest.grizzly

import java.util.concurrent.TimeUnit

import com.typesafe.config.ConfigFactory

object Common {
  val conf = ConfigFactory.load()
  val onlyTestConnect = conf.getBoolean("onlyTestConnect")
  val totalSize = conf.getInt("totalSize")

  val port = conf.getInt("server.port")
  val delay = conf.getDuration("sending.timer.delay", TimeUnit.MILLISECONDS)
  val interval = conf.getDuration("sending.timer.interval", TimeUnit.MILLISECONDS)
}
