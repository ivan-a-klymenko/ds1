package tech.ai_robotics.drone_shooter_2.bluetooth.server

import android.Manifest.permission.BLUETOOTH_CONNECT
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.ContextCompat
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.util.UUID


/**
 * @author ivan.klymenko@fuib.com on 01/05/2025
 */
// Android 1
class BluetoothClientSocketHandler(
    private val context: Context,
    private val onMessageReceived: (String) -> Unit
) : Thread() {

    private var serverSocket: BluetoothServerSocket? = null
    private var running = true

    override fun run() {
        if (ContextCompat.checkSelfPermission(context, BLUETOOTH_CONNECT)
            != PackageManager.PERMISSION_GRANTED
        ) {
            Log.e("BTServer", "Missing BLUETOOTH_CONNECT permission")
            return
        }

        try {
            val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
            serverSocket = bluetoothAdapter.listenUsingRfcommWithServiceRecord(
                "BT_APP",
                UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
            )
        } catch (e: IOException) {
            Log.e("BTServer", "Server socket init failed: ${e.message}")
            return
        }

        while (running) {
            val socket = try {
                serverSocket?.accept()
            } catch (e: IOException) {
                Log.e("BTServer", "Accept failed: ${e.message}")
                break
            }

            socket?.also {
                handleSocket(it)
            }
        }
    }

    private fun handleSocket(socket: BluetoothSocket) {
        val reader = BufferedReader(InputStreamReader(socket.inputStream))
        while (running) {
            try {
                val line = reader.readLine() ?: break
                onMessageReceived(line)
            } catch (e: IOException) {
                break
            }
        }
        socket.close()
    }

    fun stopServer() {
        running = false
        serverSocket?.close()
    }
}

