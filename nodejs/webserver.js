var ws = require("nodejs-websocket")

var server = ws.createServer(function (conn) {
    //console.log("New connection")
    conn.on("text", function (str) {
        //console.log("Received "+str)
        //conn.sendText(str.toUpperCase()+"!!!")
    })
    conn.on("close", function (code, reason) {
        //console.log("Connection closed")
    })
}).listen(8088)

function broadcast() {
	if (server.connections.length >= 1000000) {
		console.log(new Date().getTime() + ": send msg to current connections")
		server.connections.forEach(function (conn) {
			conn.sendText(new Date().getTime() + "")
		})
		console.log(new Date().getTime() + ": sent msg to current connections:" + server.connections.length)
	} else {
		console.log(new Date().getTime() +": current connections:" + server.connections.length)
	}
}

function send() {
	setInterval(broadcast, 60 * 1000)
}

setTimeout(send, 10 * 60 * 1000);

console.log("started")