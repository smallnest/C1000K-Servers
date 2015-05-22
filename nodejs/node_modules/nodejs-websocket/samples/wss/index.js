var fs = require('fs')
var ws = require('../../')

// See http://www.nodejs.org/api/tls.html for info on how to create key and certificate files
var options = {
	secure: true,
	key: fs.readFileSync("key.pem"),
	cert: fs.readFileSync("cert.pem")
}

ws.createServer(options, function (conn) {
	conn.on("text", function (str) {
		conn.sendText(str.toUpperCase() + "!!!")
	})
}).listen(8001, "127.0.0.1")
