package com.littletrickster.androidrsocketdemo.server

import android.content.Context
import com.littletrickster.androidrsocketdemo.CONNECTION_COUNT
import com.littletrickster.androidrsocketdemo.COUNTER
import com.littletrickster.androidrsocketdemo.CURRENT_COLOR
import com.littletrickster.androidrsocketdemo.GlobalSquareColor
import com.littletrickster.androidrsocketdemo.square.UUIDAndMySquareColors
import io.ktor.application.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.websocket.*
import io.rsocket.kotlin.ConnectionAcceptor
import io.rsocket.kotlin.RSocketRequestHandler
import io.rsocket.kotlin.core.RSocketServer
import io.rsocket.kotlin.metadata.RoutingMetadata
import io.rsocket.kotlin.metadata.read
import io.rsocket.kotlin.payload.Payload
import io.rsocket.kotlin.payload.buildPayload
import io.rsocket.kotlin.payload.data
import io.rsocket.kotlin.transport.ktor.server.RSocketSupport
import io.rsocket.kotlin.transport.ktor.server.rSocket
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.coroutines.coroutineContext

class MyKtorServer(
    val context: Context,
    val globalSquareColor: GlobalSquareColor
) {

    var ktorServer: ApplicationEngine? = null


    var connection = MutableSharedFlow<Boolean>()

    var connectedDeviceCount = connection.subscriptionCount


    fun Payload.route(): String =
        metadata?.read(RoutingMetadata)?.tags?.first() ?: error("No route provided")

    val incrementalFlow = MutableSharedFlow<Int>(1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    var counterJob: Job? = null

    suspend fun runServer() {
        counterJob?.cancel()
        counterJob = GlobalScope.launch {
            var i = 0
            while (true) {
                incrementalFlow.emit(i++)
                delay(1000)
            }
        }


        val acceptor = ConnectionAcceptor {
            val scope = CoroutineScope(coroutineContext)

            connection.launchIn(scope)


            RSocketRequestHandler {

                fireAndForget {

                    when (it.route()) {
                        "sendColor" -> {
                            val receivedColor =
                                Json.decodeFromString<UUIDAndMySquareColors>(it.data.readText())
                            globalSquareColor.colorFlow.tryEmit(receivedColor)
                        }
                        else -> error("route not found")
                    }
                }

                requestResponse {
                    when (it.route()) {
                        else -> error("route not found")
                    }


                }

                requestStream {
                    when (it.route()) {

                        CONNECTION_COUNT -> {
                            connectedDeviceCount.map {
                                buildPayload {
                                    data(it.toString())
                                }
                            }
                        }
                        CURRENT_COLOR -> {
                            globalSquareColor.distinctColor.map {
                                buildPayload {
                                    data(Json.encodeToString(it))
                                }
                            }
                        }
                        COUNTER -> {
                            incrementalFlow.map {
                                buildPayload {
                                    data(it.toString())
                                }
                            }
                        }

                        else -> error("route not found")
                    }
                }
            }
        }

        ktorServer = embeddedServer(CIO, port = 8000)
        {
            install(WebSockets)
            install(RSocketSupport) {
                server = RSocketServer {
                }
            }

            routing {
                get("/") {
                    call.respondText("Hello, world!")
                }

                rSocket("rsocket", acceptor = acceptor)

            }

        }.start()


    }


    suspend fun stopServer() {
        ktorServer?.stop(1000, 2000)
        counterJob?.cancel()
    }

}

