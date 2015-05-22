This project implements four high performance websocket servers.
It contains:
* [spray-websocket](https://github.com/wandoulabs/spray-websocket)
* [Netty](http://netty.io/)
* [Undertow](http://undertow.io/)
* [nodejs-websocket/Node.js](https://github.com/sitegui/nodejs-websocket)

And it also contains one client to test those servers.

I have test them on AWS servers. One *C3.4xlarge* as the websocket server and two *C3.2xlarge* servers as clients.
Basically servers use 12G memory and each client uses 1G memory.
I have tuned those servers to support more than 1,000,000 connections. For example, add the below lines into `/etc/sysctl.conf`
```
fs.file-max = 9999999 
fs.nr_open = 9999999  
net.ipv4.ip_local_port_range = 1024 65535
```

Each client can create 60000 connections (<65535) on each IP so you should create multiple IP addresses on one AWS for clients.
I have created 10 virtual internal IP addresses on each client.

Servers will send one message per minutes to all 1,200,000 connections. Each message only contains current time of this server. Clients can output metrics to monitor.
Currently servers only use one thread to send messages to all websockets. If we use multiple threads to send, maybe we can get higher performance.

Ine one word, it is feasible to achieve 1,000,000 connections by popular frameworks. 
