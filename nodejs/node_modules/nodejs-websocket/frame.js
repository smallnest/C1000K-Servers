/**
 * @file Utility functions for creating frames
 */
'use strict'

/**
 * Creates a text frame
 * @param {string} data
 * @param {boolean} [masked=false] if the frame should be masked
 * @returns {Buffer}
 * @private
 */
exports.createTextFrame = function (data, masked) {
	var payload, meta

	payload = new Buffer(data)
	meta = generateMetaData(true, 1, masked === undefined ? false : masked, payload)

	return Buffer.concat([meta, payload], meta.length + payload.length)
}

/**
 * Create a binary frame
 * @param {Buffer} data
 * @param {boolean} [masked=false] if the frame should be masked
 * @param {boolean} [first=true] if this is the first frame in a sequence
 * @param {boolean} [fin=true] if this is the final frame in a sequence
 * @returns {Buffer}
 * @private
 */
exports.createBinaryFrame = function (data, masked, first, fin) {
	var payload, meta

	first = first === undefined ? true : first
	masked = masked === undefined ? false : masked
	if (masked) {
		payload = new Buffer(data.length)
		data.copy(payload)
	} else {
		payload = data
	}
	meta = generateMetaData(fin === undefined ? true : fin, first ? 2 : 0, masked, payload)

	return Buffer.concat([meta, payload], meta.length + payload.length)
}

/**
 * Create a close frame
 * @param {number} code
 * @param {string} [reason='']
 * @param {boolean} [masked=false] if the frame should be masked
 * @returns {Buffer}
 * @private
 */
exports.createCloseFrame = function (code, reason, masked) {
	var payload, meta

	if (code !== undefined && code !== 1005) {
		payload = new Buffer(reason === undefined ? '--' : '--' + reason)
		payload.writeUInt16BE(code, 0)
	} else {
		payload = new Buffer(0)
	}
	meta = generateMetaData(true, 8, masked === undefined ? false : masked, payload)

	return Buffer.concat([meta, payload], meta.length + payload.length)
}

/**
 * Create a ping frame
 * @param {boolean} [masked=false] if the frame should be masked
 * @returns {Buffer}
 * @private
 */
exports.createPingFrame = function (masked) {
	return generateMetaData(true, 9, masked === undefined ? false : masked, new Buffer(0))
}

/**
 * Create a pong frame
 * @param {string} data
 * @param {boolean} [masked=false] if the frame should be masked
 * @returns {Buffer}
 * @private
 */
exports.createPongFrame = function (data, masked) {
	var payload, meta

	payload = new Buffer(data)
	meta = generateMetaData(true, 10, masked === undefined ? false : masked, payload)

	return Buffer.concat([meta, payload], meta.length + payload.length)
}

/**
 * Creates the meta-data portion of the frame
 * If the frame is masked, the payload is altered accordingly
 * @param {boolean} fin
 * @param {number} opcode
 * @param {boolean} masked
 * @param {Buffer} payload
 * @returns {Buffer}
 * @private
 */
function generateMetaData(fin, opcode, masked, payload) {
	var len, meta, start, mask, i

	len = payload.length

	// Creates the buffer for meta-data
	meta = new Buffer(2 + (len < 126 ? 0 : (len < 65536 ? 2 : 8)) + (masked ? 4 : 0))

	// Sets fin and opcode
	meta[0] = (fin ? 128 : 0) + opcode

	// Sets the mask and length
	meta[1] = masked ? 128 : 0
	start = 2
	if (len < 126) {
		meta[1] += len
	} else if (len < 65536) {
		meta[1] += 126
		meta.writeUInt16BE(len, 2)
		start += 2
	} else {
		// Warning: JS doesn't support integers greater than 2^53
		meta[1] += 127
		meta.writeUInt32BE(Math.floor(len / Math.pow(2, 32)), 2)
		meta.writeUInt32BE(len % Math.pow(2, 32), 6)
		start += 8
	}

	// Set the mask-key
	if (masked) {
		mask = new Buffer(4)
		for (i = 0; i < 4; i++) {
			meta[start + i] = mask[i] = Math.floor(Math.random() * 256)
		}
		for (i = 0; i < payload.length; i++) {
			payload[i] ^= mask[i % 4]
		}
		start += 4
	}

	return meta
}