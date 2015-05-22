/**
 * @file Simple wrapper for stream.Readable, used for receiving binary data
 */
'use strict'

var util = require('util'),
	stream = require('stream')

/**
 * Represents the readable stream for binary frames
 * @class
 * @event readable
 * @event end
 */
function InStream() {
	stream.Readable.call(this)
}

module.exports = InStream

util.inherits(InStream, stream.Readable)

/**
 * No logic here, the pushs are made outside _read
 * @private
 */
InStream.prototype._read = function () {}

/**
 * Add more data to the stream and fires "readable" event
 * @param {Buffer} data
 * @private
 */
InStream.prototype.addData = function (data) {
	this.push(data)
}

/**
 * Indicates there is no more data to add to the stream
 * @private
 */
InStream.prototype.end = function () {
	this.push(null)
}