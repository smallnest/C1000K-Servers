/**
 * @file Represents a connection (both client and server sides)
 */
'use strict'

var util = require('util'),
	events = require('events'),
	crypto = require('crypto'),
	InStream = require('./InStream'),
	OutStream = require('./OutStream'),
	frame = require('./frame'),
	Server = require('./Server')

/**
 * @class
 * @param {(net.Socket|tls.CleartextStream)} socket a net or tls socket
 * @param {(Server|{path:string,host:string})} parentOrUrl parent in case of server-side connection, url object in case of client-side
 * @param {Function} [callback] will be added as a listener to 'connect'
 * @inherits EventEmitter
 * @event close the numeric code and string reason will be passed
 * @event error an error object is passed
 * @event text a string is passed
 * @event binary a inStream object is passed
 * @event connect
 */
function Connection(socket, parentOrUrl, callback) {
	var that = this,
		connectEvent

	if (parentOrUrl instanceof Server) {
		// Server-side connection
		this.server = parentOrUrl
		this.path = null
		this.host = null
	} else {
		// Client-side
		this.server = null
		this.path = parentOrUrl.path
		this.host = parentOrUrl.host
	}

	this.socket = socket
	this.readyState = this.CONNECTING
	this.buffer = new Buffer(0)
	this.frameBuffer = null // string for text frames and InStream for binary frames
	this.outStream = null // current allocated OutStream object for sending binary frames
	this.key = null // the Sec-WebSocket-Key header
	this.headers = {} // read only map of header names and values. Header names are lower-cased

	// Set listeners
	socket.on('readable', function () {
		that.doRead()
	})

	socket.on('error', function (err) {
		that.emit('error', err)
	})

	if (!this.server) {
		connectEvent = socket.constructor.name === 'CleartextStream' ? 'secureConnect' : 'connect'
		socket.on(connectEvent, function () {
			that.startHandshake()
		})
	}

	// Close listeners
	var onclose = function () {
		if (that.readyState === that.CONNECTING || that.readyState === that.OPEN) {
			that.emit('close', 1006, '')
		}
		that.readyState = this.CLOSED
		if (that.frameBuffer instanceof InStream) {
			that.frameBuffer.end()
			that.frameBuffer = null
		}
		if (that.outStream instanceof OutStream) {
			that.outStream.end()
			that.outStream = null
		}
	}
	socket.once('close', onclose)
	socket.once('finish', onclose)

	// super constructor
	events.EventEmitter.call(this)
	if (callback) {
		this.once('connect', callback)
	}
}

util.inherits(Connection, events.EventEmitter)
module.exports = Connection

/**
 * Minimum size of a pack of binary data to send in a single frame
 * @property {number} binaryFragmentation
 */
Connection.binaryFragmentation = 512 * 1024 // .5 MiB

/**
 * The maximum size the internal Buffer can grow
 * If at any time it stays bigger than this, the connection will be closed with code 1009
 * This is a security measure, to avoid memory attacks
 * @property {number} maxBufferLength
 */
Connection.maxBufferLength = 2 * 1024 * 1024 // 2 MiB

/**
 * Possible ready states for the connection
 * @constant {number} CONNECTING
 * @constant {number} OPEN
 * @constant {number} CLOSING
 * @constant {number} CLOSED
 */
Connection.prototype.CONNECTING = 0
Connection.prototype.OPEN = 1
Connection.prototype.CLOSING = 2
Connection.prototype.CLOSED = 3

/**
 * Send a given string to the other side
 * @param {string} str
 * @param {Function} [callback] will be executed when the data is finally written out
 */
Connection.prototype.sendText = function (str, callback) {
	if (this.readyState === this.OPEN) {
		if (!this.outStream) {
			return this.socket.write(frame.createTextFrame(str, !this.server), callback)
		}
		this.emit('error', new Error('You can\'t send a text frame until you finish sending binary frames'))
	}
	this.emit('error', new Error('You can\'t write to a non-open connection'))
}

/**
 * Request for a OutStream to send binary data
 * @returns {OutStream}
 */
Connection.prototype.beginBinary = function () {
	if (this.readyState === this.OPEN) {
		if (!this.outStream) {
			return (this.outStream = new OutStream(this, Connection.binaryFragmentation))
		}
		this.emit('error', new Error('You can\'t send more binary frames until you finish sending the previous binary frames'))
	}
	this.emit('error', new Error('You can\'t write to a non-open connection'))
}

/**
 * Sends a binary buffer at once
 * @param {Buffer} data
 * @param {Function} [callback] will be executed when the data is finally written out
 */
Connection.prototype.sendBinary = function (data, callback) {
	if (this.readyState === this.OPEN) {
		if (!this.outStream) {
			return this.socket.write(frame.createBinaryFrame(data, !this.server, true, true), callback)
		}
		this.emit('error', new Error('You can\'t send more binary frames until you finish sending the previous binary frames'))
	}
	this.emit('error', new Error('You can\'t write to a non-open connection'))
}

/**
 * Close the connection, sending a close frame and waiting for response
 * If the connection isn't OPEN, closes it without sending a close frame
 * @param {number} [code]
 * @param {string} [reason]
 * @fires close
 */
Connection.prototype.close = function (code, reason) {
	if (this.readyState === this.OPEN) {
		this.socket.write(frame.createCloseFrame(code, reason, !this.server))
		this.readyState = this.CLOSING
	} else if (this.readyState !== this.CLOSED) {
		this.socket.end()
		this.readyState = this.CLOSED
	}
	this.emit('close', code, reason)
}

/**
 * Reads contents from the socket and process it
 * @fires connect
 * @private
 */
Connection.prototype.doRead = function () {
	var buffer, temp

	// Fetches the data
	buffer = this.socket.read()
	if (!buffer) {
		// Waits for more data
		return
	}

	// Save to the internal buffer
	this.buffer = Buffer.concat([this.buffer, buffer], this.buffer.length + buffer.length)

	if (this.readyState === this.CONNECTING) {
		if (!this.readHandshake()) {
			// May have failed or we're waiting for more data
			return
		}
	}

	if (this.readyState !== this.CLOSED) {
		// Try to read as many frames as possible
		while ((temp = this.extractFrame()) === true) {}
		if (temp === false) {
			// Protocol error
			this.close(1002)
		} else if (this.buffer.length > Connection.maxBufferLength) {
			// Frame too big
			this.close(1009)
		}
	}
}

/**
 * Create and send a handshake as a client
 * @private
 */
Connection.prototype.startHandshake = function () {
	var str, i, key
	key = new Buffer(16)
	for (i = 0; i < 16; i++) {
		key[i] = Math.floor(Math.random() * 256)
	}
	this.key = key.toString('base64')
	str = 'GET ' + this.path + ' HTTP/1.1\r\n' +
		'Host: ' + this.host + '\r\n' +
		'Upgrade: websocket\r\n' +
		'Connection: Upgrade\r\n' +
		'Sec-WebSocket-Key: ' + this.key + '\r\n' +
		'Sec-WebSocket-Version: 13\r\n\r\n'
	this.socket.write(str)
}

/**
 * Try to read the handshake from the internal buffer
 * If it succeeds, the handshake data is consumed from the internal buffer
 * @returns {boolean} - whether the handshake was done
 * @private
 */
Connection.prototype.readHandshake = function () {
	var found = false,
		i, data

	// Do the handshake and try to connect
	if (this.buffer.length > Connection.maxBufferLength) {
		// Too big for a handshake
		this.socket.end(this.server ? 'HTTP/1.1 400 Bad Request\r\n\r\n' : undefined)
		return false
	}

	// Search for '\r\n\r\n'
	for (i = 0; i < this.buffer.length - 3; i++) {
		if (this.buffer[i] === 13 && this.buffer[i + 2] === 13 &&
			this.buffer[i + 1] === 10 && this.buffer[i + 3] === 10) {
			found = true
			break
		}
	}
	if (!found) {
		// Wait for more data
		return false
	}
	data = this.buffer.slice(0, i + 4).toString().split('\r\n')
	if (this.server ? this.answerHandshake(data) : this.checkHandshake(data)) {
		this.buffer = this.buffer.slice(i + 4)
		this.readyState = this.OPEN
		this.emit('connect')
		return true
	} else {
		this.socket.end(this.server ? 'HTTP/1.1 400 Bad Request\r\n\r\n' : undefined)
		return false
	}
}

/**
 * Read headers from HTTP protocol
 * Update the Connection#headers property
 * @param {string[]} lines one for each '\r\n'-separated HTTP request line
 * @private
 */
Connection.prototype.readHeaders = function (lines) {
	var i, match

	// Extract all headers
	// Ignore bad-formed lines and ignore the first line (HTTP header)
	for (i = 1; i < lines.length; i++) {
		if ((match = lines[i].match(/^([a-z-]+): (.+)$/i))) {
			this.headers[match[1].toLowerCase()] = match[2]
		}
	}
}

/**
 * Process and check a handshake answered by a server
 * @param {string[]} lines one for each '\r\n'-separated HTTP request line
 * @returns {boolean} if the handshake was sucessful. If not, the connection must be closed
 * @private
 */
Connection.prototype.checkHandshake = function (lines) {
	var key, sha1

	// First line
	if (lines.length < 4) {
		return false
	}
	if (!lines[0].match(/^HTTP\/\d\.\d 101( .*)?$/i)) {
		return false
	}

	// Extract all headers
	this.readHeaders(lines)

	// Validate necessary headers
	if (!('upgrade' in this.headers) ||
		!('sec-websocket-accept' in this.headers) ||
		!('connection' in this.headers)) {
		return false
	}
	if (this.headers.upgrade.toLowerCase() !== 'websocket' ||
		this.headers.connection.toLowerCase().split(', ').indexOf('upgrade') === -1) {
		return false
	}
	key = this.headers['sec-websocket-accept']

	// Check the key
	sha1 = crypto.createHash('sha1')
	sha1.end(this.key + '258EAFA5-E914-47DA-95CA-C5AB0DC85B11')
	if (key !== sha1.read().toString('base64')) {
		return false
	}
	return true
}

/**
 * Process and answer a handshake started by a client
 * @param {string[]} lines one for each '\r\n'-separated HTTP request line
 * @returns {boolean} if the handshake was sucessful. If not, the connection must be closed with error 400-Bad Request
 * @private
 */
Connection.prototype.answerHandshake = function (lines) {
	var path, key, sha1

	// First line
	if (lines.length < 6) {
		return false
	}
	path = lines[0].match(/^GET (.+) HTTP\/\d\.\d$/i)
	if (!path) {
		return false
	}
	this.path = path[1]

	// Extract all headers
	this.readHeaders(lines)

	// Validate necessary headers
	if (!('host' in this.headers) ||
		!('sec-websocket-key' in this.headers) ||
		!('upgrade' in this.headers) ||
		!('connection' in this.headers)) {
		return false
	}
	if (this.headers.upgrade.toLowerCase() !== 'websocket' ||
		this.headers.connection.toLowerCase().split(', ').indexOf('upgrade') === -1) {
		return false
	}
	if (this.headers['sec-websocket-version'] !== '13') {
		return false
	}

	this.key = this.headers['sec-websocket-key']

	// Build and send the response
	sha1 = crypto.createHash('sha1')
	sha1.end(this.key + '258EAFA5-E914-47DA-95CA-C5AB0DC85B11')
	key = sha1.read().toString('base64')
	this.socket.write('HTTP/1.1 101 Switching Protocols\r\n' +
		'Upgrade: websocket\r\n' +
		'Connection: Upgrade\r\n' +
		'Sec-WebSocket-Accept: ' + key + '\r\n\r\n')
	return true
}

/**
 * Try to extract frame contents from the buffer (and execute it)
 * @returns {(boolean|undefined)} false=something went wrong (the connection must be closed); undefined=there isn't enough data to catch a frame; true=the frame was successfully fetched and executed
 * @private
 */
Connection.prototype.extractFrame = function () {
	var fin, opcode, B, HB, mask, len, payload, start, i, hasMask

	if (this.buffer.length < 2) {
		return
	}

	// Is this the last frame in a sequence?
	B = this.buffer[0]
	HB = B >> 4
	if (HB % 8) {
		// RSV1, RSV2 and RSV3 must be clear
		return false
	}
	fin = HB === 8
	opcode = B % 16

	if (opcode !== 0 && opcode !== 1 && opcode !== 2 &&
		opcode !== 8 && opcode !== 9 && opcode !== 10) {
		// Invalid opcode
		return false
	}
	if (opcode >= 8 && !fin) {
		// Control frames must not be fragmented
		return false
	}

	B = this.buffer[1]
	hasMask = B >> 7
	if ((this.server && !hasMask) || (!this.server && hasMask)) {
		// Frames sent by clients must be masked
		return false
	}
	len = B % 128
	start = hasMask ? 6 : 2

	if (this.buffer.length < start + len) {
		// Not enough data in the buffer
		return
	}

	// Get the actual payload length
	if (len === 126) {
		len = this.buffer.readUInt16BE(2)
		start += 2
	} else if (len === 127) {
		// Warning: JS can only store up to 2^53 in its number format
		len = this.buffer.readUInt32BE(2) * Math.pow(2, 32) + this.buffer.readUInt32BE(6)
		start += 8
	}
	if (this.buffer.length < start + len) {
		return
	}

	// Extract the payload
	payload = this.buffer.slice(start, start + len)
	if (hasMask) {
		// Decode with the given mask
		mask = this.buffer.slice(start - 4, start)
		for (i = 0; i < payload.length; i++) {
			payload[i] ^= mask[i % 4]
		}
	}
	this.buffer = this.buffer.slice(start + len)

	// Proceeds to frame processing
	return this.processFrame(fin, opcode, payload)
}

/**
 * Process a given frame received
 * @param {boolean} fin
 * @param {number} opcode
 * @param {Buffer} payload
 * @returns {boolean} false if any error occurs, true otherwise
 * @fires text
 * @fires binary
 * @private
 */
Connection.prototype.processFrame = function (fin, opcode, payload) {
	if (opcode === 8) {
		// Close frame
		if (this.readyState === this.CLOSING) {
			this.socket.end()
		} else if (this.readyState === this.OPEN) {
			this.processCloseFrame(payload)
		}
		return true
	} else if (opcode === 9) {
		// Ping frame
		if (this.readyState === this.OPEN) {
			this.socket.write(frame.createPongFrame(payload.toString(), !this.server))
		}
		return true
	} else if (opcode === 10) {
		// Pong frame
		return true
	}

	if (this.readyState !== this.OPEN) {
		// Ignores if the connection isn't opened anymore
		return true
	}

	if (opcode === 0 && this.frameBuffer === null) {
		// Unexpected continuation frame
		return false
	} else if (opcode !== 0 && this.frameBuffer !== null) {
		// Last sequence didn't finished correctly
		return false
	}

	if (!opcode) {
		// Get the current opcode for fragmented frames
		opcode = typeof this.frameBuffer === 'string' ? 1 : 2
	}

	if (opcode === 1) {
		// Save text frame
		payload = payload.toString()
		this.frameBuffer = this.frameBuffer ? this.frameBuffer + payload : payload

		if (fin) {
			// Emits 'text' event
			this.emit('text', this.frameBuffer)
			this.frameBuffer = null
		}
	} else {
		// Sends the buffer for InStream object
		if (!this.frameBuffer) {
			// Emits the 'binary' event
			this.frameBuffer = new InStream
			this.emit('binary', this.frameBuffer)
		}
		this.frameBuffer.addData(payload)

		if (fin) {
			// Emits 'end' event
			this.frameBuffer.end()
			this.frameBuffer = null
		}
	}

	return true
}

/**
 * Process a close frame, emitting the close event and sending back the frame
 * @param {Buffer} payload
 * @fires close
 * @private
 */
Connection.prototype.processCloseFrame = function (payload) {
	var code, reason
	if (payload.length >= 2) {
		code = payload.readUInt16BE(0)
		reason = payload.slice(2).toString()
	} else {
		code = 1005
		reason = ''
	}
	this.socket.write(frame.createCloseFrame(code, reason, !this.server))
	this.readyState = this.CLOSED
	this.emit('close', code, reason)
}