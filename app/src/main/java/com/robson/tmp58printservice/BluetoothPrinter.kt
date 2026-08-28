package com.robson.tmp58printservice

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import java.io.IOException
import java.util.UUID

class BluetoothPrinter(
    private val context: Context
) {

    companion object {

        private const val TAG = "BluetoothPrinter"

        private val SPP_UUID: UUID =
            UUID.fromString(
                "00001101-0000-1000-8000-00805F9B34FB"
            )
    }

    private var socket: BluetoothSocket? = null

    private fun getBluetoothAdapter(): BluetoothAdapter? {

        val bluetoothManager =
            context.getSystemService(
                Context.BLUETOOTH_SERVICE
            ) as BluetoothManager

        return bluetoothManager.adapter
    }

    fun conectarPorNome(
        nomeImpressora: String
    ): Boolean {

        verificarPermissao()

        val adapter = getBluetoothAdapter()

        if (adapter == null) {
            Log.e(TAG, "Bluetooth não disponível")
            return false
        }

        if (!adapter.isEnabled) {
            Log.e(TAG, "Bluetooth está desligado")
            return false
        }

        val dispositivosPareados =
            adapter.bondedDevices

        Log.d(
            TAG,
            "Dispositivos pareados: ${dispositivosPareados.size}"
        )

        dispositivosPareados.forEach {

            Log.d(
                TAG,
                "Bluetooth: ${it.name} - ${it.address}"
            )
        }

        val device =
            dispositivosPareados.firstOrNull {

                it.name?.contains(
                    nomeImpressora,
                    ignoreCase = true
                ) == true
            }

        if (device == null) {

            Log.e(
                TAG,
                "Impressora não encontrada: $nomeImpressora"
            )

            return false
        }

        Log.d(
            TAG,
            "Conectando em ${device.name} / ${device.address}"
        )

        return try {

            adapter.cancelDiscovery()

            socket?.close()

            socket =
                device.createRfcommSocketToServiceRecord(
                    SPP_UUID
                )

            socket!!.connect()

            Log.d(
                TAG,
                "Bluetooth conectado com sucesso"
            )

            true

        } catch (e: IOException) {

            Log.e(
                TAG,
                "Erro Bluetooth",
                e
            )

            try {
                socket?.close()
            } catch (_: Exception) {
            }

            socket = null

            false
        }
    }

    fun enviar(
        dados: ByteArray
    ): Boolean {

        val bluetoothSocket = socket

        if (
            bluetoothSocket == null ||
            !bluetoothSocket.isConnected
        ) {
            Log.e(TAG, "Impressora não conectada")
            return false
        }

        return try {

            val output =
                bluetoothSocket.outputStream

            output.write(dados)

            output.flush()

            Log.d(
                TAG,
                "${dados.size} bytes enviados"
            )

            true

        } catch (e: IOException) {

            Log.e(
                TAG,
                "Erro enviando dados",
                e
            )

            false
        }
    }

    fun desconectar() {

        try {
            socket?.close()
        } catch (_: Exception) {
        }

        socket = null
    }

    private fun verificarPermissao() {

        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.S
        ) {

            if (
                context.checkSelfPermission(
                    Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED
            ) {

                throw SecurityException(
                    "Permissão BLUETOOTH_CONNECT não concedida"
                )
            }
        }
    }
}