package com.slinet.federatedlearningclient

import android.icu.text.DecimalFormat
import android.os.Bundle
import android.view.LayoutInflater
import android.view.WindowManager
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.slinet.federatedlearningclient.databinding.ActivityMainBinding
import java.io.File
import java.time.Instant


class MainActivity : AppCompatActivity() {
    lateinit var binding: ActivityMainBinding
    lateinit var socketClient: SocketClient
    private var autoMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        this.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        socketClient = SocketClient(this)
        val inflater = LayoutInflater.from(this)
        val layout = inflater.inflate(R.layout.connect_server, findViewById(android.R.id.content), false)
        val serverIpEditText: EditText = layout.findViewById(R.id.serverIpEditText)
        val serverPortEditText: EditText = layout.findViewById(R.id.serverPortEditText)
        AlertDialog.Builder(this).apply {
            setView(layout)
            setPositiveButton("开启") { _, _ ->
                val serverIp = serverIpEditText.text.toString()
                val serverPort = serverPortEditText.text.toString()
                //TODO: Check IP address
                if (serverPort.length !in 1..5 || serverPort.toInt() !in 1..65535) {
                    Toast.makeText(this@MainActivity, "请输入正确的端口号", Toast.LENGTH_SHORT).show()
                } else {
                    socketClient.connectServer(serverIp, serverPort.toInt())
                }
            }
            setNegativeButton("取消", null)
            setCancelable(false)
            show()
        }

        binding.trainButton.setOnClickListener {
            if (!Model.isCreated()) {
                Model.create {
                    runOnUiThread {
                        Toast.makeText(this, "模型已建立", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            val layoutInflater = LayoutInflater.from(this)
            val connectionLayout = layoutInflater.inflate(R.layout.set_epoch, findViewById(android.R.id.content), false)
            val epochEditText: EditText = connectionLayout.findViewById(R.id.epochEditText)
            AlertDialog.Builder(this).apply {
                setView(connectionLayout)
                setPositiveButton("确定") { _, _ ->
                    Model.train(epochEditText.text.toString().toInt(), binding.progressBar) {
                        runOnUiThread {
                            Toast.makeText(this@MainActivity, "训练结束", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                setNegativeButton("取消", null)
                setCancelable(true)
                show()
            }
        }

        binding.inferButton.setOnClickListener {
            val pl = binding.petalLength.text.toString().toDouble()
            val pw = binding.petalWidth.text.toString().toDouble()
            val sl = binding.sepalLength.text.toString().toDouble()
            val sw = binding.sepalWidth.text.toString().toDouble()
            val output = Model.infer(pl, pw, sl, sw)
            val df2 = DecimalFormat("#.##")
            binding.setosa.text = df2.format(output[0]).toString()
            binding.versicolor.text = df2.format(output[1]).toString()
            binding.virginica.text = df2.format(output[2]).toString()
        }

        binding.saveButton.setOnClickListener {
            Model.save(this) {
                socketClient.sendFile(File(this.filesDir, "trained_model.zip"), -1.0)
            }
        }

        binding.loadButton.setOnClickListener {
            Model.load(this)
        }

        binding.autoModeButton.setOnClickListener {
            if (!autoMode) {
                autoMode = true
                binding.autoModeButton.text = "关闭自动训练"
                if (!Model.isCreated()) {
                    Model.create {
                        FederatedLearningThread().start()
                    }
                } else {
                    FederatedLearningThread().start()
                }
            } else {
                autoMode = false
                binding.autoModeButton.text = "开启自动训练"
                Toast.makeText(this, "正在停止自动训练", Toast.LENGTH_SHORT).show()
            }
        }
    }

    inner class FederatedLearningThread : Thread() {
        override fun run() {
            runOnUiThread { Toast.makeText(this@MainActivity, "开始自动训练", Toast.LENGTH_SHORT).show() }
            var blocked: Boolean
            while (autoMode) {
                blocked = true
                val startTime = Instant.now().toEpochMilli() / 1000.0
                Model.train(100, binding.progressBar) {
                    val endTime = Instant.now().toEpochMilli() / 1000.0
                    Model.save(this@MainActivity) {
                        socketClient.sendFile(File(this@MainActivity.filesDir, "trained_model.zip"), endTime - startTime) {
                            sleep(3000)
                            Model.load(this@MainActivity)
                            blocked = false
                        }
                    }
                }
                while (blocked) {
                    sleep(1000)
                }
            }
            runOnUiThread { Toast.makeText(this@MainActivity, "结束自动训练", Toast.LENGTH_SHORT).show() }
        }
    }
}