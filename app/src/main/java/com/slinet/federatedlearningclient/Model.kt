package com.slinet.federatedlearningclient

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.ProgressBar
import com.google.android.material.snackbar.Snackbar
import com.slinet.federatedlearningclient.Utils.displaySnackBar
import org.deeplearning4j.nn.conf.NeuralNetConfiguration
import org.deeplearning4j.nn.conf.layers.DenseLayer
import org.deeplearning4j.nn.conf.layers.OutputLayer
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork
import org.deeplearning4j.nn.weights.WeightInit
import org.deeplearning4j.util.ModelSerializer
import org.nd4j.linalg.activations.Activation
import org.nd4j.linalg.api.ndarray.INDArray
import org.nd4j.linalg.dataset.DataSet
import org.nd4j.linalg.factory.Nd4j
import org.nd4j.linalg.lossfunctions.LossFunctions
import java.io.File
import java.io.FileOutputStream
import kotlin.concurrent.thread

object Model {
    private var myNetwork: MultiLayerNetwork? = null
    private lateinit var trainingIn: INDArray
    private lateinit var trainingOut: INDArray
    private lateinit var myData: DataSet

    //训练数据的初始化
    init {
        thread {
            val row = TrainingData.irisData.size / 4
            val col = 4
            val irisMatrix = Array(row) { DoubleArray(col) }
            var i = 0
            for (r in 0 until row) {
                for (c in 0 until col) {
                    irisMatrix[r][c] = TrainingData.irisData[i++]
                }
            }

            val rowLabel = TrainingData.labelData.size / 3
            val colLabel = 3
            val twodimLabel = Array(rowLabel) { DoubleArray(colLabel) }
            i = 0
            for (r in 0 until rowLabel) {
                for (c in 0 until colLabel) {
                    twodimLabel[r][c] = TrainingData.labelData[i++]
                }
            }

            trainingIn = Nd4j.create(irisMatrix)
            trainingOut = Nd4j.create(twodimLabel)
            myData = DataSet(trainingIn, trainingOut)
        }
    }

    //创建模型
    fun create(onCreated: () -> Unit) {
        thread {
            //输入层
            val inputLayer = DenseLayer.Builder()
                .nIn(4)
                .nOut(20)
                .name("Input")
                .build()

            //隐藏层
            val hiddenLayer = DenseLayer.Builder()
                .nIn(20)
                .nOut(20)
                .name("Hidden")
                .build()

            //输出层
            val outputLayer = OutputLayer.Builder(LossFunctions.LossFunction.NEGATIVELOGLIKELIHOOD)
                .nIn(20)
                .nOut(3)
                .name("Output")
                .activation(Activation.SOFTMAX) //标准的 softmax 激活函数
                .build()

            val nncBuilder = NeuralNetConfiguration.Builder()
            val seed = 6L
            nncBuilder.seed(seed)
            nncBuilder.activation(Activation.TANH) //标准的双曲正切激活函数
            nncBuilder.weightInit(WeightInit.XAVIER) //初始权重为均值 0, 方差为 2.0/(fanIn + fanOut)的高斯分布

            val listBuilder = nncBuilder.list()
            listBuilder.layer(0, inputLayer)
            listBuilder.layer(1, hiddenLayer)
            listBuilder.layer(2, outputLayer)

            myNetwork = MultiLayerNetwork(listBuilder.build())
            myNetwork!!.init()

            Log.d("Model", "Model created")
            onCreated.invoke()
        }
    }

    //训练模型
    fun train(epoch: Int, progressBar: ProgressBar, onFinished: () -> Unit) {
        if (!isCreated()) return
        Handler(Looper.getMainLooper()).post {
            progressBar.progress = 0
        }
        thread {
            Log.d("Model", "Start training")
            for (i in 0 until epoch) {
                myNetwork!!.fit(myData)
                Handler(Looper.getMainLooper()).post {
                    progressBar.progress = (i + 1) * 1000 / epoch
                }
            }
            TrainingInfo.losses.add(myNetwork!!.score())
            Log.d("Model", "Finish training")
            onFinished.invoke()
        }
    }

    //使用模型判断输入数据的类别（没用到）
    fun infer(pl: Double = 0.0, pw: Double = 0.0, sl: Double = 0.0, sw: Double = 0.0): DoubleArray {
        if (!isCreated()) return DoubleArray(3)
        val actualInput = Nd4j.zeros(1, 4)
        actualInput.putScalar(intArrayOf(0, 0), pl)
        actualInput.putScalar(intArrayOf(0, 1), pw)
        actualInput.putScalar(intArrayOf(0, 2), sl)
        actualInput.putScalar(intArrayOf(0, 3), sw)
        val actualOutput = myNetwork!!.output(actualInput)
        Log.d("Model", "Inferred")
        return actualOutput.toDoubleVector()
    }

    //将训练后的模型保存到本地
    fun save(context: Context, onFinished: () -> Unit) {
        if (!isCreated()) return
        thread {
            try {
                val file = File(context.filesDir, "trained_model.zip")
                val outputStream = FileOutputStream(file)
                ModelSerializer.writeModel(myNetwork!!, outputStream, true)
                Handler(Looper.getMainLooper()).post {
                    displaySnackBar(Snackbar.make((context as MainActivity).findViewById(R.id.main_layout), "模型已保存", Snackbar.LENGTH_SHORT))
                }
            } catch (e: Exception) {
                Log.e("Save model error", e.message.toString())
                Handler(Looper.getMainLooper()).post {
                    displaySnackBar(Snackbar.make((context as MainActivity).findViewById(R.id.main_layout), "模型保存失败", Snackbar.LENGTH_SHORT))
                }
            }
            onFinished.invoke()
        }
    }

    //从本地加载模型
    fun load(context: Context) {
        thread {
            try {
                val file = File(context.filesDir, "received_model.zip")
                myNetwork = ModelSerializer.restoreMultiLayerNetwork(file)
                Handler(Looper.getMainLooper()).post {
                    displaySnackBar(Snackbar.make((context as MainActivity).findViewById(R.id.main_layout), "模型已加载", Snackbar.LENGTH_SHORT))
                }
            } catch (e: Exception) {
                Log.e("Load model error", e.message.toString())
                Handler(Looper.getMainLooper()).post {
                    displaySnackBar(Snackbar.make((context as MainActivity).findViewById(R.id.main_layout), "模型加载失败", Snackbar.LENGTH_SHORT))
                }
            }
        }
    }

    //判断模型是否已创建
    fun isCreated() = myNetwork != null
}