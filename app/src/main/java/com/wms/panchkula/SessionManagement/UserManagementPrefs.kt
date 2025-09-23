package com.wms.panchkula.SessionManagement

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.wms.panchkula.Model.ModelDashboardItem

class UserManagementPrefs(context: Context) {

    private val PREF_NAME = "UserPrefs"
    private val USER_DATA_KEY = "user_mgmt_data"
    private val pref: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    fun saveUserMgmtData(data: ModelDashboardItem) {
        val gson = Gson()
        val json = gson.toJson(data)
        pref.edit().putString(USER_DATA_KEY, json).apply()
    }

    fun getUserMgmtData(): ModelDashboardItem? {
        val json = pref.getString(USER_DATA_KEY, null)
        return if (json != null) {
            Gson().fromJson(json, ModelDashboardItem::class.java)
        } else {
            null
        }
    }

    fun clearUserMgmtData() {
        pref.edit().remove(USER_DATA_KEY).apply()
    }
}