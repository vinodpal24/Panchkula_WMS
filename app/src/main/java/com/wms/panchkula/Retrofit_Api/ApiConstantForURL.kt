package com.wms.panchkula.Retrofit_Api

import com.wms.panchkula.Global_Classes.AppConstants
import com.pixplicity.easyprefs.library.Prefs
import com.wms.panchkula.Global_Classes.AppConstants.isDevelopmentForClient
import com.wms.panchkula.Global_Classes.AppConstants.isTestEnvUIVisible

class ApiConstantForURL {

    private val isDevelopment: Boolean =
        if (isTestEnvUIVisible) !Prefs.getBoolean(AppConstants.IS_TEST_ENVIRONMENT) else false

    // expose the port globally
    val PORT: Int
        get() = if (isDevelopment || isDevelopmentForClient) 9092 else 9090

    val BASE_URL: String
        get() = "http://${Prefs.getString(AppConstants.DBUrl)}:50001/b1s/v1/"

    val QUANTITY_BASE_URL: String
        get() {
            val ip = Prefs.getString(AppConstants.AppIP)
            return "http://$ip:$PORT/api/"
        }
}


/*
class ApiConstantForURL {
    private val isDevelopment: Boolean = if (isTestEnvUIVisible) !Prefs.getBoolean(AppConstants.IS_TEST_ENVIRONMENT) else false

    val BASE_URL: String
        get() = "http://${Prefs.getString(AppConstants.DBUrl)}:50001/b1s/v1/"

    val QUANTITY_BASE_URL: String

        get() {
            val port = if (isDevelopment) 9092 else 9090
            val ip = Prefs.getString(AppConstants.AppIP)
            return "http://$ip:$port/api/"
        }
}*/
