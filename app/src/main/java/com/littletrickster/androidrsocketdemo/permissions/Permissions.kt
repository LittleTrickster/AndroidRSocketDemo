package com.littletrickster.androidrsocketdemo.permissions


import android.Manifest.permission.FOREGROUND_SERVICE
import android.Manifest.permission.INTERNET
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first


object Permissions {
    val RUNTIME_PERMISSIONS = arrayListOf(
        INTERNET,
    ).apply {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            add(FOREGROUND_SERVICE)
        }
    }.toTypedArray()

    val permissionsState = MutableStateFlow(PermissionState.UNSET)
    suspend fun waitForFinePermissionState() {
        permissionsState.first { it == PermissionState.FINE }
    }

    fun haveAllPermissions(context: Context): Boolean {
        val permissionsOk = context.hasPermissions(*RUNTIME_PERMISSIONS)
        if (permissionsOk) permissionsState.tryEmit(PermissionState.FINE)
        else permissionsState.tryEmit(PermissionState.BAD)
        return permissionsOk
    }

    private fun Context.hasPermissions(vararg permissions: String): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            for (permission in permissions) {
                if (ActivityCompat.checkSelfPermission(
                        this,
                        permission
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    return false
                }
            }
        }
        return true
    }


    enum class PermissionState {
        UNSET,
        BAD,
        FINE
    }
}


