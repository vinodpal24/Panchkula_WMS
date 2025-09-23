package com.wms.panchkula.SessionManagement

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import com.wms.panchkula.Global_Classes.AppConstants

class SessionManagement(_context: Context) {

    var _context: Context
    init {
        this._context = _context
    }

    private val NAME = "ProductionApp"
    private val MODE = Context.MODE_PRIVATE

    // todo Shared Preferences
    var pref: SharedPreferences = _context.getSharedPreferences(NAME, MODE)

    //todo  Editor for Shared preferences
    lateinit var editor: SharedPreferences.Editor

    //todo code for first login...
    private val IS_FIRST_RUN = "IsFirstRun+"

    // todo All Shared Preferences Keys
    private val IS_LOGIN = "IsLoggedIn"


    //todo Shared preference String value store..
    fun setSharedPrefernce(context: Context, key: String, value: String) {
        editor = pref.edit()
        editor.putString(key, value )
        editor.commit()
    }

    private fun getDataFromSharedPreferences(context: Context, Key: String): String? {
        return try {
            val returnString: String? = pref.getString(Key, null)
            returnString
        } catch (e: java.lang.Exception) {
            ""
        }
    }

    //todo Shared preference int value store..
    fun setIntSharedPrefernce(key: String, value: Int) {
        editor = pref.edit()
        editor.putInt(key, value)
        editor.commit()
    }

    /*fun saveUserMgmtData(key: String, data: ModelDashboardItem) {
        editor = pref.edit()
        val gson = Gson()
        val json = gson.toJson(data) // Convert the object to JSON
        editor.putString(key, json)
        editor.apply()
    }

    // Retrieve EmployeeAtLoginData from JSON
    fun getUserMgmtData(key: String): ModelDashboardItem? {
        val gson = Gson()
        val json = pref.getString(key, null)
        return if (json != null) {
            gson.fromJson(json, ModelDashboardItem::class.java) // Convert JSON back to the object
        } else {
            null // Return null if no data exists
        }
    }

    fun clearUserMgmtData() {
        editor = pref.edit()
        editor.remove(AppConstants.USER_MGMT_DATA) // Example: Clear user mgmt data
        editor.commit()
    }*/

    private fun getIntDataFromSharedPreferences(Key: String): Int {
        val returnInt: Int = pref.getInt(Key, 0)
        return returnInt
    }

    fun ClearSession(mContext: Context) {
        val editor: SharedPreferences.Editor = PreferenceManager.getDefaultSharedPreferences(mContext).edit()
        editor.clear()
        editor.commit()
    }

    fun setSessionId(context: Context, SessionId: String?) {
        if (SessionId != null) {
            setSharedPrefernce(context, AppConstants.SESSION_ID, SessionId)
        }
    }

    fun getSessionId(context: Context): String? {
        return getDataFromSharedPreferences(context, AppConstants.SESSION_ID)
    }

    fun setSessionTimeout(context: Context, SessionTimeout: String?) {
        if (SessionTimeout != null) {
            setSharedPrefernce(context, AppConstants.SESSION_TIMEOUT, SessionTimeout)
        }
    }

    fun getSessionTimeout(context: Context): String? {
        return getDataFromSharedPreferences(context, AppConstants.SESSION_TIMEOUT)
    }

    fun setFromWhere(context: Context, fromWhere: String?) {
        if (fromWhere != null) {
            setSharedPrefernce(context, AppConstants.FromWhere, fromWhere)
        }
    }

    fun getFromWhere(context: Context): String? {
        return getDataFromSharedPreferences(context, AppConstants.FromWhere)
    }

    fun setCompanyDB(context: Context, CompanyDB: String?) {
        if (CompanyDB != null) {
            setSharedPrefernce(context, AppConstants.COMPANY_DB, CompanyDB)
        }
    }

    fun getCompanyDB(context: Context): String? {
        return getDataFromSharedPreferences(context, AppConstants.COMPANY_DB)
    }

    fun setIPAddress(context: Context, ip: String?) {
        if (ip != null) {
            setSharedPrefernce(context, AppConstants.DBUrl, ip)
        }
    }

    fun getIPAddress(context: Context): String? {
        return getDataFromSharedPreferences(context, AppConstants.DBUrl)
    }

    fun setWarehouseCode(context: Context, Warehouse: String?) {
        if (Warehouse != null) {
            setSharedPrefernce(context, AppConstants.WHAREHOUSE, Warehouse)
        }
    }

    fun getWarehouseCode(context: Context): String? {
        return getDataFromSharedPreferences(context, AppConstants.WHAREHOUSE)
    }

    fun setInvReqWarehouseCode(context: Context, Warehouse: String?) {
        if (Warehouse != null) {
            setSharedPrefernce(context, AppConstants.WHAREHOUSE, Warehouse)
        }
    }

    fun getInvReqWarehouseCode(context: Context): String? {
        return getDataFromSharedPreferences(context, AppConstants.WHAREHOUSE)
    }

    fun setWarehouseCode(context: Context, warehouseCode: String?, type: String) {
        if (warehouseCode != null) {
            val key = when (type) {
                "From_Warehouse" -> AppConstants.FROM_WAREHOUSE
                "To_Warehouse" -> AppConstants.TO_WAREHOUSE
                else -> return // or throw IllegalArgumentException("Invalid warehouse type")
            }
            setSharedPrefernce(context, key, warehouseCode)
        }
    }

    fun getWarehouseCode(context: Context, type: String): String? {
        val key = when (type) {
            "From_Warehouse" -> AppConstants.FROM_WAREHOUSE
            "To_Warehouse" -> AppConstants.TO_WAREHOUSE
            else -> return null // or throw IllegalArgumentException("Invalid warehouse type")
        }
        return getDataFromSharedPreferences(context, key)
    }

    fun setQRScanner(scanner_check: Int?) {
        if (scanner_check != null) {
            setIntSharedPrefernce( AppConstants.SCANNER_CHECK, scanner_check)
        }
    }

    fun getQRScannerCheck(): Int? {
        return getIntDataFromSharedPreferences(AppConstants.SCANNER_CHECK)
    }

    fun setLaser(leaser_check: Int?) {
        if (leaser_check != null) {
            setIntSharedPrefernce(AppConstants.LEASER_CHECK, leaser_check)
        }
    }

    fun getLeaserCheck(): Int? {
        return getIntDataFromSharedPreferences(AppConstants.LEASER_CHECK)
    }

    fun setScannerType(context: Context, type: String?) {
        if (type != null) {
            setSharedPrefernce(context, AppConstants.SCANNER_TYPE, type)
        }
    }

    fun getScannerType(context: Context): String? {
        return getDataFromSharedPreferences(context, AppConstants.SCANNER_TYPE)
    }

    fun setPassword(context: Context, type: String?) {
        if (type != null) {
            setSharedPrefernce(context, AppConstants.USER_PASSWORD, type)
        }
    }

    fun getPassword(context: Context): String? {
        return getDataFromSharedPreferences(context, AppConstants.USER_PASSWORD)
    }

    fun setSapPassword(context: Context, type: String?) {
        if (type != null) {
            setSharedPrefernce(context, "sap_password", type)
        }
    }

    fun getSapPassword(context: Context): String? {
        return getDataFromSharedPreferences(context, "sap_password")
    }

    fun setUsername(context: Context, type: String?) {
        if (type != null) {
            setSharedPrefernce(context, AppConstants.USER_NAME, type)
        }
    }

    fun getUsername(context: Context): String? {
        return getDataFromSharedPreferences(context, AppConstants.USER_NAME)
    }

}