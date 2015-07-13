package com.colobu.webtest.undertow

import java.util.concurrent.TimeUnit

import com.typesafe.config.ConfigFactory
import io.undertow.websockets.core.WebSocketChannel

import scala.collection.mutable


object Common {
  val conf = ConfigFactory.load()
  val onlyTestConnect = conf.getBoolean("onlyTestConnect")
  val clients =new mutable.HashSet[WebSocketChannel]() with mutable.SynchronizedSet[WebSocketChannel]
  val totalSize = conf.getInt("totalSize")

  val serverIP = conf.getString("server.ip")
  val port = conf.getInt("server.port")
  val delay = conf.getDuration("sending.timer.delay", TimeUnit.MILLISECONDS)
  val interval = conf.getDuration("sending.timer.interval", TimeUnit.MILLISECONDS)
}
