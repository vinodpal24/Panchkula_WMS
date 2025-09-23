package com.wms.panchkula.ui.setting

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import com.wms.panchkula.R
import com.wms.panchkula.Retrofit_Api.ApiConstantForURL
import com.wms.panchkula.Retrofit_Api.QuantityNetworkClient
import com.wms.panchkula.databinding.ActivitySettingBinding
import com.wms.panchkula.databinding.LayoutSapLoginAlertBinding
import com.wms.panchkula.databinding.LayoutSapLoginBinding
import com.google.gson.GsonBuilder
import com.pixplicity.easyprefs.library.Prefs
import com.webapp.internetconnection.CheckNetwoorkConnection
import com.wms.panchkula.Global_Classes.AppConstants
import com.wms.panchkula.Global_Classes.AppConstants.isTestEnvUIVisible
import com.wms.panchkula.Global_Classes.GlobalMethods
import com.wms.panchkula.Global_Classes.GlobalMethods.handleFailureError
import com.wms.panchkula.Global_Classes.MaterialProgressDialog
import com.wms.panchkula.Model.OtpErrorModel
import com.wms.panchkula.SessionManagement.SessionManagement
import com.wms.panchkula.ui.home.HomeActivity
import com.wms.panchkula.ui.login.LoginActivity
import com.wms.panchkula.ui.setting.model.ModelGetBranch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.IOException

class SettingActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingBinding
    lateinit var dialogBinding: LayoutSapLoginBinding
    lateinit var checkNetwoorkConnection: CheckNetwoorkConnection
    lateinit var materialProgressDialog: MaterialProgressDialog
    private var branchList_gl: ArrayList<ModelGetBranch.Value> = ArrayList()
    private var selectedBranchName = ""
    private var selectedBranchCode = ""

    lateinit var sessionManagement: SessionManagement
    private var selectedRowItems = ""
    private var selectedRowItemsInt = 0

    val items = listOf("2 items in each row", "3 items in each row", "4 items in each row")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySettingBinding.inflate(layoutInflater)
        initViews()
        clickListeners()
        setContentView(binding.root)
    }


    private fun initViews() {
        supportActionBar?.hide()
        binding.apply {
            switchForEnvironment.visibility = if(isTestEnvUIVisible) View.VISIBLE else View.GONE
            if (Prefs.getBoolean(AppConstants.IS_TEST_ENVIRONMENT)) {
                switchForEnvironment.isChecked = true
                switchForEnvironment.text = "Live Environment (Port : 9090)"
                Log.e("Environment", "SettingActivity: Default => ${Prefs.getBoolean(AppConstants.IS_TEST_ENVIRONMENT)}")
            } else {
                switchForEnvironment.isChecked = false
                switchForEnvironment.text = "Test Environment (Port : 9092)"
                Log.e("Environment", "SettingActivity: Default => ${Prefs.getBoolean(AppConstants.IS_TEST_ENVIRONMENT)}")
            }
        }
        materialProgressDialog = MaterialProgressDialog(this@SettingActivity)
        checkNetwoorkConnection = CheckNetwoorkConnection(application)
        sessionManagement = SessionManagement(this)
        sessionManagement.setScannerType(this, "LEASER")
        callGetBranchList()
        setRowItemSpinner(items)
    }

    private fun setRowItemSpinner(items: List<String>) {
        val adapter = ArrayAdapter(this, R.layout.drop_down_item_textview, items)
        binding.acSpanCount.setAdapter(adapter)
        val spanCount = if (Prefs.getInt(AppConstants.ITEM_IN_ROW) == 0) 2 else Prefs.getInt(AppConstants.ITEM_IN_ROW)
        when (spanCount) {
            2 -> {
                binding.acSpanCount.setText(items[0], false)
            }

            3 -> {
                binding.acSpanCount.setText(items[1], false)
            }

            4 -> {
                binding.acSpanCount.setText(items[2], false)
            }
        }
        binding.acSpanCount.setOnItemClickListener { _, _, position, _ ->
            val selectedItem = items[position]
            selectedRowItems = selectedItem
            selectedRowItemsInt = if (selectedItem.equals("2 items in each row", true)) 2 else if (selectedItem.equals("3 items in each row", true)) 3 else 4
            Prefs.putInt(AppConstants.ITEM_IN_ROW, selectedRowItemsInt)
            binding.acSpanCount.setText(selectedItem, false)
        }
    }

    private fun clickListeners() {
        binding.apply {
            tvChooseScannerType.setOnClickListener {
                chooseScannerPopupDialog()
            }

            ivBackArrow.setOnClickListener {

                onBackPressed()
            }

            tvSapLogin.setOnClickListener {
                callSapLogin(true)

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
                callGetBranchList()
            }
        }
    }

    override fun onBackPressed() {
        val savedBPLID = Prefs.getString(AppConstants.BPLID, "")
        if (savedBPLID.isNotEmpty()) {
            var intent: Intent = Intent(this@SettingActivity, HomeActivity::class.java)
            startActivity(intent)
            finish()
            super.onBackPressed()
        } else {
            GlobalMethods.showError(this@SettingActivity, "First you have to select branch then go back")
        }

    }

    private fun callSapLogin(isAdmin: Boolean) {
        if (isAdmin) {
            openSAPLoginDialog(this@SettingActivity)
        } else {
            openAlertDialog(this@SettingActivity)
        }
    }

    fun callGetBranchList() {
        checkNetwoorkConnection.observe(this) { isConnected ->
            if (isConnected) {
                materialProgressDialog.show()
                var apiConfig = ApiConstantForURL()

               QuantityNetworkClient.updateBaseUrlFromConfig(apiConfig)

                val networkClient = QuantityNetworkClient.create(this)
                networkClient.getBranchList().apply {
                    enqueue(object : Callback<ModelGetBranch> {
                        override fun onResponse(
                            call: Call<ModelGetBranch>,
                            response: Response<ModelGetBranch>
                        ) {
                            try {
                                if (response.isSuccessful) {

                                    materialProgressDialog.dismiss()
                                    var branches = response.body()?.value
                                    Toast.makeText(this@SettingActivity, "Successfully!", Toast.LENGTH_SHORT)
                                    if (!branches.isNullOrEmpty() && branches.size > 0) {

                                        branchList_gl.addAll(branches)
                                        if (branchList_gl.size == 0) {
                                            Toast.makeText(this@SettingActivity, "No Branch Found.", Toast.LENGTH_SHORT).show()
                                        } else {
                                            val branchList = branchList_gl.map { it.bPLName }
                                            val adapter = ArrayAdapter(this@SettingActivity, R.layout.drop_down_item_textview, branchList)
                                            binding.acBranches.setAdapter(adapter)

                                            if (branchList_gl.isNotEmpty()) {
                                                val savedBPLID = Prefs.getString(AppConstants.BPLID, "")
                                                val matchedBranch = branchList_gl.find { it.bPLID == savedBPLID }

                                                if (matchedBranch != null) {
                                                    selectedBranchName = matchedBranch.bPLName ?: ""
                                                    selectedBranchCode = matchedBranch.bPLID ?: ""
                                                    Log.e("FILTER_DIALOG", "Already Saved Branch -> $selectedBranchName ($selectedBranchCode)")
                                                } /*else {
                                                    val defaultBranch = branchList_gl[0]
                                                    selectedBranchName = defaultBranch.bPLName ?: ""
                                                    selectedBranchCode = defaultBranch.bPLID ?: ""
                                                    Prefs.putString(AppConstants.BPLID, selectedBranchCode) // update prefs
                                                    Log.e("FILTER_DIALOG", "Default Branch -> $selectedBranchName ($selectedBranchCode)")
                                                }*/

                                                binding.acBranches.setText(selectedBranchName, false)
                                                Log.e("FILTER_DIALOG", "Set n Save to Prefs: Branch -> $selectedBranchName ($selectedBranchCode)")
                                            }

                                            binding.acBranches.setOnItemClickListener { parent, _, position, _ ->
                                                val branch = parent.getItemAtPosition(position) as String
                                                selectedBranchName = branch
                                                selectedBranchCode = branches[position].bPLID.toString()
                                                Prefs.putString(AppConstants.BPLID, selectedBranchCode)
                                                Log.e("FILTER_DIALOG", "Selected Branch -> $selectedBranchName ($selectedBranchCode)")
                                                if (!binding.acBranches.text.toString().isNullOrEmpty()) {
                                                    var intent: Intent = Intent(this@SettingActivity, HomeActivity::class.java)
                                                    startActivity(intent)
                                                    finish()
                                                }

                                                if (branch.isNotEmpty()) {
                                                    binding.acBranches.setText(branch, false)
                                                } else {
                                                    selectedBranchName = ""
                                                    binding.acBranches.setText("")
                                                }
                                            }
                                        }


                                    }

                                } else {
                                    materialProgressDialog.dismiss()
                                    val gson1 = GsonBuilder().create()
                                    var mError: OtpErrorModel
                                    try {
                                        val s = response.errorBody()!!.string()
                                        mError = gson1.fromJson(s, OtpErrorModel::class.java)
                                        Log.e("MSZ==>", mError.error.message.value)
                                        if (mError.error.code == 400) {
                                            GlobalMethods.showError(this@SettingActivity, mError.error.message.value)
                                        }
                                        if (mError.error.code == 306 && mError.error.message.value != null) {
                                            GlobalMethods.showError(this@SettingActivity, mError.error.message.value)
                                            val mainIntent = Intent(this@SettingActivity, LoginActivity::class.java)
                                            startActivity(mainIntent)
                                            finish()
                                        }
                                        /*if (mError.error.message.value != null) {
                                            AppConstants.showError(this@ProductionListActivity, mError.error.message.value)
                                            Log.e("json_error------", mError.error.message.value)
                                        }*/
                                    } catch (e: IOException) {
                                        e.printStackTrace()
                                    }
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }

                        override fun onFailure(call: Call<ModelGetBranch>, t: Throwable) {
                            Log.e("issueCard_failure-----", t.toString())
                            handleFailureError(this@SettingActivity,t)
                            materialProgressDialog.dismiss()
                        }
                    })
                }

            } else {
                materialProgressDialog.dismiss()
                GlobalMethods.showError(this, "No Network Connection")
            }
        }

    }

    private fun openSAPLoginDialog(context: Context) {
        val dialog = Dialog(context, R.style.Theme_Dialog)
        val dialogBinding = LayoutSapLoginBinding.inflate(LayoutInflater.from(context))
        dialog.setContentView(dialogBinding.root)

        dialog.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setGravity(Gravity.CENTER)
        }

        dialogBinding.apply {
            btnCancel.setOnClickListener {
                dialog.dismiss()
            }

            btnSave.setOnClickListener {
                if (isValidate()) {
                    Log.i("SETTING", "Valid input")
                } else {
                    Log.i("SETTING", "Invalid input")
                }
            }

            tvTitle.setOnClickListener {
                dialog.dismiss()
            }
        }

        dialog.show()
    }


    private fun openAlertDialog(context: Context) {
        val dialog = Dialog(context, R.style.Theme_Dialog)
        val dialogBinding = LayoutSapLoginAlertBinding.inflate(LayoutInflater.from(context))
        dialog.setContentView(dialogBinding.root)

        dialog.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setGravity(Gravity.CENTER)
        }

        dialogBinding.apply {
            btnCancel.setOnClickListener {
                dialog.dismiss()
            }

            btnOk.setOnClickListener {
                dialog.dismiss()
            }

            tvTitle.setOnClickListener {
                dialog.dismiss()
            }
        }

        dialog.show()
    }


    private fun isValidate(): Boolean { // added by vinod
        dialogBinding.apply {
            val userName = etUserNameSAP.text.toString().trim()
            val userPassword = etUserPassSAP.text.toString().trim()

            var isValid = true

            // Validate Pickup Contact Person
            if (userName.isEmpty()) {
                etUserNameSAP.error = "Please enter user name."
                isValid = false
            } else {
                inputLayoutUserNameSetting.error = null
            }

            if (userPassword.isEmpty()) {
                etUserPassSAP.error = "Please enter password."
                isValid = false
            } else {
                inputLayoutUserPassSetting.error = null
            }
            return isValid
        }

    }

    //todo choose scanner type..
    @SuppressLint("MissingInflatedId")
    private fun chooseScannerPopupDialog() {
        val builder = AlertDialog.Builder(this, R.style.CustomAlertDialog).create()
        val view = LayoutInflater.from(this).inflate(R.layout.scanner_custom_alert, null)
        builder.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        builder.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        builder.window?.setGravity(Gravity.CENTER)
        builder.setView(view)

        //todo set ui text ...
        val radioGroup = view.findViewById<RadioGroup>(R.id.radio_group)
        val radioLaser = view.findViewById<RadioButton>(R.id.radioLaser)
        val radioQrScanner = view.findViewById<RadioButton>(R.id.radioQrScanner)
        val goBtn = view.findViewById<AppCompatButton>(R.id.goBtn)

        //todo get radio buttons selected id..
        var checkGender = ""

        radioGroup?.setOnCheckedChangeListener(RadioGroup.OnCheckedChangeListener { group, checkedId ->
            var radioButton = group.findViewById<RadioButton>(checkedId)
            checkGender = radioButton.text.toString()
            when (checkedId) {
                R.id.radioLaser -> {
                    radioLaser.isChecked = true
                }

                R.id.radioQrScanner -> {
                    radioQrScanner.isChecked = true
                }
            }
            /*  if (radioButton != null && checkedId != -1) {
                  Toast.makeText(this, radioButton.text, Toast.LENGTH_SHORT).show()
              } else {
                  return@OnCheckedChangeListener
              }*/
        })

        //todo validation for toggle..
        if (sessionManagement.getScannerType(this) == "LEASER") {
            radioLaser.isChecked = true
        } else if (sessionManagement.getScannerType(this) == "QR_SCANNER") {
            radioQrScanner.isChecked = true
        }

        //todo go btn..
        goBtn?.setOnClickListener {
            if (checkGender.equals("L")) {
//                sessionManagement.setLaser(1)
//                sessionManagement.setQRScanner(0)
                sessionManagement.setScannerType(this, "LEASER")
            } else if (checkGender.equals("S")) {
//                sessionManagement.setLaser(0)
//                sessionManagement.setQRScanner(1)
                sessionManagement.setScannerType(this, "QR_SCANNER")
            }
            builder.dismiss()
        }

        builder.setCancelable(true)
        builder.show()

    }


}