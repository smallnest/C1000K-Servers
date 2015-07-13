package com.colobu.webtest.netty

import java.util.concurrent.TimeUnit

import com.typesafe.config.ConfigFactory
import io.netty.channel.group.DefaultChannelGroup
import io.netty.util.concurrent.GlobalEventExecutor


object Common {
  val conf = ConfigFactory.load()
  val onlyTestConnect = conf.getBoolean("onlyTestConnect")
  val clients = new DefaultChannelGroup("activeWebsocketClients", GlobalEventExecutor.INSTANCE)
  val totalSize = conf.getInt("totalSize")

  val port = conf.getInt("server.port")
  val delay = conf.getDuration("sending.timer.delay", TimeUnit.MILLISECONDS)
  val interval = conf.getDuration("sending.timer.interval", TimeUnit.MILLISECONDS)
}
