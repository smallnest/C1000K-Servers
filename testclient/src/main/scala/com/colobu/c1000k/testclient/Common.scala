package com.colobu.c1000k.testclient

import java.util.UUID
import java.util.concurrent.CountDownLatch

import com.codahale.metrics.MetricRegistry
import com.typesafe.config.ConfigFactory

/**
 * @author <a href="mailto:yuepan.chao@thistech.com">Yuepan Chao</a>
 * Created on 2015/5/6.
 */

object Common {
  val name = UUID.randomUUID().toString
  val conf = ConfigFactory.load()

  var localIP = conf.getString("local.ip")
  val serverIP =conf.getString("server.uri")
  val batchSize = conf.getInt("setup.batchSize")
  val setupInterval = conf.getLong("setup.interval")
  val totalClients = conf.getInt("total.clients")

  val metrics = new MetricRegistry()
  val setupRate = Common.metrics.meter(s"Setup Rate for $name")
  val messageRate = Common.metrics.meter(s"Message Rate for $name")
  val activeWebSockets = Common.metrics.counter(s"Active WebSockets for $name")
  val websocketError = Common.metrics.counter(s"WebSocket Errors for $name")
  val messageLatency = Common.metrics.histogram(s"Message latency for $name")
}