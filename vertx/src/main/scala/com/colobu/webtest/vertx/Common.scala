package com.colobu.webtest.vertx


import com.typesafe.config.ConfigFactory
import org.vertx.scala.core.http.ServerWebSocket

import scala.collection.mutable


object Common {
  val conf = ConfigFactory.load()
  val onlyTestConnect = conf.getBoolean("onlyTestConnect")

  val totalSize = conf.getInt("totalSize")
  var serverWebSockets = new mutable.HashSet[ServerWebSocket]()

  val port = conf.getInt("server.port")
  val delay = conf.getLong("sending.timer.delay")
  val interval = conf.getLong("sending.timer.interval")
}
