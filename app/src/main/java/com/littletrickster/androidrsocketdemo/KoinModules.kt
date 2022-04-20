package com.littletrickster.androidrsocketdemo

import com.littletrickster.androidrsocketdemo.fservice.ServiceLauncher
import com.littletrickster.androidrsocketdemo.mainactivity.MainActivityViewModel
import com.littletrickster.androidrsocketdemo.server.MyKtorServer
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.websocket.*
import io.rsocket.kotlin.core.RSocketConnector
import io.rsocket.kotlin.core.WellKnownMimeType
import io.rsocket.kotlin.ktor.client.RSocketSupport
import io.rsocket.kotlin.payload.PayloadMimeType

import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module


object KoinModules {
    val simpleModels = module {
        single { ServiceLauncher(get()) }
        single { MyKtorServer(get(), get()) }
        single { GlobalSquareColor() }
        single {
            HttpClient(CIO) {
                install(WebSockets)
                install(RSocketSupport) {
                    connector = RSocketConnector {

                        this.connectionConfig {
                            payloadMimeType = PayloadMimeType(
                                data = WellKnownMimeType.ApplicationCloudeventsJson,
                                metadata = WellKnownMimeType.MessageRSocketRouting
                            )
                        }
                    }
                }
            }
        }

    }

    val viewModels = module {
        viewModel { MainActivityViewModel() }
    }

}