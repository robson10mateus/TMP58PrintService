package com.robson.tmp58printservice

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.content.Intent
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.app.Activity

class MainActivity : Activity(){

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(savedInstanceState)

        solicitarBluetooth()

        val layout =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.VERTICAL

                setPadding(
                    50,
                    100,
                    50,
                    50
                )
            }

        val titulo =
            TextView(this).apply {

                text =
                    "TMP58 Print Service"

                textSize = 24f
            }

        val descricao =
            TextView(this).apply {

                text =
                    "\nServiço para impressão Bluetooth IMP-TMP58ABT.\n"
            }

        val botao =
            Button(this).apply {

                text =
                    "CONFIGURAÇÕES DE IMPRESSÃO"

                setOnClickListener {

                    startActivity(
                        Intent(
                            Settings.ACTION_PRINT_SETTINGS
                        )
                    )
                }
            }

        layout.addView(titulo)
        layout.addView(descricao)
        layout.addView(botao)

        setContentView(layout)
    }

    private fun solicitarBluetooth() {

        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.S
        ) {

            if (
                checkSelfPermission(
                    Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED
            ) {

                requestPermissions(
                    arrayOf(
                        Manifest.permission.BLUETOOTH_CONNECT
                    ),
                    100
                )
            }
        }
    }
}