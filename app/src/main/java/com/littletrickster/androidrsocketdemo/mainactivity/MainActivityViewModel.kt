package com.littletrickster.androidrsocketdemo.mainactivity

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.littletrickster.androidrsocketdemo.*
import com.littletrickster.androidrsocketdemo.fservice.MyService
import com.littletrickster.androidrsocketdemo.fservice.ServiceLauncher
import com.littletrickster.androidrsocketdemo.square.MySquareColors
import com.littletrickster.androidrsocketdemo.square.UUIDAndMySquareColors
import io.ktor.client.*
import io.ktor.client.engine.*
import io.ktor.client.engine.cio.*
import io.rsocket.kotlin.RSocket
import io.rsocket.kotlin.core.RSocketConnector
import io.rsocket.kotlin.transport.ktor.websocket.client.WebSocketClientTransport
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.*


class MainActivityViewModel : ViewModel(), KoinComponent {
    val serverColor: GlobalSquareColor by inject()
    val serviceLauncher: ServiceLauncher by inject()
    val httpClient: HttpClient by inject()

    val myId = UUID.randomUUID().toString()

    val clientConnected = MutableStateFlow(false)

    val failedToConnect = MutableSharedFlow<String>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    var rsocket: RSocket? = null

    val clientLock = Mutex()

    private val handler = CoroutineExceptionHandler { c, e ->
        rsocket?.cancel()
        rsocket = null
        clientConnected.tryEmit(false)
        connectionCount.tryEmit(0)
        failedToConnect.tryEmit(e.message ?: "failed to connect")

    }

    fun startClient(address: String) {
        viewModelScope.launch(handler) {
            clientLock.withLock {
                if (clientConnected.value) return@launch

//                object :io.ktor.client.engine.HttpClientEngineFactory<String>{
//                    override fun create(block: String.() -> Unit): HttpClientEngine {
//                        return httpClient
//                    }
//                }




                val connection =
                    WebSocketClientTransport(CIO, host = address, port = 8000, path = "/rsocket", secure = false)

                val currentRsocket = RSocketConnector{}.connect(connection)

                clientConnected.tryEmit(true)


                currentRsocket.coroutineContext[Job]!!.invokeOnCompletion {
                    clientConnected.tryEmit(false)
                }

                currentRsocket
                    .requestStream("current color")
                    .map { Json.decodeFromString<UUIDAndMySquareColors>(it.data.readText()) }
                    .filter { it.uid != myId }
                    .onEach { backendColor.emit(it.color) }
                    .launchIn(this)

                currentRsocket
                    .requestStream(CONNECTION_COUNT)
                    .map { it.data.readText().toInt() }
                    .onEach(connectionCount::emit)
                    .launchIn(this)


                currentRsocket
                    .requestStream(COUNTER)
                    .map { it.data.readText().toInt() }
                    .onEach(counter::emit)
                    .launchIn(this)


                rsocket = currentRsocket
            }
        }
    }

    fun stopClient() {
        viewModelScope.launch {
            clientLock.withLock {
                if (!clientConnected.value) return@launch
                rsocket?.cancel()
                rsocket = null
                connectionCount.tryEmit(0)
            }
        }
    }

    fun startServer(context: Context) {
        serverColor.colorFlow.tryEmit(UUIDAndMySquareColors(myId, rgb.value))
        serviceLauncher.start(context)
    }

    fun stopServer(context: Context) {
        serviceLauncher.stop(context)
    }

    val serviceIsRunning = MyService.running


    fun setRed(value: Int) {
        val current = rgb.value.apply { red = value }
        frontEndColor.tryEmit(current)
        sendToServer(current)
    }

    fun setGreen(value: Int) {
        val current = rgb.value.apply { green = value }
        frontEndColor.tryEmit(current)
        sendToServer(current)
    }

    fun setBlue(value: Int) {
        val current = rgb.value.apply { blue = value }
        frontEndColor.tryEmit(current)
        sendToServer(current)
    }

    private var sendToServerJob: Job? = null

    private fun sendToServer(current: MySquareColors) {
        sendToServerJob?.cancel()
        sendToServerJob = rsocket?.run {
            viewModelScope.launch {
                fireAndForget(
                    "sendColor",
                    Json.encodeToString(UUIDAndMySquareColors(myId, current))
                )
            }
        }
    }

    val backendColor =
        MutableSharedFlow<MySquareColors>(replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    val frontEndColor =
        MutableSharedFlow<MySquareColors>(replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    val connectionCount =
        MutableSharedFlow<Int>(replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)


    val counter =
        MutableSharedFlow<Int>(replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)


    val red = merge(backendColor.map { it.red }, frontEndColor.map { it.red })
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    val green = merge(backendColor.map { it.green }, frontEndColor.map { it.green })
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    val blue = merge(backendColor.map { it.blue }, frontEndColor.map { it.blue })
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0)


    val rgb = merge(backendColor, frontEndColor)
        .onStart { emit(MySquareColors()) }
        .conflate()
        .shareIn(viewModelScope, SharingStarted.Eagerly, 1)

    override fun onCleared() {
        rsocket?.cancel()
    }
}

//slow
private val <T> SharedFlow<T>.value: T
    get() = this.replayCache[0]

//@OptIn(InternalCoroutinesApi::class)
//private fun <T> Flow<T>.shareWithCurrent(
//    coroutineScope: CoroutineScope,
//    started: SharingStarted,
//    default: T
//): StateFlow<T> {
//    var current = default
//    val flow = this.onEach { current = it }.conflate().shareIn(coroutineScope, started, 0)
//    return object : StateFlow<T> {
//        override val replayCache: List<T>
//            get() = flow.replayCache
//
//        override val value: T
//            get() = current
//
//        @InternalCoroutinesApi
//        override suspend fun collect(collector: FlowCollector<T>) = flow.collect(collector)
//    }
//}

