package com.littletrickster.androidrsocketdemo

import io.rsocket.kotlin.RSocket
import io.rsocket.kotlin.metadata.RoutingMetadata
import io.rsocket.kotlin.metadata.metadata
import io.rsocket.kotlin.payload.buildPayload
import io.rsocket.kotlin.payload.data
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.SocketException

const val CONNECTION_COUNT = "connection count"
const val CURRENT_COLOR = "current color"
const val COUNTER = "counter"


fun getLocalIpAddress(): String? {
    try {
        val en = NetworkInterface.getNetworkInterfaces()
        while (en.hasMoreElements()) {
            val intf = en.nextElement()
            val enumIpAddr = intf.inetAddresses
            while (enumIpAddr.hasMoreElements()) {
                val inetAddress = enumIpAddr.nextElement()
                if (!inetAddress.isLoopbackAddress && inetAddress is Inet4Address) {
                    return inetAddress.hostAddress
                }
            }
        }
    } catch (ex: SocketException) {
        ex.printStackTrace()
    }
    return null
}

suspend fun RSocket.fireAndForget(destination: String, data: String) =
    fireAndForget(buildPayload {
        metadata(RoutingMetadata(destination))
        data(data)
    })

suspend fun RSocket.requestResponse(destination: String) =
    requestResponse(buildPayload {
        metadata(RoutingMetadata(destination))
        data("")
    })

suspend fun RSocket.requestResponse(destination: String, data: String) =
    requestResponse(buildPayload {
        metadata(RoutingMetadata(destination))
        data(data)
    })

fun RSocket.requestStream(destination: String) =
    requestStream(buildPayload {
        metadata(RoutingMetadata(destination))
        data("")
    })

fun RSocket.requestStream(destination: String, data: String) =
    requestStream(buildPayload {
        metadata(RoutingMetadata(destination))
        data(data)
    })
