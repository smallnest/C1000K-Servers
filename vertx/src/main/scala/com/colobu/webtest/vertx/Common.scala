package com.colobu.webtest.vertx


import java.util.concurrent.TimeUnit

import com.typesafe.config.ConfigFactory
import org.vertx.scala.core.http.ServerWebSocket

import scala.collection.mutable


object Common {
  val conf = ConfigFactory.load()
  val onlyTestConnect = conf.getBoolean("onlyTestConnect")

  val totalSize = conf.getInt("totalSize")
  var serverWebSockets = new mutable.HashSet[ServerWebSocket]()

  val port = conf.getInt("server.port")
  val delay = conf.getDuration("sending.timer.delay", TimeUnit.MILLISECONDS)
  val interval = conf.getDuration("sending.timer.interval", TimeUnit.MILLISECONDS)
}
