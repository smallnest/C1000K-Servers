package com.colobu.webtest.grizzly

import java.util.concurrent.TimeUnit
import javax.websocket.Session

import com.typesafe.config.ConfigFactory

import scala.collection.mutable


object Common {
  val conf = ConfigFactory.load()
  val onlyTestConnect = conf.getBoolean("onlyTestConnect")

  val totalSize = conf.getInt("totalSize")
  var sessions = new mutable.HashSet[Session]()

  val port = conf.getInt("server.port")
  val delay = conf.getDuration("sending.timer.delay", TimeUnit.MILLISECONDS)
  val interval = conf.getDuration("sending.timer.interval", TimeUnit.MILLISECONDS)
}
