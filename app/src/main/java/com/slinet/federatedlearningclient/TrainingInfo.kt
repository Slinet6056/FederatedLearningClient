package com.slinet.federatedlearningclient

//用于记录本机训练信息
object TrainingInfo {
    var trainingTimes: Int = 0
    var averageDuration: Double = 0.0
    var totalDuration: Double = 0.0
    val losses: MutableList<Double> = mutableListOf()

    //在每轮训练结束时调用，用于更新训练信息
    fun finishTraining(duration: Double) {
        trainingTimes++
        totalDuration += duration
        averageDuration = totalDuration / trainingTimes
    }
}