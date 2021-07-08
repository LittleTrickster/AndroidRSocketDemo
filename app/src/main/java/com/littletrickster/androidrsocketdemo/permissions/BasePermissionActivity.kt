package com.littletrickster.androidrsocketdemo.permissions


import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar


abstract class BasePermissionActivity : AppCompatActivity() {

    companion object {
        private const val REQUEST_CODE_ASK_PERMISSIONS = 1
    }

    override fun onStart() {
        super.onStart()
        checkAndRequest()
    }

    private fun permissionAsk() {
        ActivityCompat.requestPermissions(
            this,
            Permissions.RUNTIME_PERMISSIONS,
            REQUEST_CODE_ASK_PERMISSIONS
        )
    }

    private fun checkAndRequest() {
        if (!Permissions.haveAllPermissions(this)) permissionAsk()
        else snackbar?.dismiss()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            REQUEST_CODE_ASK_PERMISSIONS -> {
                for (index in permissions.indices) {
                    if (grantResults[index] != PackageManager.PERMISSION_GRANTED) {
                        if (ActivityCompat.shouldShowRequestPermissionRationale(
                                this,
                                permissions[index]
                            )
                        ) showNormalSnack()
                        else showManualSnack()
                    }
                }
                Permissions.haveAllPermissions(this)
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }


    private var snackbar: Snackbar? = null
    private fun showNormalSnack() {
        snackbar = Snackbar.make(
            findViewById(android.R.id.content),
            "Permissions are needed for core functionality of this App",
            Snackbar.LENGTH_INDEFINITE
        ).setBehavior(NoSwipeBehavior()).setAction("ok") {
            ActivityCompat.requestPermissions(
                this,
                Permissions.RUNTIME_PERMISSIONS,
                REQUEST_CODE_ASK_PERMISSIONS
            )
        }.apply {
            show()
        }
    }

    private fun showManualSnack() {
        snackbar = Snackbar.make(
            findViewById(android.R.id.content),
            """Need to set permissions manually""",
            Snackbar.LENGTH_INDEFINITE
        ).setBehavior(NoSwipeBehavior()).setAction("ok") {
            if (Build.VERSION.SDK_INT >= 23) {
                val intent = Intent("android.settings.APPLICATION_DETAILS_SETTINGS")
                intent.data = Uri.parse("package:" + this.packageName)
                this.startActivity(intent)
            }
        }.apply {
            show()
        }
    }


    class NoSwipeBehavior : BaseTransientBottomBar.Behavior() {
        override fun canSwipeDismissView(child: View) = false
    }
}


