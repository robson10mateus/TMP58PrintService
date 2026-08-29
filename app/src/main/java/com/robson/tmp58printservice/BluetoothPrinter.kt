package com.robson.tmp58printservice

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import java.io.IOException
import java.util.UUID
import java.util.concurrent.CancellationException

data class PairedPrinter(
    val name: String,
    val address: String
)

class BluetoothPrinter(
    private val context: Context
) {
    companion object {
        private const val TAG = "BluetoothPrinter"
        private const val WRITE_CHUNK_SIZE = 1024

        private val SPP_UUID: UUID = UUID.fromString(
            "00001101-0000-1000-8000-00805F9B34FB"
        )

        fun hasConnectPermission(context: Context): Boolean {
            return Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
                context.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) ==
                PackageManager.PERMISSION_GRANTED
        }

        @SuppressLint("MissingPermission")
        fun listPairedDevices(context: Context): List<PairedPrinter> {
            if (!hasConnectPermission(context)) return emptyList()

            val adapter = bluetoothAdapter(context) ?: return emptyList()
            if (!adapter.isEnabled) return emptyList()

            return adapter.bondedDevices
                .map { device ->
                    PairedPrinter(
                        name = device.name?.takeIf { it.isNotBlank() } ?: "Dispositivo sem nome",
                        address = device.address
                    )
                }
                .sortedBy { it.name.lowercase() }
        }

        fun isConfiguredDeviceAvailable(
            context: Context,
            configuration: PrinterConfiguration
        ): Boolean {
            if (!hasConnectPermission(context)) return false

            return listPairedDevices(context).any { device ->
                if (configuration.address != null) {
                    device.address.equals(configuration.address, ignoreCase = true)
                } else {
                    device.name.contains(configuration.name, ignoreCase = true)
                }
            }
        }

        private fun bluetoothAdapter(context: Context): BluetoothAdapter? {
            val manager = context.getSystemService(Context.BLUETOOTH_SERVICE)
                as? BluetoothManager
            return manager?.adapter
        }
    }

    @Volatile
    private var socket: BluetoothSocket? = null

    @SuppressLint("MissingPermission")
    fun connect(configuration: PrinterConfiguration): Boolean {
        requirePermission()

        val adapter = bluetoothAdapter(context)
        if (adapter == null) {
            Log.e(TAG, "Bluetooth não disponível")
            return false
        }
        if (!adapter.isEnabled) {
            Log.e(TAG, "Bluetooth está desligado")
            return false
        }

        val device = adapter.bondedDevices.firstOrNull { candidate ->
            if (configuration.address != null) {
                candidate.address.equals(configuration.address, ignoreCase = true)
            } else {
                candidate.name?.contains(configuration.name, ignoreCase = true) == true
            }
        }

        if (device == null) {
            Log.e(TAG, "Impressora configurada não está pareada")
            return false
        }

        return try {
            disconnect()
            val newSocket = device.createRfcommSocketToServiceRecord(SPP_UUID)
            socket = newSocket
            newSocket.connect()
            Log.d(TAG, "Bluetooth conectado com sucesso")
            true
        } catch (error: IOException) {
            Log.e(TAG, "Erro na conexão Bluetooth", error)
            disconnect()
            false
        }
    }

    fun send(
        data: ByteArray,
        isCancelled: () -> Boolean = { false }
    ): Boolean {
        val currentSocket = socket
        if (currentSocket == null || !currentSocket.isConnected) {
            Log.e(TAG, "Impressora não conectada")
            return false
        }

        return try {
            val output = currentSocket.outputStream
            var offset = 0

            while (offset < data.size) {
                if (isCancelled() || Thread.currentThread().isInterrupted) {
                    throw CancellationException("Impressão cancelada")
                }

                val count = minOf(WRITE_CHUNK_SIZE, data.size - offset)
                output.write(data, offset, count)
                offset += count
            }

            output.flush()
            Log.d(TAG, "${data.size} bytes enviados")
            true
        } catch (error: CancellationException) {
            throw error
        } catch (error: IOException) {
            Log.e(TAG, "Erro enviando dados", error)
            false
        }
    }

    fun disconnect() {
        try {
            socket?.close()
        } catch (_: IOException) {
            // A conexão já está sendo descartada.
        } finally {
            socket = null
        }
    }

    private fun requirePermission() {
        if (!hasConnectPermission(context)) {
            throw SecurityException("Permissão BLUETOOTH_CONNECT não concedida")
        }
    }
}
