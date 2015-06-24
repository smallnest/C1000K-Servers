package com.colobu.webtest.netty

import javax.websocket.Session

import com.typesafe.config.ConfigFactory

import scala.collection.mutable


object Common {
  val conf = ConfigFactory.load()
  val onlyTestConnect = conf.getBoolean("onlyTestConnect")

  val totalSize = conf.getInt("totalSize")
  var sessions = new mutable.HashSet[Session]()

  val port = conf.getInt("server.port")
  val delay = conf.getLong("sending.timer.delay")
  val interval = conf.getLong("sending.timer.interval")
}
