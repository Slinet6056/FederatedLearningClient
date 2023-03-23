package com.slinet.federatedlearningclient

import android.content.Context
import android.util.Log
import org.json.JSONObject
import java.io.*
import java.net.Socket
import kotlin.concurrent.thread


class SocketClient(private val context: Context) {
    private var ipAddress: String = ""
    private var port: Int = 0
    private var socket: Socket? = null
    private var running = false
    private var output: PrintWriter? = null

    //连接服务器
    //初次连接服务器时，会创建一个socket连接，以JSON格式传输控制信号以及一些设备信息
    //当客户端或服务器想要发送文件时，会向对方发送一个控制信号，对方收到信号后，会创建一个新的socket连接，用于传输文件
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
                checkConnection()

                //不断尝试接收服务器的控制信号
                val input = BufferedReader(InputStreamReader(socket!!.getInputStream()))
                var stringData: String?
                while (running) {
                    stringData = input.readLine()
                    if (stringData == null) continue
                    try {
                        val json = JSONObject(stringData)    //解析JSON格式的数据
                        when (json.getInt("statusCode")) {
                            0 -> checkConnection()           //statusCode为0时检查连接，服务器如果长时间收不到回应，会断开连接
                            1 -> receiveFile()               //statusCode为1时接收文件
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

    //向服务器发送训练后的模型
    fun sendFile(file: File, trainingDuration: Double, onSent: () -> Unit = {}) {
        if (socket == null) {
            Log.d("SocketClient", "Server not connected")
            return
        }
        thread {
            try {
                //请求发送文件，同时传输一些设备信息
                val deviceName = android.os.Build.DEVICE
                val deviceFingerprint = android.os.Build.FINGERPRINT
                output!!.println("""{"statusCode":1,"trainingDuration":$trainingDuration,"deviceName":"$deviceName","deviceFingerprint":"$deviceFingerprint"}""")
                Thread.sleep(500)

                //新建一个socket连接发送文件
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

    //收到服务器发送文件的请求后，新建一个socket连接接收文件
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

    //收到服务器检查连接的请求后，向服务器发送一些设备信息
    private fun checkConnection() {
        thread {
            try {
                val deviceName = android.os.Build.DEVICE
                val deviceFingerprint = android.os.Build.FINGERPRINT
                output!!.println("""{"statusCode":0,"deviceName":"$deviceName","deviceFingerprint":"$deviceFingerprint"}""")
                Log.d("SocketClient", "Connection checked")
            } catch (e: Exception) {
                Log.d("SocketClient", e.message.toString())
            }
        }
    }
}