package main.scala.com.colobu.webtest.netty

import com.typesafe.config.ConfigFactory

object Common {
  val conf = ConfigFactory.load()
  val onlyTestConnect = conf.getBoolean("onlyTestConnect")
  val totalSize = conf.getInt("totalSize")

  val port = conf.getInt("server.port")
  val delay = conf.getLong("sending.timer.delay")
  val interval = conf.getLong("sending.timer.interval")
}
