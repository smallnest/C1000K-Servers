package com.colobu.c1000k.testclient

import java.util.concurrent.{Executors, TimeUnit}

import com.codahale.metrics.ConsoleReporter
import com.typesafe.scalalogging.LazyLogging


object AllMain extends App  with LazyLogging{
  val ipAddresses = Common.localIP.split(",")


  val es = Executors.newFixedThreadPool(ipAddresses.length)

  for (ip <- ipAddresses) {
    logger.info("start IP: " + ip)
    new SingleClient().main(Array(ip))
  }


  startReport

  System.in.read()


  def startReport(): Unit = {
    val reporter = ConsoleReporter.forRegistry(Common.metrics)
      .convertRatesTo(TimeUnit.SECONDS)
      .convertDurationsTo(TimeUnit.SECONDS)
      .build()
    reporter.start(10, TimeUnit.SECONDS)
  }

}
