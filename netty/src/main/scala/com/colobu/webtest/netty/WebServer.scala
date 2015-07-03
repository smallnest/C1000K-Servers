package com.colobu.webtest.netty

import java.util.UUID
import java.util.concurrent.{TimeUnit, Executors}

import com.typesafe.scalalogging.LazyLogging
import io.netty.bootstrap.ServerBootstrap
import io.netty.buffer.{ByteBufAllocator, PooledByteBufAllocator}
import io.netty.channel._
import io.netty.channel.epoll.{EpollChannelOption, Epoll, EpollEventLoopGroup, EpollServerSocketChannel}
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.handler.codec.http._
import io.netty.handler.codec.http.websocketx._
import scala.collection.JavaConverters._
import io.netty.handler.codec.http.websocketx.extensions.compression.{WebSocketServerCompressionHandler, DeflateFrameServerExtensionHandshaker}

object WebServer extends App with LazyLogging{
  val bossGroup = if (Epoll.isAvailable()) new EpollEventLoopGroup() else new NioEventLoopGroup()
  val workerGroup = if (Epoll.isAvailable()) new EpollEventLoopGroup() else new NioEventLoopGroup()

  try {
    val bootstrap = new ServerBootstrap()
    bootstrap.group(bossGroup, workerGroup)
      .option[Integer](ChannelOption.SO_BACKLOG, 1024)
      .option[java.lang.Boolean](ChannelOption.SO_REUSEADDR, true)
      //.option[java.lang.Boolean](EpollChannelOption.SO_REUSEPORT, true)
      .option[Integer](ChannelOption.MAX_MESSAGES_PER_READ, Integer.MAX_VALUE)
      .childOption[ByteBufAllocator](ChannelOption.ALLOCATOR, new PooledByteBufAllocator(true))
      .childOption[java.lang.Boolean](ChannelOption.SO_REUSEADDR, true)
      .childOption[Integer](ChannelOption.MAX_MESSAGES_PER_READ, Integer.MAX_VALUE)

    if (Epoll.isAvailable())
      bootstrap.channel(classOf[EpollServerSocketChannel])
    else
      bootstrap.channel(classOf[NioServerSocketChannel])

    bootstrap.childHandler(new ChannelInitializer[SocketChannel]() {
      override def initChannel(socketChannel: SocketChannel): Unit = {
        val pipeline = socketChannel.pipeline()
        pipeline.addLast(new HttpServerCodec())
        pipeline.addLast(new HttpObjectAggregator(65536))
        pipeline.addLast(new WebSocketServerCompressionHandler())
        pipeline.addLast(new WebSocketServerHandler())
      }
    })

    val channel = bootstrap.bind(Common.port).sync().channel()
    if (!Common.onlyTestConnect) {
      Executors.newScheduledThreadPool(1).scheduleAtFixedRate(new Runnable {
        override def run(): Unit = {
          val flag = UUID.randomUUID().toString
          if (Common.clients.size() < Common.totalSize) {
            logger.info(s"current channels: ${Common.clients.size()} for $flag")
          } else {
            logger.info(s"send msg to channels for $flag")
            Common.clients.write(new TextWebSocketFrame(System.currentTimeMillis().toString))
//            Common.clients.asScala.par.foreach(c => {
//              c.write(new TextWebSocketFrame(System.currentTimeMillis().toString))
//              c.flush()
//            })
            logger.info(s"sent msg to channels for $flag. current channels: ${Common.clients.size}")
          }
        }
      }, Common.delay, Common.interval, TimeUnit.MINUTES)
    }
    logger.info("started")
    channel.closeFuture().sync()

  }
  finally {
    bossGroup.shutdownGracefully()
    workerGroup.shutdownGracefully()
    Common.clients.asScala.par.foreach(c => {
      c.close()
    })
  }
}

class WebSocketServerHandler extends SimpleChannelInboundHandler[Object] with LazyLogging {
  private var handshaker: WebSocketServerHandshaker = _

  override def channelReadComplete(ctx: ChannelHandlerContext) {
    ctx.flush()
  }


  override def messageReceived(ctx: ChannelHandlerContext, msg: Object): Unit = {
    msg match {
      case req: FullHttpRequest => handleHttpRequest(ctx, req)
      case m: WebSocketFrame => handleWebSocketFrame(ctx, m)
      case _ =>
    }
  }

  def handleWebSocketFrame(ctx: ChannelHandlerContext, frame: WebSocketFrame) {
    frame match {
      case f: CloseWebSocketFrame => ctx.channel().write(new CloseWebSocketFrame())
      case f: PingWebSocketFrame => ctx.channel().write(new PongWebSocketFrame(frame.content().retain()))
      case f: TextWebSocketFrame => logger.debug(s"${f.text}")
      case _ => throw new UnsupportedOperationException()
    }
  }

  def handleHttpRequest(ctx: ChannelHandlerContext, req: FullHttpRequest): Unit = {
    val wsFactory = new WebSocketServerHandshakerFactory(getWebSocketLocation(req), null, true)
    handshaker = wsFactory.newHandshaker(req)
    if (handshaker == null) {
      WebSocketServerHandshakerFactory.sendUnsupportedVersionResponse(ctx.channel())
    } else {
      handshaker.handshake(ctx.channel(), req).addListener(new ChannelFutureListener() {
        override def operationComplete(future: ChannelFuture): Unit = {
          if (!Common.onlyTestConnect) {
            if (future.isSuccess()) {
              Common.clients.add(ctx.channel())
              logger.debug("added channel:" + ctx.channel().remoteAddress())
            }
          }
        }
      })
    }

  }

  override def handlerRemoved(ctx: ChannelHandlerContext): Unit = {
    Common.clients.remove(ctx.channel())
    logger.debug("removed channel:" + ctx.channel().remoteAddress())
  }

  def getWebSocketLocation(req: FullHttpRequest): String = {
    val location = req.headers().get(io.netty.handler.codec.http.HttpHeaderNames.HOST)
    "ws://" + location
  }

}