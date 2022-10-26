package com.slinet.federatedlearningclient

object TrainingInfo {
    var trainingTimes: Int = 0
    var averageDuration: Double = 0.0
    var totalDuration: Double = 0.0
    val losses: MutableList<Double> = mutableListOf()

    fun finishTraining(duration: Double) {
        trainingTimes++
        totalDuration += duration
        averageDuration = totalDuration / trainingTimes
    }
}