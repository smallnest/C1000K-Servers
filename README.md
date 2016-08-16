![](https://travis-ci.org/smallnest/C1000K-Servers.svg?branch=master)

This project provides eight high performance websocket server implementation and can be used to test their benchmark.


The eight high performance websocket servers contain:
* [spray-websocket](https://github.com/wandoulabs/spray-websocket)
* [Netty](http://netty.io/)
* [Undertow](http://undertow.io/)
* [nodejs-websocket/Node.js](https://github.com/sitegui/nodejs-websocket)
* [Jetty](http://www.eclipse.org/jetty/)
* [Vert.x](http://http://vertx.io)
* [Grizzly](https://grizzly.java.net/)
* [Go](https://golang.org/)
* [Go-fasthttp](https://github.com/valyala/fasthttp)、[Go](https://github.com/fasthttp-contrib/websocket)

I have test them and wrote two posts about test results in Chinese:

[七种WebSocket框架的性能比较](http://colobu.com/2015/07/14/performance-comparison-of-7-websocket-frameworks/)
[使用四种框架分别实现百万websocket常连接的服务器](http://colobu.com/2015/05/22/implement-C1000K-servers-by-spray-netty-undertow-and-node-js/#comments)

The test result shows Netty, Go, Node.js, Undertow, Vert.x can setup 1 million connections. Netty and Go is best.

You can test them and update the test result by [submitting an issue](https://github.com/smallnest/C1000K-Servers/issues) or create a pull request.


### Compile
I wrote those websocket implementation in Scala, Go and Node.js. The code is simple but they are a basic websocket server prototype.
You can use them as a scaffold to develop chat, message pushing, notifications systems, etc.

You can run sbt task to create distributions for scala projects (netty, grizzly, jetty,spray,undertow,vert.x, testclient).

```sh
sbt clean dist
```

For goserver and go-fasthttp, you can run the "install" script to build the server.

Node.js is a javascript file.


### Deploy
For scala projects, you can copy target/universal/xxxxxx.zip to your server and unzip it.
The unzipped directory contains start script (at bin directory), configuration files (at conf directory) and libraries (at lib directory).

For Go projects, copy compiled binary file and config.json to your server.


For node.js, copy scripts and dependencies modules to your server.


### Configuration
One server can't support benchmark test for one million websocket connections.
There servers are recommended, for example, there AWS C3.8xlarge servers or better.

One server is used for a websocket server and two servers are for clients.


```sh
echo 2000000 > /proc/sys/fs/nr_open
echo 2000000 > /proc/sys/fs/file-max
ulimit -n 2000000
```

Modify `/etc/sysctl.conf` and add:

```
net.ipv4.ip_local_port_range = 1024  65000
```


and for Java, set memory options:
```
export JAVA_OPTS="-Xms12G -Xmx12G -Xss1M -XX:+UseParallelGC"
```

for node.js/V8:
```
node --nouse-idle-notification --expose-gc --max-new-space-size=1024 --max-new-space-size=2048 --max-old-space-size=8192 ./webserver.js
```

because one client/one IP only setup about 60,000 connections so you need set up more test IP, for example:
```sh
ifconfig eth0:1 192.168.77.11 netmask 255.255.255.0 up
ifconfig eth0:2 192.168.77.12 netmask 255.255.255.0 up
ifconfig eth0:3 192.168.77.13 netmask 255.255.255.0 up
……
ifconfig eth0:19 192.168.77.29 netmask 255.255.255.0 up
ifconfig eth0:20 192.168.77.30 netmask 255.255.255.0 up
```

Servers will send one message per minutes to all 1,200,000 connections. Each message only contains current time of this server. Clients can output metrics to monitor.

You can adjust those parameters in application.conf. Please refer to reference.conf.

For example, Netty uses the below parameters:
```
server.port = 8088
sending.timer.delay = 10 minutes
sending.timer.interval = 1 minutes
totalSize = 1200000
onlyTestConnect = true
```

It will send the first message after 10 minutes and will send messages per minute since then.
But if connections has not reached 1,200,000, it won't send messages to active connections.
`onlyTestConnect` is true means server won't send message and we only test websocket setup.


For testclient, you can adjust those below parameters:
```
local.ip = "0.0.0.0"
server.uri = "ws://localhost:8088"
setup.batchSize = 500
setup.interval = 1000
total.clients = 50000
websocket.timeout = 0
setup.threadpool.size = 1
```

You can change `local.ip` to multiple ip:
```
local.ip = "192.168.77.11,192.168.77.12,192.168.77.13,192.168.77.14,192.168.77.15,192.168.77.16,192.168.77.17,192.168.77.18,192.168.77.19,192.168.77.20,192.168.77.21,192.168.77.22,192.168.77.23,192.168.77.24,192.168.77.25,192.168.77.26,192.168.77.27,192.168.77.28,192.168.77.29,192.168.77.30"
```

and server uri:
```
server.uri = "ws://192.168.77.10:8088"
```

`total.clients` is max count of clients per IP.

