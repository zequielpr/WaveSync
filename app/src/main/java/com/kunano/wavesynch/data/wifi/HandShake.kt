package com.kunano.wavesynch.data.wifi

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class HandShake(
    val appIdentifier: String,
    val userId: String,
    val deviceName: String,
    val protocolVersion: Int,
    var response: Int?= null


)





sealed class HandShakeResult(val intValue: Int) {
    data class Success(val handShake: HandShake? = null): HandShakeResult(1) //1
    data class  InvalidHandshake(val handShake: HandShake? = null) : HandShakeResult(2)
    data class  InvalidProtocol(val handShake: HandShake? = null) : HandShakeResult(3)
    data class  InvalidAppId(val handShake: HandShake? = null) : HandShakeResult(4)
    data class  InvalidUserId(val handShake: HandShake? = null) : HandShakeResult(5)
    data class  DeclinedByHost(val handShake: HandShake? = null) : HandShakeResult(5)
    data class HostApprovalRequired(val handShake: HandShake? = null) : HandShakeResult(7)
    data class Error(val message: String) : HandShakeResult(8)
    data  object None : HandShakeResult(9)
}



fun parseHandshake(json: String): HandShake {
    return Json.decodeFromString(json)
}

fun serializeHandshake(handshake: HandShake): String {
    return Json.encodeToString(HandShake.serializer(), handshake)
}





