package com.wms.panchkula.Retrofit_Api

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Toast

object VpnToastManager {
    private var isToastShown = false

    fun showToastOnce(context: Context, message: String) {

            Handler(Looper.getMainLooper()).post {
                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            }

    }

    fun reset() {
        isToastShown = false
    }
}
