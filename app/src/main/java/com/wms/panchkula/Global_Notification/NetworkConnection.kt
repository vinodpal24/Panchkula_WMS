package com.wms.panchkula.Global_Notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager

class NetworkConnection : BroadcastReceiver() {
    var TYPE_WIFI = 1
    var TYPE_MOBILE = 2
    var TYPE_NOT_CONNECTED = 0

    fun getConnectivityStatus(context: Context): Int {
        val cm = context
            .getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = cm.activeNetworkInfo
        if (null != activeNetwork) {
            if (activeNetwork.type == ConnectivityManager.TYPE_WIFI) return TYPE_WIFI
            if (activeNetwork.type == ConnectivityManager.TYPE_MOBILE) return TYPE_MOBILE
        }
        return TYPE_NOT_CONNECTED
    }

    fun getConnectivityStatusBoolean(context: Context): Boolean {
        val conn: Int = getConnectivityStatus(context)
        if (conn == TYPE_WIFI) {
            return true //for wifi
        } else if (conn == TYPE_MOBILE) {
            return true //for mobiledata
        } else if (conn == TYPE_NOT_CONNECTED) {
            return false // for not connected
        }
        return false
    }


    override fun onReceive(context: Context, p1: Intent?) {
        val speedtest: Boolean = getConnectivityStatusBoolean(context)
        if (speedtest) {
            val i = Intent("checkInterNetBackground")
            i.putExtra("message", true)
            context.sendBroadcast(i)
        } else {
            val i = Intent("checkInterNetBackground")
            i.putExtra("message", false)
            context.sendBroadcast(i)
        }
    }
}