package com.wms.panchkula.Retrofit_Api

import android.content.Context
import com.wms.panchkula.Global_Classes.GlobalMethods.isVpnConnected
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

class VpnCheckInterceptor(private val context: Context) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        if (!isVpnConnected(context)) {
            VpnToastManager.showToastOnce(context, "VPN is not connected. Please connect and try again.")

            throw IOException("VPN is not connected. Please connect and try again.")
        }
        return chain.proceed(chain.request())
    }
}
