package com.wms.panchkula.ui.login

import android.R
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.wms.panchkula.Global_Classes.AppConstants
import com.wms.panchkula.Global_Classes.GlobalMethods
import com.wms.panchkula.Global_Classes.MaterialProgressDialog
import com.wms.panchkula.Global_Notification.NetworkConnection
import com.wms.panchkula.ui.login.Model.LoginResponseModel
import com.wms.panchkula.Model.OtpErrorModel
import com.wms.panchkula.Retrofit_Api.NetworkClients
import com.wms.panchkula.Retrofit_Api.QuantityNetworkClient
import com.wms.panchkula.SessionManagement.SessionManagement
import com.wms.panchkula.Validation.Validation
import com.wms.panchkula.databinding.ActivityLoginBinding
import com.wms.panchkula.Retrofit_Api.ApiConstantForURL
import com.wms.panchkula.ui.home.HomeActivity
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.pixplicity.easyprefs.library.Prefs
import com.webapp.internetconnection.CheckNetwoorkConnection
import com.wms.panchkula.Global_Classes.AppConstants.isTestEnvUIVisible
import com.wms.panchkula.Model.ErrorModel
import com.wms.panchkula.Model.HomeItem
import com.wms.panchkula.Model.ModelDashboardItem
import com.wms.panchkula.SessionManagement.UserManagementPrefs
import com.wms.panchkula.ui.setting.SettingActivity
import com.wms.panchkula.ui.setting.model.ModelValidateUser
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.IOException
import java.net.SocketTimeoutException


class LoginActivity : AppCompatActivity() {
    private lateinit var activityLoginBinding: ActivityLoginBinding

    //    private var loginViewModel: LoginViewModel? = null
    lateinit var validation: Validation
    lateinit var networkConnection: NetworkConnection
    lateinit var checkNetwoorkConnection: CheckNetwoorkConnection

    lateinit var materialProgressDialog: MaterialProgressDialog
    private lateinit var sessionManagement: SessionManagement
    private lateinit var userPrefs: UserManagementPrefs

    val listHome = ArrayList<HomeItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activityLoginBinding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(activityLoginBinding.root)
        supportActionBar?.hide()

        //todo initialization...
        validation = Validation()
        networkConnection = NetworkConnection()
        checkNetwoorkConnection = CheckNetwoorkConnection(application)
        materialProgressDialog = MaterialProgressDialog(this@LoginActivity)
        sessionManagement = SessionManagement(this@LoginActivity)
        //userPrefs = UserManagementPrefs(this@LoginActivity)
        setDynamicHintLabels()

//        todo set dbname statically
        setStaticDbName()

        //todo
        sessionManagement.setFromWhere(applicationContext, "Login")

        val companyDB = sessionManagement.getCompanyDB(this)


        val DBUrl_url = Prefs.getString(AppConstants.DBUrl, "")

        val AppIP_url = Prefs.getString(AppConstants.AppIP, "")
        activityLoginBinding.edtAppUrl.setText(AppIP_url)
        activityLoginBinding.edtIPAddress.setText(DBUrl_url)

        //todo set code for check company db is null or not and set text auto---
        // activityLoginBinding.edtCompanyDB.setText(companyDB ?: "")
        activityLoginBinding.AcDbNameList.setText(companyDB ?: "", false)

        // todo dbNameList Api
//        callDBNameListApi()
       /* if (credentialEnabled) {
            activityLoginBinding.loginUsername.setText("manager")
            activityLoginBinding.loginPassword.setText("Sdbh@7448")
        } else {
            activityLoginBinding.loginUsername.setText("")
            activityLoginBinding.loginPassword.setText("")
        }*/

        activityLoginBinding.apply {
            switchForEnvironment.visibility = if (isTestEnvUIVisible) View.VISIBLE else View.GONE

            if (Prefs.getBoolean(AppConstants.IS_TEST_ENVIRONMENT)) {
                switchForEnvironment.isChecked = true
                switchForEnvironment.text = "Live Environment (Port : 9090)"
                Log.e("Environment", "SettingActivity: Default => ${Prefs.getBoolean(AppConstants.IS_TEST_ENVIRONMENT)}")
            } else {
                switchForEnvironment.isChecked = false
                switchForEnvironment.text = "Test Environment (Port : 9092)"
                Log.e("Environment", "SettingActivity: Default => ${Prefs.getBoolean(AppConstants.IS_TEST_ENVIRONMENT)}")
            }

            switchForEnvironment.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    Prefs.putBoolean(AppConstants.IS_TEST_ENVIRONMENT, true)
                    switchForEnvironment.text = "Live Environment (Port : 9090)"
                    Log.e("Environment", "SettingActivity: switchForEnvironment.setOnCheckedChangeListener => ${Prefs.getBoolean(AppConstants.IS_TEST_ENVIRONMENT)}")
                } else {
                    Prefs.putBoolean(AppConstants.IS_TEST_ENVIRONMENT, false)
                    switchForEnvironment.text = "Test Environment (Port : 9092)"
                    Log.e("Environment", "SettingActivity: switchForEnvironment.setOnCheckedChangeListener => ${Prefs.getBoolean(AppConstants.IS_TEST_ENVIRONMENT)}")
                }
            }

            edtIPAddress.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    edtAppUrl.setText(s.toString())
                }

                override fun afterTextChanged(s: Editable?) {

                }
            })
        }

        //todo Place cursor at the end of text in EditText
        //activityLoginBinding.loginUsername.setSelection(activityLoginBinding.loginUsername.length())


        val IpAddress = Prefs.getString(AppConstants.DBUrl)

        Log.e("IpAddress===>", "onCreate: " + IpAddress)

        //todo set code for check IpAddress is null or not and set text auto---
        activityLoginBinding.edtIPAddress.setText(IpAddress ?: "")


        val AppIP = Prefs.getString(AppConstants.AppIP)

        Log.e("AppIP===>", "onCreate: " + AppIP)

        //todo set code for check IpAddress is null or not and set text auto---
        activityLoginBinding.edtAppUrl.setText(AppIP ?: "")


        //todo login click listener..
        activityLoginBinding.loginButton.setOnClickListener {
            apiCall()
        }
    }

    private fun setDynamicHintLabels() {
        /*activityLoginBinding.apply {
            inputLayoutDbUrl.setupDynamicHint(edtIPAddress,"Enter DB Url","Database Url")
        }*/
    }

    private fun setStaticDbName() {
        val dbNames = arrayListOf("TEST_DB_11092025","SDBH_LIVE_NEW", "TEST_10022025", "TEST_WMS_17072025", "TEST_01082025", "TEST_21082025", "TEST_27082025")

        val adapter = ArrayAdapter(
            this@LoginActivity,
            R.layout.simple_spinner_dropdown_item,
            dbNames
        )

        activityLoginBinding.AcDbNameList.setAdapter(adapter)
        activityLoginBinding.AcDbNameList.hint = "Select DB"

        activityLoginBinding.AcDbNameList.threshold = 0 // Show suggestions even without typing

        // Handle item selection
        activityLoginBinding.AcDbNameList.setOnItemClickListener { parent, _, position, _ ->
            val selectedItem = adapter.getItem(position)
            activityLoginBinding.AcDbNameList.setText(selectedItem, false) // false to prevent filtering again
        }
    }

    private fun apiCall() {
        if (networkConnection.getConnectivityStatusBoolean(applicationContext)) {

            if (activityLoginBinding.edtIPAddress.text.toString().isNullOrEmpty()) {
                activityLoginBinding.edtIPAddress.error = "Please enter your DB URL."
            } else if (activityLoginBinding.edtAppUrl.text.toString().isNullOrEmpty()) {
                activityLoginBinding.edtAppUrl.error = "Please enter your App URL."
            } else if (activityLoginBinding.AcDbNameList.text.toString().isNullOrEmpty()) {
                activityLoginBinding.AcDbNameList.error = "Please enter your Company DB"
            } else if (activityLoginBinding.loginUsername.text.toString().isNullOrEmpty()) {
                activityLoginBinding.loginUsername.error = "Please enter user name"
            } else if (activityLoginBinding.loginPassword.text.toString().isNullOrEmpty()) {
                activityLoginBinding.loginPassword.error = "Please enter password"
            } else {
                materialProgressDialog.show()
                sessionManagement.setCompanyDB(applicationContext, activityLoginBinding.AcDbNameList.getText().toString())

                Prefs.putString(AppConstants.DBUrl, activityLoginBinding.edtIPAddress.getText().toString().trim())

                Prefs.putString(AppConstants.AppIP, activityLoginBinding.edtAppUrl.getText().toString().trim())

                var apiConfig = ApiConstantForURL()

                NetworkClients.updateBaseUrlFromConfig(apiConfig)

                QuantityNetworkClient.updateBaseUrlFromConfig(apiConfig)

                //callLoginApi()
                val userName = activityLoginBinding.loginUsername.text.toString().trim()
                val userPass = activityLoginBinding.loginPassword.text.toString().trim()
                val networkClient = QuantityNetworkClient.create(this)
                networkClient.validateUser(userName, userPass).apply {
                    enqueue(object : Callback<ModelValidateUser> {
                        override fun onResponse(call: Call<ModelValidateUser>, response: Response<ModelValidateUser>) {
                            try {
                                if (response.isSuccessful) {
                                    materialProgressDialog.dismiss()
                                    //materialProgressDialog.dismiss()
                                    if (response.body()?.value?.get(0)?.validUser.equals("true", true)) {
                                        //callGetBranchList()
                                        sessionManagement.setUsername(this@LoginActivity, userName)
                                        //callUserAccessMgmt(userName)
                                        val password = response.body()?.value?.get(0)?.password
                                        sessionManagement.setSapPassword(this@LoginActivity, password)
                                        //password?.let { callLoginApi(it) }
                                        callLoginApi(userName, userPass)
                                    } else {
                                        GlobalMethods.showError(this@LoginActivity, "You are not an authorized WMS user.")
                                    }

                                } else if (response.code() == 500) {
                                    val gson = GsonBuilder().create()
                                    var mError: ErrorModel
                                    try {
                                        val s = response.errorBody()!!.string()
                                        mError = gson.fromJson(s, ErrorModel::class.java)

                                        GlobalMethods.showError(this@LoginActivity, mError.Message)
                                        Log.e("json_error------", mError.Message)

                                    } catch (e: IOException) {
                                        e.printStackTrace()
                                    }
                                } else {

                                    Prefs.clear()
                                    sessionManagement.setUsername(this@LoginActivity, "")

                                    val gson1 = GsonBuilder().create()
                                    var mError: OtpErrorModel
                                    try {
                                        val s = response.errorBody()!!.string()
                                        mError = gson1.fromJson(s, OtpErrorModel::class.java)
                                        if (mError.error.code.equals(400)) {
                                            GlobalMethods.showError(this@LoginActivity, mError.error.message.value)
                                        }
                                        if (mError.error.message.value != null) {
                                            GlobalMethods.showError(this@LoginActivity, mError.error.message.value)
                                            Log.e("json_error------", mError.error.message.value)
                                        }
                                    } catch (e: IOException) {
                                        e.printStackTrace()
                                    }
                                }

                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }

                        override fun onFailure(call: Call<ModelValidateUser>, t: Throwable) {
                            Log.e("login_api_failure-----", t.toString())
                            GlobalMethods.showError(this@LoginActivity, t.toString())
                            materialProgressDialog.dismiss()
//                                Prefs.clear()
                            Log.e("DBURL==>", "onFailure: " + Prefs.getString(AppConstants.DBUrl, ""))

                            Toast.makeText(this@LoginActivity, t.message, Toast.LENGTH_SHORT)
                        }

                    })
                }

                /*var jsonObject: JsonObject = JsonObject()
                jsonObject.addProperty("CompanyDB", sessionManagement.getCompanyDB(this))
                jsonObject.addProperty("Password", activityLoginBinding.loginPassword.text.toString())
                jsonObject.addProperty("UserName", activityLoginBinding.loginUsername.text.toString())
                var networkClient = NetworkClients.create(this)
                networkClient.doGetLoginCall(jsonObject).apply {
                    enqueue(object : Callback<LoginResponseModel> {
                        override fun onResponse(call: Call<LoginResponseModel>, response: Response<LoginResponseModel>) {
                            try {
                                if (response.isSuccessful) {
                                    materialProgressDialog.dismiss()
                                    var loginResponseModel = response.body()!!
                                    //todo shares preference store...
                                    sessionManagement.setSessionId(this@LoginActivity, loginResponseModel.SessionId)
                                    sessionManagement.setSessionTimeout(this@LoginActivity, loginResponseModel.SessionTimeout)
                                    sessionManagement.setFromWhere(this@LoginActivity, "ElseCase")
                                    Log.e("api_success-----", response.toString())
                                    var intent: Intent = Intent(this@LoginActivity, HomeActivity::class.java)
                                    startActivity(intent)
                                    finish()
                                    GlobalMethods.showSuccess(this@LoginActivity, "Successfully Login.")
                                } else {
                                    materialProgressDialog.dismiss()

                                    Prefs.clear()

                                    val gson1 = GsonBuilder().create()
                                    var mError: OtpErrorModel
                                    try {
                                        val s = response.errorBody()!!.string()
                                        mError = gson1.fromJson(s, OtpErrorModel::class.java)
                                        if (mError.error.code.equals(400)) {
                                            GlobalMethods.showError(this@LoginActivity, mError.error.message.value)
                                        }
                                        if (mError.error.message.value != null) {
                                            GlobalMethods.showError(this@LoginActivity, mError.error.message.value)
                                            Log.e("json_error------", mError.error.message.value)
                                        }
                                    } catch (e: IOException) {
                                        e.printStackTrace()
                                    }
                                }

                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }

                        override fun onFailure(call: Call<LoginResponseModel>, t: Throwable) {
                            Log.e("login_api_failure-----", t.toString())
                            materialProgressDialog.dismiss()
//                                Prefs.clear()
                            Log.e("DBURL==>", "onFailure: " + Prefs.getString(AppConstants.DBUrl, ""))

                            Toast.makeText(this@LoginActivity, t.message, Toast.LENGTH_SHORT)
                        }

                    })
                }*/

            }
        } else {
            materialProgressDialog.dismiss()
            AlertDialog.Builder(this)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle("Internet Connection Alert")
                .setMessage("Please Check Your Internet Connection")
                .setPositiveButton("Close") { dialogInterface, i ->
                    finish()
                }.show()
        }
    }

    private fun callUserAccessMgmt(userName: String) {
        var networkClient = QuantityNetworkClient.create(this)
        networkClient.userAccessMgmt(userName).apply {
            enqueue(object : Callback<ModelDashboardItem> {
                override fun onResponse(call: Call<ModelDashboardItem>, response: Response<ModelDashboardItem>) {
                    try {
                        if (response.isSuccessful) {
                            materialProgressDialog.dismiss()
                            var userAccessResponse = response.body()!!
                            userPrefs.saveUserMgmtData(userAccessResponse)
                            //GlobalMethods.showSuccess(this@LoginActivity, "Successfully Login.")
                        } else {
                            materialProgressDialog.dismiss()

                            Prefs.clear()

                            val gson1 = GsonBuilder().create()
                            var mError: OtpErrorModel
                            try {
                                val s = response.errorBody()!!.string()
                                mError = gson1.fromJson(s, OtpErrorModel::class.java)
                                if (mError.error.code.equals(400)) {
                                    GlobalMethods.showError(this@LoginActivity, mError.error.message.value)
                                }
                                if (mError.error.message.value != null) {
                                    GlobalMethods.showError(this@LoginActivity, mError.error.message.value)
                                    Log.e("json_error------", mError.error.message.value)
                                }
                            } catch (e: IOException) {
                                e.printStackTrace()
                            }
                        }

                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                override fun onFailure(call: Call<ModelDashboardItem>, t: Throwable) {
                    Log.e("login_api_failure-----", t.toString())
                    materialProgressDialog.dismiss()
                    when (t) {
                        is SocketTimeoutException -> {
                            GlobalMethods.showError(this@LoginActivity, "Connection timed out. Please try again.")
                        }

                        is IOException -> {
                            GlobalMethods.showError(this@LoginActivity, "Network error. Please check your internet connection.")
                        }

                        else -> {
                            GlobalMethods.showError(this@LoginActivity, "Something went wrong: ${t.localizedMessage}")
                        }
                    }
//                                Prefs.clear()
                    Log.e("DBURL==>", "onFailure: " + Prefs.getString(AppConstants.DBUrl, ""))

                    Toast.makeText(this@LoginActivity, t.message, Toast.LENGTH_SHORT)
                }

            })
        }
    }

    fun callLoginApi(userName:String, password: String) {
        materialProgressDialog.show()
        var jsonObject: JsonObject = JsonObject()
        jsonObject.addProperty("CompanyDB", sessionManagement.getCompanyDB(this))
        jsonObject.addProperty("Password", password)
        jsonObject.addProperty("UserName", userName) //"manager"
        var networkClient = NetworkClients.create(this)
        networkClient.doGetLoginCall(jsonObject).apply {
            enqueue(object : Callback<LoginResponseModel> {
                override fun onResponse(call: Call<LoginResponseModel>, response: Response<LoginResponseModel>) {
                    try {
                        if (response.isSuccessful) {
                            materialProgressDialog.dismiss()
                            var loginResponseModel = response.body()!!
                            //todo shares preference store...
                            sessionManagement.setSessionId(this@LoginActivity, loginResponseModel.SessionId)
                            sessionManagement.setSessionTimeout(this@LoginActivity, loginResponseModel.SessionTimeout)
                            sessionManagement.setFromWhere(this@LoginActivity, "ElseCase")
                            Log.e("api_success-----", response.toString())
                            if (Prefs.getString(AppConstants.BPLID, "").isNotEmpty()) {
                                var intent: Intent = Intent(this@LoginActivity, HomeActivity::class.java)
                                startActivity(intent)
                            } else {
                                var intent: Intent = Intent(this@LoginActivity, SettingActivity::class.java)
                                startActivity(intent)
                            }
                            finish()
                            GlobalMethods.showSuccess(this@LoginActivity, "Successfully Login.")
                        } else {
                            materialProgressDialog.dismiss()

                            Prefs.clear()

                            val gson1 = GsonBuilder().create()
                            var mError: OtpErrorModel
                            try {
                                val s = response.errorBody()!!.string()
                                mError = gson1.fromJson(s, OtpErrorModel::class.java)
                                if (mError.error.code.equals(400)) {
                                    GlobalMethods.showError(this@LoginActivity, mError.error.message.value)
                                }
                                if (mError.error.message.value != null) {
                                    GlobalMethods.showError(this@LoginActivity, mError.error.message.value)
                                    Log.e("json_error------", mError.error.message.value)
                                }
                            } catch (e: IOException) {
                                e.printStackTrace()
                            }
                        }

                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                override fun onFailure(call: Call<LoginResponseModel>, t: Throwable) {
                    Log.e("login_api_failure-----", t.toString())
                    materialProgressDialog.dismiss()
                    when (t) {
                        is SocketTimeoutException -> {
                            GlobalMethods.showError(this@LoginActivity, "Connection timed out. Please try again.")
                        }

                        is IOException -> {
                            GlobalMethods.showError(this@LoginActivity, "Network error. Please check your internet connection.")
                        }

                        else -> {
                            GlobalMethods.showError(this@LoginActivity, "Something went wrong: ${t.localizedMessage}")
                        }
                    }
//                                Prefs.clear()
                    Log.e("DBURL==>", "onFailure: " + Prefs.getString(AppConstants.DBUrl, ""))

                    Toast.makeText(this@LoginActivity, t.message, Toast.LENGTH_SHORT)
                }

            })
        }
    }

}