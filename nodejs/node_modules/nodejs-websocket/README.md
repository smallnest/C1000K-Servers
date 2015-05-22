# Nodejs Websocket
A nodejs module for websocket server and client

# How to use it
Install with `npm install nodejs-websocket` or put all files in a folder called "nodejs-websocket", and:
```javascript
var ws = require("nodejs-websocket")

// Scream server example: "hi" -> "HI!!!"
var server = ws.createServer(function (conn) {
	console.log("New connection")
	conn.on("text", function (str) {
		console.log("Received "+str)
		conn.sendText(str.toUpperCase()+"!!!")
	})
	conn.on("close", function (code, reason) {
		console.log("Connection closed")
	})
}).listen(8001)
```

Se other examples inside the folder samples

# ws
The main object, returned by `require("nodejs-websocket")`.

## ws.createServer([options], [callback])
Returns a new `Server` object.

The `options` is an optional object that will be handed to net.createServer() to create an ordinary socket.
If it has a property called "secure" with value `true`, tls.createServer() will be used instead.

The `callback` is a function which is automatically added to the `"connection"` event.

## ws.connect(URL, [options], [callback])
Returns a new `Connection` object, representing a websocket client connection

`URL` is a string with the format "ws://localhost:8000/chat" (the port can be omitted)

`options` is an object that will be passed to net.connect() (or tls.connect() if the protocol is "wss:").
The properties "host" and "port" will be read from the `URL`

`callback` will be added as "connect" listener

## ws.setBinaryFragmentation(bytes)
Sets the minimum size of a pack of binary data to send in a single frame (default: 512kiB)

## ws.setMaxBufferLength(bytes)
Set the maximum size the internal Buffer can grow (default: 2MiB)
If at any time it stays bigger than this, the connection will be closed with code 1009
This is a security measure, to avoid memory attacks

# Server
The class that represents a websocket server, much like a HTTP server

## server.listen(port, [host], [callback])
Starts accepting connections on a given `port` and `host`.

If the `host` is omitted, the server will accept connections directed to any IPv4 address (INADDR_ANY).

A `port` value of zero will assign a random port.

`callback` will be added as an listener for the `'listening'` event.

## server.socket
The underlying socket, returned by net.createServer or tls.createServer

## server.connections
An Array with all connected clients. It's useful for broadcasting a message:
```javascript
function broadcast(server, msg) {
	server.connections.forEach(function (conn) {
		conn.sendText(msg)
	})
}
```

## Event: 'listening()'
Emitted when the server has been bound after calling server.listen

## Event: 'close()'
Emitted when the server closes. Note that if connections exist, this event is not emitted until all connections are completely ended.

## Event: 'error(errObj)'
Emitted when an error occurs. The 'close' event will be called directly following this event.

## Event: 'connection(conn)'
Emitted when a new connection is made successfully (after the handshake have been completed). conn is an instance of Connection

# Connection
The class that represents a connection, either a client-created (accepted by a nodejs ws server) or client connection.
The websocket protocol has two types of data frames: text and binary.
Text frames are implemented as simple send function and receive event.
Binary frames are implemented as streams: when you receive binary data, you get a ReadableStream; to send binary data, you must ask for a WritableStream and write into it.
The binary data will be divided into frames and be sent over the socket.

You cannot send text data while sending binary data. If you try to do so, the connection will emit an "error" event

## connection.sendText(str, [callback])
Sends a given string to the other side. You can't send text data in the middle of a binary transmission.

`callback` will be added as a listener to write operation over the socket

## connection.beginBinary()
Asks the connection to begin transmitting binary data. Returns a WritableStream.
The binary transmission will end when the WritableStream finishes (like when you call .end on it)

## connection.sendBinary(data, [callback])
Sends a single chunk of binary data (like calling connection.beginBinary().end(data))

`callback` will be added as a listener to write operation over the socket

## connection.close([code, [reason]])
Starts the closing handshake (sends a close frame)

## connection.socket
The underlying net or tls socket

## connection.server
If the connection was accepted by a nodejs server, a reference to it will be saved here. null otherwise

## connection.readyState
One of these constants, representing the current state of the connection. Only an open connection can be used to send/receive data.
* connection.CONNECTING (waiting for handshake completion)
* connection.OPEN
* connection.CLOSING (waiting for the answer to a close frame)
* connection.CLOSED

## connection.outStream
Stores the OutStream object returned by connection.beginBinary(). null if there is no current binary data beeing sent.

## connection.path
For a connection accepted by a server, it is a string representing the path to which the connection was made (example: "/chat"). null otherwise

## connection.headers
Read only map of header names and values. Header names are lower-cased

## Event: 'close(code, reason)'
Emitted when the connection is closed by any side

## Event: 'error(err)'
Emitted in case of error (like trying to send text data while still sending binary data)

## Event: 'text(str)'
Emitted when a text is received. `str` is a string

## Event: 'binary(inStream)'
Emitted when the beginning of binary data is received. `inStream` is a ReadableStream

## Event: 'connect()'
Emitted when the connection is fully established (after the handshake)

