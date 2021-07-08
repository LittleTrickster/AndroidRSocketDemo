package com.littletrickster.androidrsocketdemo


import com.littletrickster.androidrsocketdemo.square.MySquareColors
import com.littletrickster.androidrsocketdemo.square.UUIDAndMySquareColors
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.distinctUntilChanged

class GlobalSquareColor {

    val colorFlow =
        MutableSharedFlow<UUIDAndMySquareColors>(replay = 1, onBufferOverflow = BufferOverflow.DROP_LATEST)
            .apply { tryEmit(UUIDAndMySquareColors("-1", MySquareColors(0, 0, 0))) }

    val distinctColor = colorFlow.distinctUntilChanged()


}