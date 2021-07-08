package com.littletrickster.androidrsocketdemo.mainactivity

import android.graphics.Color
import android.os.Bundle
import android.widget.SeekBar
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.littletrickster.androidrsocketdemo.databinding.ActivityMainBinding
import com.littletrickster.androidrsocketdemo.getLocalIpAddress
import com.littletrickster.androidrsocketdemo.permissions.BasePermissionActivity
import com.littletrickster.androidrsocketdemo.permissions.Permissions
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.koin.android.ext.android.inject
import java.util.*


class MainActivity : BasePermissionActivity() {

    val viewModel: MainActivityViewModel by inject()


    lateinit var binding: ActivityMainBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycleScope.launchWhenCreated {
            Permissions.waitForFinePermissionState()

            binding = ActivityMainBinding.inflate(layoutInflater)
            setContentView(binding.root)


            val startStopServiceButton = binding.startServiceButton

            myAddressListener()

            binding.redBar.setOnUserSeekChanged(viewModel::setRed)
            binding.greenBar.setOnUserSeekChanged(viewModel::setGreen)
            binding.blueBar.setOnUserSeekChanged(viewModel::setBlue)

            binding.connectToServer.setOnClickListener {
                if (!viewModel.clientConnected.value) viewModel.startClient(binding.serverToConnectAddress.text.toString())
                else viewModel.stopClient()
            }

            viewModel.failedToConnect.onEach {
                Toast.makeText(this@MainActivity, "$it", Toast.LENGTH_SHORT).show()
            }.launchIn(this)

            viewModel.red.onEach {
                binding.redColorText.text = it.toString()
                binding.redBar.progress = it
            }.launchIn(this)

            viewModel.green.onEach {
                binding.greenColorText.text = it.toString()
                binding.greenBar.progress = it
            }.launchIn(this)

            viewModel.blue.onEach {
                binding.blueColorText.text = it.toString()
                binding.blueBar.progress = it
            }.launchIn(this)


            viewModel.rgb.onEach {
                binding.colorSquere.setBackgroundColor(Color.rgb(it.red, it.green, it.blue))
            }.launchIn(this)

            viewModel.connectionCount.onEach {
                binding.connectionCountNr.text = it.toString()
            }.launchIn(this)


            viewModel.clientConnected.onEach {
                binding.connectToServer.text = if (!it) "CONNECT"
                else "DISCONNECT"
            }.launchIn(this)


            combine(viewModel.clientConnected, viewModel.serviceIsRunning) { client, server ->
                binding.startServiceButton.isEnabled = !(client && !server)

            }

            viewModel.serviceIsRunning.onEach {
                startStopServiceButton.text = if (!it) "START SERVER"
                else "STOP SERVER"

                //auto connect
                if (it && !viewModel.clientConnected.value) {
                    delay(1000)
                    val address = cachedAddress ?: "localhost"
                    binding.serverToConnectAddress.setText(address)
                    viewModel.startClient(address)
                }
            }.launchIn(this)

            viewModel.counter.onEach {
                binding.counterNr.text = it.toString()
            }.launchIn(this)



            startStopServiceButton.setOnClickListener {
                if (viewModel.serviceIsRunning.value) viewModel.stopServer(this@MainActivity)
                else viewModel.startServer(this@MainActivity)
            }

        }
    }

    var cachedAddress: String? = null

    private fun CoroutineScope.myAddressListener() {
        launch(Dispatchers.IO) {
            while (isActive) {
                val address = getLocalIpAddress()
                if (address != null) withContext(Dispatchers.Main) {
                    binding.ipAddress.text = "$address:8000"
                    cachedAddress = address
                }
                delay(5_000)
            }
        }
    }

    private fun SeekBar.setOnUserSeekChanged(function: (Int) -> Unit) {

        setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(
                seekBar: SeekBar,
                progress: Int,
                fromUser: Boolean
            ) {
                if (fromUser) function(progress)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })
    }


}
