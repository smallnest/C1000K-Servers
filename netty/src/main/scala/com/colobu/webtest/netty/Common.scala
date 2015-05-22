package com.colobu.webtest.netty

import com.typesafe.config.ConfigFactory
import io.netty.channel.group.DefaultChannelGroup
import io.netty.util.concurrent.GlobalEventExecutor


object Common {
  val conf = ConfigFactory.load()
  val onlyTestConnect = conf.getBoolean("onlyTestConnect")
  val clients = new DefaultChannelGroup("activeWebsocketClients", GlobalEventExecutor.INSTANCE)
  val totalSize = conf.getInt("totalSize")

  val port = conf.getInt("server.port")
  val delay = conf.getLong("sending.timer.delay")
  val interval = conf.getLong("sending.timer.interval")
}
