package com.kunano.wavesynch.data.wifi.server

import com.kunano.wavesynch.CrashReporter
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

@Serializable
data class HandShake(
    val appIdentifier: String,
    val userId: String,
    val deviceName: String,
    val roomName: String? = null,
    val protocolVersion: Int,
    var response: Int? = null
)

open class HandShakeResult(val intValue: Int) {
    data class Success(val handShake: HandShake? = null) : HandShakeResult(1)
    data class InvalidHandshake(val handShake: HandShake? = null) : HandShakeResult(2)
    data class InvalidProtocol(val handShake: HandShake? = null) : HandShakeResult(3)
    data class InvalidAppId(val handShake: HandShake? = null) : HandShakeResult(4)
    data class InvalidUserId(val handShake: HandShake? = null) : HandShakeResult(5)
    data class DeclinedByHost(val handShake: HandShake? = null) : HandShakeResult(6)
    data class HostApprovalRequired(val handShake: HandShake? = null) : HandShakeResult(7)
    data class Error(val message: String) : HandShakeResult(8)
    data class UdpSocketOpen(val handShake: HandShake? = null) : HandShakeResult(9)
    data class UdpSocketClosed(val handShake: HandShake? = null) : HandShakeResult(10)
    data class ExpelledByHost(val handShake: HandShake? = null) : HandShakeResult(11)
    data class GuestLeftRoom(val handShake: HandShake? = null) : HandShakeResult(12)
    data class RoomFull(val handShake: HandShake? = null) : HandShakeResult(13)
    object None : HandShakeResult(14)
}

fun parseHandshake(json: String): HandShake? {
    return try {
        Json.decodeFromString(json)
    } catch (e: SerializationException) {
        CrashReporter.set("operation_tag", "parse_handshake_serialization")
        CrashReporter.record(e)
        null
    } catch (e: IllegalArgumentException) {
        CrashReporter.set("operation_tag", "parse_handshake_illegal_argument")
        CrashReporter.record(e)
        null
    }
}

fun serializeHandshake(handshake: HandShake): String {
    return try {
        Json.encodeToString(HandShake.serializer(), handshake)
    } catch (e: SerializationException) {
        CrashReporter.set("operation_tag", "serialize_handshake_serialization")
        CrashReporter.record(e)
        ""
    } catch (e: IllegalArgumentException) {
        CrashReporter.set("operation_tag", "serialize_handshake_illegal_argument")
        CrashReporter.record(e)
        ""
    }
}