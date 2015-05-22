/**
 * @file Represents a websocket server
 */
'use strict'

function nop() {}

var util = require('util'),
	net = require('net'),
	tls = require('tls'),
	events = require('events'),
	Connection

/**
 * Creates a new ws server and starts listening for new connections
 * @class
 * @param {boolean} secure indicates if it should use tls
 * @param {Object} [options] will be passed to net.createServer() or tls.createServer()
 * @param {Function} [callback] will be added as "connection" listener
 * @inherits EventEmitter
 * @event listening
 * @event close
 * @event error an error object is passed
 * @event connection a Connection object is passed
 */
function Server(secure, options, callback) {
	var that = this

	if (typeof options === 'function') {
		callback = options
		options = undefined
	}

	var onConnection = function (socket) {
		var conn = new Connection(socket, that, function () {
			that.connections.push(conn)
			conn.removeListener('error', nop)
			that.emit('connection', conn)
		})
		conn.on('close', function () {
			var pos = that.connections.indexOf(conn)
			if (pos !== -1) {
				that.connections.splice(pos, 1)
			}
		})

		// Ignore errors before the connection is established
		conn.on('error', nop)
	}

	if (secure) {
		this.socket = tls.createServer(options, onConnection)
	} else {
		this.socket = net.createServer(options, onConnection)
	}

	this.socket.on('close', function () {
		that.emit('close')
	})
	this.socket.on('error', function (err) {
		that.emit('error', err)
	})
	this.connections = []

	// super constructor
	events.EventEmitter.call(this)
	if (callback) {
		this.on('connection', callback)
	}
}

util.inherits(Server, events.EventEmitter)
module.exports = Server

Connection = require('./Connection')

/**
 * Start listening for connections
 * @param {number} port
 * @param {string} [host]
 * @param {Function} [callback] will be added as "connection" listener
 */
Server.prototype.listen = function (port, host, callback) {
	var that = this

	if (typeof host === 'function') {
		callback = host
		host = undefined
	}

	if (callback) {
		this.on('listening', callback)
	}

	this.socket.listen(port, host, function () {
		that.emit('listening')
	})

	return this
}