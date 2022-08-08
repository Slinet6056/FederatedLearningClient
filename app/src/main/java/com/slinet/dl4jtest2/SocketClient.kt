package com.slinet.dl4jtest2

import android.content.Context
import android.util.Log
import org.json.JSONObject
import java.io.*
import java.net.Socket
import kotlin.concurrent.thread

class SocketClient(val context: Context) {
    private var ipAddress: String = ""
    private var port: Int = 0
    private var socket: Socket? = null
    private var running = false
    private var output: PrintWriter? = null

    fun connectServer(ipAddress: String, port: Int) {
        if (socket != null) {
            Log.d("SocketClient", "Already connected")
            return
        }
        this.ipAddress = ipAddress
        this.port = port
        thread {
            try {
                socket = Socket(ipAddress, port)
                output = PrintWriter(OutputStreamWriter(socket!!.getOutputStream(), "UTF-8"), true)
                running = true
                Log.d("SocketClient", "Server connected")
                val input = BufferedReader(InputStreamReader(socket!!.getInputStream()))
                var stringData: String?
                while (running) {
                    stringData = input.readLine()
                    if (stringData == null) continue
                    try {
                        val json = JSONObject(stringData)
                        when (json.getInt("statusCode")) {
                            1 -> receiveFile()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            } catch (e: Exception) {
                Log.d("SocketClient", e.message.toString())
            }
        }
    }

    fun sendFile(file: File, onSent: () -> Unit = {}) {
        if (socket == null) {
            Log.d("SocketClient", "Server not connected")
            return
        }
        thread {
            try {
                output!!.println("""{"statusCode":1}""")
                Thread.sleep(500)

                val transferSocket = Socket(ipAddress, port)
                Log.d("SocketClient", "Transfer socket get connected")
                val outputStream = transferSocket.getOutputStream()
                val fileInputStream = FileInputStream(file)
                var size: Int
                val buffer = ByteArray(1024)
                while (fileInputStream.read(buffer, 0, 1024).also { size = it } != -1) {
                    outputStream.write(buffer, 0, size)
                }
                fileInputStream.close()
                outputStream.close()
                transferSocket.close()
                Log.d("SocketClient", "File sent")
            } catch (e: Exception) {
                Log.d("SocketClient", e.message.toString())
            }
            onSent.invoke()
        }

    }

    private fun receiveFile() {
        thread {
            try {
                val transferSocket = Socket(ipAddress, port)
                val inputStream = transferSocket.getInputStream()
                val file = File(context.filesDir, "received_model.zip")
                val fileOutputStream = FileOutputStream(file, false)
                val buffer = ByteArray(1024)
                var size: Int
                while (inputStream.read(buffer).also { size = it } != -1) {
                    fileOutputStream.write(buffer, 0, size)
                }
                fileOutputStream.close()
                inputStream.close()
                transferSocket.close()
                Log.d("SocketClient", "File received")
            } catch (e: Exception) {
                Log.d("SocketClient", e.message.toString())
            }
        }
    }
}