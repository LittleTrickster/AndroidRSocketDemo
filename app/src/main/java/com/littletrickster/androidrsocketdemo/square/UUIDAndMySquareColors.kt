package com.littletrickster.androidrsocketdemo.square

import kotlinx.serialization.Serializable

@Serializable
data class UUIDAndMySquareColors(var uid:String, var color: MySquareColors)