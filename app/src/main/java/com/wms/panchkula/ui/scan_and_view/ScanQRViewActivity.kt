package com.wms.panchkula.ui.scan_and_view

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import com.wms.panchkula.Global_Classes.GlobalMethods
import com.wms.panchkula.Global_Classes.MaterialProgressDialog
import com.wms.panchkula.Global_Notification.NetworkConnection
import com.wms.panchkula.Model.OtpErrorModel
import com.wms.panchkula.R
import com.wms.panchkula.Retrofit_Api.NetworkClients
import com.wms.panchkula.Retrofit_Api.QuantityNetworkClient
import com.wms.panchkula.SessionManagement.SessionManagement
import com.wms.panchkula.databinding.ActivityScanQrviewBinding
import com.wms.panchkula.Retrofit_Api.ApiConstantForURL
import com.wms.panchkula.ui.home.HomeActivity
import com.wms.panchkula.ui.issueForProductionOrder.UI.qrScannerUi.QRScannerActivity
import com.google.android.material.button.MaterialButton
import com.google.gson.GsonBuilder
import com.pixplicity.easyprefs.library.Prefs
import com.webapp.internetconnection.CheckNetwoorkConnection
import com.wms.panchkula.Global_Classes.AppConstants
import com.wms.panchkula.Model.ModelWarehouseLocation
import com.wms.panchkula.ui.goodsreceipt.model.IssueFromModel
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.IOException

class ScanQRViewActivity : AppCompatActivity() {

    lateinit var binding: ActivityScanQrviewBinding
    lateinit var materialProgressDialog: MaterialProgressDialog
    lateinit var checkNetwoorkConnection: CheckNetwoorkConnection
    private lateinit var sessionManagement: SessionManagement
    private lateinit var networkConnection: NetworkConnection
    private var pos: Int = 0
    private var itemCode = ""
    val REQUEST_CODE = 100
    val handler = Handler(Looper.getMainLooper())
    var type = ""
    var warehouseCode = ""
    var itemDesc = ""
    private var selectedWarehouseLocation: String? = null
    private var selectedLocationCode: String? = null

    companion object {
        private const val TAG = "ScanQRViewActivity"
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityScanQrviewBinding.inflate(layoutInflater)
        setContentView(binding.root)


        supportActionBar?.setDisplayShowHomeEnabled(true)

        materialProgressDialog = MaterialProgressDialog(this@ScanQRViewActivity)
        checkNetwoorkConnection = CheckNetwoorkConnection(application)
        sessionManagement = SessionManagement(this@ScanQRViewActivity)
        networkConnection = NetworkConnection()

        title = "Scan & View"
        getLocations()
        //todo HIDE
        Handler(Looper.getMainLooper()).postDelayed({
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
            if (imm != null && binding.edBatchCodeScan != null) {
                imm.hideSoftInputFromWindow(binding.edBatchCodeScan.windowToken, 0)
            }
        }, 200)

        binding.tvDate.setText(GlobalMethods.getCurrentDate_dd_MM_yyyy())

        //todo if leaser type choose..
        if (sessionManagement.getScannerType(this@ScanQRViewActivity) == "LEASER") {

            binding.ivScanBatchCode.visibility = View.GONE

            binding.edBatchCodeScan.requestFocus()

            //todo HIDE
            Handler(Looper.getMainLooper()).postDelayed({
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                if (imm != null && binding.edBatchCodeScan != null) {
                    imm.hideSoftInputFromWindow(binding.edBatchCodeScan.windowToken, 0)
                }
            }, 200)

            /*binding.infoCardView.setOnClickListener {
                if (IssueFromModelArraylist.size > 0) {
                    openDynamicFieldsDialog(this, 0, IssueFromModelArraylist)
                }

            }*/

            binding.infoCardView1.setOnClickListener {
                if (IssueFromModelArraylist.size > 0) {
                    val intent = Intent(this@ScanQRViewActivity, ScanViewDetailsActivity::class.java)
                    intent.putExtra("data", IssueFromModelArraylist)
                    startActivity(intent)
                }else{
                    GlobalMethods.showMessage(this@ScanQRViewActivity, "No Data Found for selected location.")
                }
            }

            binding.edBatchCodeScan.addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {
                    //todo HIDE
                    Handler(Looper.getMainLooper()).postDelayed({
                        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                        if (imm != null && binding.edBatchCodeScan != null) {
                            imm.hideSoftInputFromWindow(binding.edBatchCodeScan.windowToken, 0)
                        }
                    }, 200)

                    val scannedTextString = s.toString().trim()


                    if (scannedTextString.isNotEmpty()) {
                        try {
                            Prefs.putString(AppConstants.SCANNED_ITEM, scannedTextString)
                            val parts = scannedTextString.split(",")

                            val lastPart = parts.last()
                            type = lastPart
                            var itemCode = parts[0]
                            warehouseCode = parts[6]
                            if (type.equals("None", true)) {
                                warehouseCode = parts[8]
                            }
                            val batch = scannedTextString.split(",")[1]

                            Log.e("TAG", "afterTextChanged: " + batch + " " + itemCode + " " + warehouseCode + " " + type)
                            //todo scan call api here...
                            val location = if (!selectedLocationCode.isNullOrEmpty()) selectedLocationCode else ""
                            scanBatchLinesItem(batch, itemCode, type, warehouseCode, location.toString())

                            // Clear the EditText and request focus
                            binding.edBatchCodeScan.setText("")
                            binding.edBatchCodeScan.requestFocus()

                            Handler(Looper.getMainLooper()).postDelayed({
                                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                                imm?.hideSoftInputFromWindow(binding.edBatchCodeScan.windowToken, 0)
                            }, 200)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }

                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            })

        }

        //todo is qr scanner type choose..
        else if (sessionManagement.getScannerType(this@ScanQRViewActivity) == "QR_SCANNER" || sessionManagement.getScannerType(this@ScanQRViewActivity) == null) { //|| sessionManagement.getLeaserCheck()!! == 0 && sessionManagement.getQRScannerCheck()!! == 1 || sessionManagement.getLeaserCheck()!! == 0 && sessionManagement.getQRScannerCheck()!! == 0
            binding.ivScanBatchCode.visibility = View.VISIBLE

            //TODO click on barcode scanner for popup..
            binding.ivScanBatchCode.setOnClickListener {
                var text = binding.edBatchCodeScan.text.toString().trim()

                if (sessionManagement.getScannerType(this@ScanQRViewActivity) == null) {
                    showPopupNotChooseScanner()
                } else if (sessionManagement.getScannerType(this@ScanQRViewActivity) == "QR_SCANNER") {
                    val intent = Intent(this@ScanQRViewActivity, QRScannerActivity::class.java)
                    (this@ScanQRViewActivity as ScanQRViewActivity).startActivityForResult(intent, REQUEST_CODE)
                }

            }


        }


    }


    //todo onActivity function override for qr code scanning in adapter..
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK) {

            Log.e("Result==>", data?.getStringExtra("batch_code").toString())
            if (requestCode == REQUEST_CODE) {
                val result = data?.getStringExtra("batch_code")

                Log.e("ItemCode===>", itemCode)

                //todo spilt string and get string at 0 index...

                if (result!!.isNotEmpty()) {
                    // Split the string by "~"
                    val parts = result.toString().split(",")

                    val lastPart = parts.last()
                    type = lastPart
                    var itemCode = parts[0]
                    warehouseCode = parts[6]


                    Log.e(TAG, "onActivityResult: " + result.toString().split(",")[1] + " " + itemCode + " " + warehouseCode + " " + type)

                    //todo scan call api here...
                    scanBatchLinesItem(result.toString().split(",")[1], itemCode, type, warehouseCode, "")

                }

            }

        }
    }


    //TODO scan item lines api here....
    private fun scanBatchLinesItem(text: String, itemCode: String, type: String, warehouseCode: String, location: String) {
        if (networkConnection.getConnectivityStatusBoolean(this)) {
            materialProgressDialog.show()
            var apiConfig = ApiConstantForURL()

            NetworkClients.updateBaseUrlFromConfig(apiConfig)

            QuantityNetworkClient.updateBaseUrlFromConfig(apiConfig)
            val bplId = if (Prefs.getString(AppConstants.BPLID, "").isNotEmpty()) Prefs.getString(AppConstants.BPLID, "") else ""
            val networkClient = QuantityNetworkClient.create(this@ScanQRViewActivity)
            networkClient.doScanAndView(sessionManagement.getCompanyDB(applicationContext)!!, text, itemCode, warehouseCode, type, bplId, location)
                .apply {
                    enqueue(object : Callback<ScanViewModel> {
                        override fun onResponse(
                            call: Call<ScanViewModel>,
                            response: Response<ScanViewModel>
                        ) {
                            try {
                                materialProgressDialog.dismiss()
                                if (response.isSuccessful) {
                                    if (response.code() == 200) {
                                        Log.e("response---------", response.body().toString())

                                        var responseModel = response.body()!!



                                        binding.infoCardView1.visibility = View.VISIBLE

                                        //todo bind data--
                                        if (!responseModel.ItemCode.isNullOrEmpty()) {

                                            IssueFromModelArraylist.clear()
                                            if (responseModel.batchdet != null) {
                                                IssueFromModelArraylist.addAll(responseModel.batchdet)
                                            }


                                            binding.tvItemCode.setText(responseModel.ItemCode)
                                        } else {
                                            binding.tvItemCode.setText("NA")
                                        }

                                        binding.tvItemName.text = responseModel.ItemName.ifEmpty { "NA" }
                                        binding.tvItemType.text = responseModel.Batch_Lot_Serial.ifEmpty { "NA" }
                                        binding.tvUom.text = responseModel.UOM.ifEmpty { "NA" }
                                        binding.tvLeadTime.text = responseModel.LeadTime.ifEmpty { "0.00" }
                                        binding.tvMin.text = responseModel.Min.toDoubleOrNull()?.let { String.format("%.2f", it) } ?: "0.00"
                                        binding.tvMax.text = responseModel.Max.toDoubleOrNull()?.let { String.format("%.2f", it) } ?: "0.00"
                                        binding.tvInStock.text = responseModel.InStock.toDoubleOrNull()?.let { String.format("%.2f", it) } ?: "0.00"

                                        binding.tvOrdered.text = responseModel.Ordered.toDoubleOrNull()?.let { String.format("%.2f", it) } ?: "0.00"
                                        binding.tvAvailableStock.text = responseModel.AvailableStock.toDoubleOrNull()?.let { String.format("%.2f", it) } ?: "0.00"
                                        binding.tvCommited.text = responseModel.Commited.toDoubleOrNull()?.let { String.format("%.2f", it) } ?: "0.00"






                                        /*if (!responseModel.ItemName.isNullOrEmpty()) {
                                            binding.tvItemName.setText(responseModel.ItemName)
                                        } else {
                                            binding.tvItemName.setText("NA")
                                        }*/

                                        /*if (!responseModel.AvailableStock.isNullOrEmpty()) {
                                            binding.tvAvailaleStock.setText(GlobalMethods.numberToK(responseModel.AvailableStock))
                                        } else {
                                            binding.tvAvailaleStock.setText("NA")
                                        }
                                        *//*if (!responseModel.Batch_Lot_Serial.isNullOrEmpty()) {
                                            binding.tvBatchLot.setText(responseModel.Batch_Lot_Serial)
                                        } else {
                                            binding.tvBatchLot.setText("NA")
                                        }
                                        if (!responseModel.UOM.isNullOrEmpty()) {
                                            binding.tvUOM.setText(responseModel.UOM)
                                        } else {
                                            binding.tvUOM.setText("NA")
                                        }*//*
                                        if (!responseModel.LeadTime.isNullOrEmpty()) {
                                            binding.tvLeadTime.setText(responseModel.LeadTime)
                                        } else {
                                            binding.tvLeadTime.setText("NA")
                                        }
                                        if (!responseModel.Min.isNullOrEmpty()) {
                                            binding.tvMin.setText(GlobalMethods.numberToK(responseModel.Min))
                                        } else {
                                            binding.tvMin.setText("NA")
                                        }
                                        if (!responseModel.Max.isNullOrEmpty()) {
                                            binding.tvMax.setText(GlobalMethods.numberToK(responseModel.Max))
                                        } else {
                                            binding.tvMax.setText("NA")
                                        }
                                        if (!responseModel.InStock.isNullOrEmpty()) {
                                            binding.tvInStock.setText(GlobalMethods.numberToK(responseModel.InStock))
                                        } else {
                                            binding.tvInStock.setText("NA")
                                        }
                                        if (!responseModel.Commited.isNullOrEmpty()) {
                                            binding.tvCommited.setText(responseModel.Commited)
                                        } else {
                                            binding.tvCommited.setText("NA")
                                        }
                                        if (!responseModel.Ordered.isNullOrEmpty()) {
                                            binding.tvOrdered.setText(GlobalMethods.numberToK(responseModel.Ordered))
                                        } else {
                                            binding.tvOrdered.setText("NA")
                                        }
                                        *//*if (!responseModel.WhsCode.isNullOrEmpty()) {
                                            binding.tvWarehouseCode.setText(responseModel.WhsCode)
                                        } else {
                                            binding.tvWarehouseCode.setText("NA")
                                        }*//*

                                        if (!responseModel.InDate.isNullOrEmpty()) {
                                            binding.tvInDate.setText(GlobalMethods.separateDateAndTime(responseModel.InDate))
                                        } else {
                                            binding.tvInDate.setText("NA")
                                        }
                                        if (!responseModel.MfgDate.isNullOrEmpty()) {
                                            binding.tvMfgDate.setText(GlobalMethods.separateDateAndTime(responseModel.MfgDate))
                                        } else {
                                            binding.tvMfgDate.setText("NA")
                                        }
                                        if (!responseModel.ExpiryDate.isNullOrEmpty()) {
                                            binding.tvExpDate.setText(GlobalMethods.separateDateAndTime(responseModel.ExpiryDate))
                                        } else {
                                            binding.tvExpDate.setText("NA")
                                        }
                                        if (!responseModel.Qty.isNullOrEmpty()) {
                                            binding.tvQty.setText(GlobalMethods.numberToK(responseModel.Qty))
                                        } else {
                                            binding.tvQty.setText("NA")
                                        }*/


                                    } else {
                                        binding.infoCardView1.visibility = View.GONE
                                        GlobalMethods.showError(this@ScanQRViewActivity, "Invalid Batch Code")
                                        Log.e("not_response---------", response.message())
                                    }

                                } else {
                                    //binding.infoCardView.visibility = View.GONE

                                    materialProgressDialog.dismiss()
                                    val gson1 = GsonBuilder().create()
                                    var mError: OtpErrorModel
                                    try {
                                        val s = response.errorBody()!!.string()
                                        mError = gson1.fromJson(s, OtpErrorModel::class.java)
                                        if (mError.error.code.equals(400)) {
                                            GlobalMethods.showError(this@ScanQRViewActivity, mError.error.message.value)
                                        }
                                        if (mError.error.message.value != null) {
                                            GlobalMethods.showError(this@ScanQRViewActivity, mError.error.message.value)
                                            Log.e("json_error------", mError.error.message.value)
                                        }
                                    } catch (e: IOException) {
                                        e.printStackTrace()
                                    }
                                }

                            } catch (e: Exception) {
                                materialProgressDialog.dismiss()
                                e.printStackTrace()
                            }
                        }

                        override fun onFailure(call: Call<ScanViewModel>, t: Throwable) {
                            Log.e("scanItemApiFailed-----", t.toString())
                            materialProgressDialog.dismiss()
                        }

                    })
                }
        } else {
            materialProgressDialog.dismiss()
            Toast.makeText(this@ScanQRViewActivity, "No Network Connection", Toast.LENGTH_SHORT).show()
        }
    }


    private fun getLocations() {
        if (networkConnection.getConnectivityStatusBoolean(this)) {
            materialProgressDialog.show()
            var apiConfig = ApiConstantForURL()

            NetworkClients.updateBaseUrlFromConfig(apiConfig)

            QuantityNetworkClient.updateBaseUrlFromConfig(apiConfig)
            val bplId = if (Prefs.getString(AppConstants.BPLID, "").isNotEmpty()) Prefs.getString(AppConstants.BPLID, "") else ""
            val networkClient = QuantityNetworkClient.create(this@ScanQRViewActivity)
            networkClient.getLocationForScanView(bplId)
                .apply {
                    enqueue(object : Callback<ModelWarehouseLocation> {
                        override fun onResponse(
                            call: Call<ModelWarehouseLocation>,
                            response: Response<ModelWarehouseLocation>
                        ) {
                            try {
                                materialProgressDialog.dismiss()
                                if (response.isSuccessful) {
                                    if (response.code() == 200) {
                                        Log.e("response---------", response.body().toString())

                                        var responseModel = response.body()!!
                                        setLocationSpinner(responseModel.value)

                                    } else {
                                        GlobalMethods.showError(this@ScanQRViewActivity, "Invalid Batch Code")
                                        Log.e("not_response---------", response.message())
                                    }

                                } else {
                                    materialProgressDialog.dismiss()
                                    val gson1 = GsonBuilder().create()
                                    var mError: OtpErrorModel
                                    try {
                                        val s = response.errorBody()!!.string()
                                        mError = gson1.fromJson(s, OtpErrorModel::class.java)
                                        if (mError.error.code.equals(400)) {
                                            GlobalMethods.showError(this@ScanQRViewActivity, mError.error.message.value)
                                        }
                                        if (mError.error.message.value != null) {
                                            GlobalMethods.showError(this@ScanQRViewActivity, mError.error.message.value)
                                            Log.e("json_error------", mError.error.message.value)
                                        }
                                    } catch (e: IOException) {
                                        e.printStackTrace()
                                    }
                                }

                            } catch (e: Exception) {
                                materialProgressDialog.dismiss()
                                e.printStackTrace()
                            }
                        }

                        override fun onFailure(call: Call<ModelWarehouseLocation>, t: Throwable) {
                            Log.e("scanItemApiFailed-----", t.toString())
                            materialProgressDialog.dismiss()
                        }

                    })
                }
        } else {
            materialProgressDialog.dismiss()
            Toast.makeText(this@ScanQRViewActivity, "No Network Connection", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setLocationSpinner(locations: ArrayList<ModelWarehouseLocation.Value>) {
        // Extract display names from the location objects for the dropdown
        val locationNames = locations.map { it.Name }  // Replace `it.name` with the actual display field

        // Create and set adapter
        val adapter = ArrayAdapter(this, R.layout.drop_down_item_textview, locationNames)
        binding.acWarehouseLocation.setAdapter(adapter)

        // Handle selection
        binding.acWarehouseLocation.setOnItemClickListener { _, _, position, _ ->
            selectedWarehouseLocation = locations[position].Name
            selectedLocationCode = locations[position].Code
            Log.e("TAG", "Scanned Item by Prefs:  ${Prefs.getString(AppConstants.SCANNED_ITEM)}")
            if (Prefs.getString(AppConstants.SCANNED_ITEM).isNotEmpty()) {
                val parts = Prefs.getString(AppConstants.SCANNED_ITEM).split(",")

                val lastPart = parts.last()
                type = lastPart
                var itemCode = parts[0]
                warehouseCode = parts[6]
                if (type.equals("None", true)) {
                    warehouseCode = parts[8]
                }
                val batch =parts[1]
                Log.e("TAG", "acWarehouseLocation.setOnItemClickListener: " + batch + " " + itemCode + " " + warehouseCode + " " + type)
                scanBatchLinesItem(batch, itemCode, type, warehouseCode, selectedLocationCode.toString())

            } else
                Log.e("TAG", "setOnItemClickListener => scannedTextString: null")
            binding.acWarehouseLocation.setText(locationNames[position], false)
        }
    }


    private fun showPopupNotChooseScanner() {
        val builder = AlertDialog.Builder(this@ScanQRViewActivity, R.style.CustomAlertDialog).create()
        val view = LayoutInflater.from(this@ScanQRViewActivity).inflate(R.layout.custom_popup_alert, null)
        builder.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        builder.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        builder.window?.setGravity(Gravity.CENTER)
        builder.setView(view)

        //todo set ui..
        val cancelBtn = view.findViewById<MaterialButton>(R.id.cancel_btn)
        val yesBtn = view.findViewById<MaterialButton>(R.id.ok_btn)

        cancelBtn.setOnClickListener {
            builder.dismiss()
        }

        yesBtn.setOnClickListener {
            var intent = Intent(this@ScanQRViewActivity, HomeActivity::class.java)
            startActivity(intent)
            builder.dismiss()

        }

        builder.setCancelable(true)
        builder.show()
    }

    var IssueFromModelArraylist = ArrayList<IssueFromModel.Value>()

    override fun onDestroy() {
        super.onDestroy()
        Prefs.putString(AppConstants.SCANNED_ITEM, "")
    }

}