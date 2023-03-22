package com.slinet.federatedlearningclient

import android.view.View
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.google.android.material.snackbar.Snackbar

object Utils {
    fun displaySnackBar(snackbar: Snackbar, sideMargin: Int = 24, marginBottom: Int = 20) {
        val snackBarView: View = snackbar.view
        val params = snackBarView.layoutParams as CoordinatorLayout.LayoutParams
        params.setMargins(
            params.leftMargin + sideMargin,
            params.topMargin,
            params.rightMargin + sideMargin,
            params.bottomMargin + marginBottom
        )
        snackBarView.layoutParams = params
        snackbar.show()
    }

    fun readTrainingData(str: String) {
        var strArray = str.split("#").toTypedArray()[0].split(",").toTypedArray()
        var doubleArray = DoubleArray(strArray.size)
        for (i in strArray.indices) {
            doubleArray[i] = strArray[i].toDouble()
        }
        TrainingData.irisData = doubleArray

        strArray = str.split("#").toTypedArray()[1].split(",").toTypedArray()
        doubleArray = DoubleArray(strArray.size)
        for (i in strArray.indices) {
            doubleArray[i] = strArray[i].toDouble()
        }
        TrainingData.labelData = doubleArray
    }
}