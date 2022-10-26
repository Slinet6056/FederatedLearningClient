package com.slinet.federatedlearningclient

import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.WindowManager
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.google.android.material.snackbar.Snackbar
import com.slinet.federatedlearningclient.Utils.displaySnackBar
import com.slinet.federatedlearningclient.databinding.ActivityMainBinding
import java.io.File
import java.time.Instant
import kotlin.math.roundToInt


class MainActivity : AppCompatActivity() {
    lateinit var binding: ActivityMainBinding
    lateinit var socketClient: SocketClient
    private var autoMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        this.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        socketClient = SocketClient(this)
        val inflater = LayoutInflater.from(this)
        val layout = inflater.inflate(R.layout.connect_server, findViewById(android.R.id.content), false)
        val serverIpEditText: EditText = layout.findViewById(R.id.server_ip_editText)
        val serverPortEditText: EditText = layout.findViewById(R.id.server_port_editText)
        AlertDialog.Builder(this).apply {
            setView(layout)
            setPositiveButton("开启") { _, _ ->
                val serverIp = serverIpEditText.text.toString()
                val serverPort = serverPortEditText.text.toString()
                //TODO: Check IP address
                if (serverPort.length !in 1..5 || serverPort.toInt() !in 1..65535) {
                    displaySnackBar(Snackbar.make(binding.root, "请输入正确的端口号", Snackbar.LENGTH_SHORT))
                } else {
                    socketClient.connectServer(serverIp, serverPort.toInt())
                }
            }
            setCancelable(false)
            show()
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
                binding.autoModeButton.text = "正在停止训练"
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_info -> {
                displaySnackBar(Snackbar.make(binding.root, "点这没用", Snackbar.LENGTH_SHORT))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    inner class FederatedLearningThread : Thread() {
        override fun run() {
            runOnUiThread {
                displaySnackBar(Snackbar.make(binding.root, "开始自动训练", Snackbar.LENGTH_SHORT))
            }
            var blocked: Boolean
            while (autoMode) {
                blocked = true
                val startTime = Instant.now().toEpochMilli() / 1000.0
                Model.train(100, binding.content.progressBar) {
                    val endTime = Instant.now().toEpochMilli() / 1000.0
                    runOnUiThread {
                        TrainingInfo.finishTraining(endTime - startTime)
                        updateTrainingInfo()
                        showLossChart()
                    }
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
            runOnUiThread {
                binding.autoModeButton.text = "开启自动训练"
                binding.content.progressBar.progress = 0
                displaySnackBar(Snackbar.make(binding.root, "结束自动训练", Snackbar.LENGTH_SHORT))
            }
        }
    }

    private fun updateTrainingInfo() {
        binding.content.trainingTimes.text = TrainingInfo.trainingTimes.toString()
        binding.content.averageDuration.text = ((TrainingInfo.averageDuration * 100.0).roundToInt() / 100.0).toString()
        binding.content.totalDuration.text = ((TrainingInfo.totalDuration * 100.0).roundToInt() / 100.0).toString()
    }

    private fun showLossChart() {
        val entries = ArrayList<Entry>()
        TrainingInfo.losses.forEachIndexed { index, loss ->
            entries.add(Entry((index + 1).toFloat(), loss.toFloat()))
        }

        val dataSet = LineDataSet(entries, "Loss")
        val lineData = LineData(dataSet)
        val chart: LineChart = binding.content.lossChart
        chart.data = lineData
        dataSet.setDrawValues(false)
        chart.description.isEnabled = false
        chart.isClickable = false
        chart.isDragEnabled = false
        chart.isHighlightPerTapEnabled = false
        chart.isHighlightPerDragEnabled = false
        chart.isScaleXEnabled = false
        chart.isScaleYEnabled = false
        chart.isDoubleTapToZoomEnabled = false
        chart.xAxis.isEnabled = false
        chart.legend.isEnabled = false
        var textColor = 0
        var lineColor = 0
        when (this.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) {
            Configuration.UI_MODE_NIGHT_YES -> {
                textColor = -3488560
                lineColor = -1319425
            }
            Configuration.UI_MODE_NIGHT_NO -> {
                textColor = -12040880
                lineColor = -14614435
            }
        }
        dataSet.color = lineColor
        dataSet.valueTextColor = textColor
        dataSet.setCircleColor(lineColor)
        chart.xAxis.textColor = textColor
        chart.axisLeft.textColor = textColor
        chart.axisRight.textColor = textColor

        chart.invalidate()
    }
}