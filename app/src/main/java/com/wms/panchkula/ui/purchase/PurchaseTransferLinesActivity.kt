package com.wms.panchkula.ui.purchase

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.*
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.wms.panchkula.Retrofit_Api.ApiConstantForURL
import com.wms.panchkula.Retrofit_Api.NetworkClients
import com.wms.panchkula.Retrofit_Api.QuantityNetworkClient
import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.pixplicity.easyprefs.library.Prefs
import com.wms.panchkula.Global_Classes.AppConstants
import com.wms.panchkula.Global_Classes.FreightBottomDialogFragment
import com.wms.panchkula.Global_Classes.GlobalMethods
import com.wms.panchkula.Global_Classes.GlobalMethods.getCurrentDate_dd_MM_yyyy
import com.wms.panchkula.Global_Classes.GlobalMethods.showSuccessDialog
import com.wms.panchkula.Global_Classes.GlobalMethods.toEditable
import com.wms.panchkula.Global_Classes.GlobalMethods.toSimpleJson
import com.wms.panchkula.Global_Classes.MaterialProgressDialog
import com.wms.panchkula.Global_Notification.NetworkConnection
import com.wms.panchkula.Model.GetWarehouseModel
import com.wms.panchkula.Model.ModelSeries
import com.wms.panchkula.Model.OtpErrorModel
import com.wms.panchkula.SessionManagement.SessionManagement
import com.wms.panchkula.databinding.ActivityPurchaseTransferLinesBinding
import com.wms.panchkula.interfaces.PassList
import com.wms.panchkula.ui.invoiceOrder.UI.TAG
import com.wms.panchkula.ui.issueForProductionOrder.Model.ScanedOrderBatchedItems
import com.wms.panchkula.ui.issueForProductionOrder.Model.WarehouseBPL_IDModel
import com.wms.panchkula.ui.purchase.model.FreightDataModel
import com.wms.panchkula.ui.purchase.model.FreightTypeModel
import com.wms.panchkula.ui.purchase.model.PurchasePostResponse
import com.wms.panchkula.ui.purchase.model.PurchaseRequestModel
import com.wms.panchkula.ui.purchase.model.TaxListModel
import org.json.JSONArray
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.io.IOException
import java.lang.reflect.InvocationTargetException
import java.net.SocketTimeoutException
import java.sql.Connection
import java.sql.SQLException
import java.sql.Statement
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter


class PurchaseTransferLinesActivity : AppCompatActivity(), PassList,
    PurchaseTransferItemAdapter.AdapterCallback {
    companion object {
        // Declare the static variable
        var staticString: String = ""
    }

    var inventoryItem: PurchaseRequestModel.Value? = null
    private var selectedSeriesName = ""
    private var selectedSeries = ""

    private var selectedWarehosueName = ""
    private var selectedWarehosueCode = ""

    private lateinit var activityFormBinding: ActivityPurchaseTransferLinesBinding

    //    private lateinit var PurchaseTransferItemAdapter: DemoAdapter
    private lateinit var PurchaseTransferItemAdapter: PurchaseTransferItemAdapter
    private var productionOrderLineList_gl: ArrayList<PurchaseRequestModel.StockTransferLines> = arrayListOf()
    private lateinit var productionOrderList_gl: ArrayList<PurchaseRequestModel.Value>

    private lateinit var taxList: ArrayList<TaxListModel.Value>
    private lateinit var whList: ArrayList<GetWarehouseModel.Value>
    private lateinit var freightType: ArrayList<FreightTypeModel.Value>

    var position: Int? = 0
    lateinit var networkConnection: NetworkConnection
    lateinit var materialProgressDialog: MaterialProgressDialog
    private lateinit var sessionManagement: SessionManagement
    private var BPLIDNum = 0
    private var valueList: List<PurchaseRequestModel.StockTransferLines> = ArrayList()
    private var connection: Connection? = null
    var openQty = 0.0

    //todo batch scan and quantity list interface override...
    var hashMapBatchList: HashMap<String, ArrayList<ScanedOrderBatchedItems.Value>> = HashMap<String, ArrayList<ScanedOrderBatchedItems.Value>>()
    var hashmapBatchQuantityList: HashMap<String, ArrayList<String>> = HashMap<String, ArrayList<String>>()
    var serialHashMapQuantityList: java.util.HashMap<String, ArrayList<String>> = java.util.HashMap<String, ArrayList<String>>()
    var noneHashMapQuantityList: java.util.HashMap<String, ArrayList<String>> = java.util.HashMap<String, ArrayList<String>>()


    @RequiresApi(33)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activityFormBinding = ActivityPurchaseTransferLinesBinding.inflate(layoutInflater)
        setContentView(activityFormBinding.root)
        activityFormBinding.chipSave.setText("Next")
        title = "Form Screen"

        deleteCache(this)

        supportActionBar?.hide()

        activityFormBinding.ivLaserCode.setFocusable(true)
        activityFormBinding.ivLaserCode.requestFocus()
        activityFormBinding.etInvoiceDate.text = getCurrentDate_dd_MM_yyyy().toEditable()
        activityFormBinding.etPostingDate.text = getCurrentDate_dd_MM_yyyy().toEditable()

        Handler(Looper.getMainLooper()).postDelayed({
            val imm = getSystemService(INPUT_METHOD_SERVICE) as? InputMethodManager
            if (imm != null && currentFocus != null) {
                imm.hideSoftInputFromWindow(currentFocus!!.windowToken, 0)
            }
        }, 200)


        //todo initialization...
        networkConnection = NetworkConnection()
        materialProgressDialog = MaterialProgressDialog(this@PurchaseTransferLinesActivity)
        sessionManagement = SessionManagement(this@PurchaseTransferLinesActivity)
        //callWarehouseListApi()
        //todo get arguments data...

        try {
            val intent = intent
            inventoryItem = intent.getSerializableExtra("inventReqModel") as PurchaseRequestModel.Value
            productionOrderList_gl = intent.getSerializableExtra("productionOrderList") as? ArrayList<PurchaseRequestModel.Value> ?: arrayListOf() // fallback to empty list
            activityFormBinding.currencyView.visibility = if (inventoryItem?.GroupCode.equals("104")) View.VISIBLE else View.GONE
            activityFormBinding.tvCurrencyLabel.text = "Currency ( ${inventoryItem?.CurrencyBP} )"
            for (item in productionOrderList_gl) {
                productionOrderLineList_gl.addAll(item.DocumentLines)
            }

            for (item in productionOrderLineList_gl) {
                Log.e("WAREHOUSE_NAME", "Warehouse Code : ${item.WarehouseCode}")
            }


            position = intent.extras?.getInt("pos")
            activityFormBinding.tvTitle.text = "Purchase Order : " + inventoryItem!!.DocNum
            //Log.e(TAG, "onCreate:===> " + productionOrderLineList_gl.size)
            callTaxListApi()
            callFreightTypeApi()
            getDocSeries()
            //setAdapter()

        } catch (e: IOException) {
            Log.e(TAG, "onCreate:===> " + e.message)
            Toast.makeText(this, e.message, Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }


        activityFormBinding.ivOnback.setOnClickListener {
            onBackPressed()
        }


        //todo cancel lines...
        activityFormBinding.chipCancel.setOnClickListener {
            onBackPressed()
        }

        activityFormBinding.etInvoiceDate.setOnClickListener {
            GlobalMethods.datePicker(this@PurchaseTransferLinesActivity, activityFormBinding.etInvoiceDate)
        }

        activityFormBinding.etPostingDate.setOnClickListener {
            GlobalMethods.datePicker(this@PurchaseTransferLinesActivity, activityFormBinding.etPostingDate)
        }

    }


    //todo set adapter....
    fun setAdapter(taxList: ArrayList<TaxListModel.Value>) {

        activityFormBinding.ivNoDataFound.visibility = View.GONE
        activityFormBinding.rvProductionOrderList.visibility = View.VISIBLE
        activityFormBinding.btnLinearLayout.visibility = View.VISIBLE
        //  productionOrderLineList_gl = setFilteredList(productionOrderLineList_gl) as ArrayList<PurchaseRequestModel.StockTransferLines> /* = java.util.ArrayList<com.soothe.sapApplication.ui.issueForProductionOrder.Model.PurchaseRequestModel.StockTransferLines> */

        val layoutManager: RecyclerView.LayoutManager = LinearLayoutManager(this)
        activityFormBinding.rvProductionOrderList.layoutManager = layoutManager
        //todo parse save button in adapter constructor for click listener on adapter...
        if (taxList != null) {
            PurchaseTransferItemAdapter = PurchaseTransferItemAdapter(
                this@PurchaseTransferLinesActivity,
                productionOrderLineList_gl,
                networkConnection,
                materialProgressDialog,
                this@PurchaseTransferLinesActivity,
                activityFormBinding.chipSave,
                taxList
            )
        }
        activityFormBinding.rvProductionOrderList.adapter = PurchaseTransferItemAdapter


    }

    @SuppressLint("SetTextI18n")
    private fun setWarehouseAdapter(value: ArrayList<GetWarehouseModel.Value>) {
        // 1. Create display list with "WHCode - WHName"
        val whList = value.map { "${it.WareHouseCode} - ${it.WareHouseName}" }

        val adapter = ArrayAdapter(
            this@PurchaseTransferLinesActivity,
            android.R.layout.simple_spinner_dropdown_item,
            whList
        )

        activityFormBinding.acWarehouse.setAdapter(adapter)

        // 2. Set default warehouse if matched
        val defaultWarehouseCode = productionOrderLineList_gl.getOrNull(0)?.WarehouseCode
        Log.e("WAREHOUSE_NAME", "Default Warehouse Code : $defaultWarehouseCode")

        // 3. Find index of matching warehouse code
        val defaultIndex = value.indexOfFirst { it.WareHouseCode.equals(defaultWarehouseCode, ignoreCase = true) }

        if (defaultIndex != -1) {
            val defaultDisplayName = "${value[defaultIndex].WareHouseCode} - ${value[defaultIndex].WareHouseName}"
            activityFormBinding.acWarehouse.setText(defaultDisplayName, false)

            // Set selected values
            selectedWarehosueCode = value[defaultIndex].WareHouseCode.toString()
            selectedWarehosueName = value[defaultIndex].WareHouseName.toString()
        }

        // 4. Handle selection change
        activityFormBinding.acWarehouse.setOnItemClickListener { _, _, position, _ ->
            selectedWarehosueName = value[position].WareHouseName.toString()
            selectedWarehosueCode = value[position].WareHouseCode.toString()

            activityFormBinding.acWarehouse.setText("$selectedWarehosueCode - $selectedWarehosueName", false)

            // Update all line items with selected warehouse code
            //productionOrderLineList_gl.forEach { it.WarehouseCode = selectedWarehosueCode }

            Log.w("WAREHOUSE_NAME", "Warehouse: $selectedWarehosueName, Code: $selectedWarehosueCode")
        }
    }


    private fun getDocSeries() {
        val apiConfig = ApiConstantForURL()
        QuantityNetworkClient.updateBaseUrlFromConfig(apiConfig)
        val networkClient = QuantityNetworkClient.create(this)
        val bplId = if (Prefs.getString(AppConstants.BPLID, "").isNotEmpty()) Prefs.getString(AppConstants.BPLID, "") else ""
        networkClient.getDocSeries(bplId, "20").apply {
            enqueue(object : Callback<ModelSeries> {
                override fun onResponse(call: Call<ModelSeries>, response: Response<ModelSeries>) {
                    try {
                        if (response.isSuccessful) {
                            materialProgressDialog.dismiss()
                            var responseModel = response.body()!!
                            setSeriesSinner(responseModel.value)
                        } else {
                            materialProgressDialog.dismiss()

                            Prefs.clear()

                            val gson1 = GsonBuilder().create()
                            var mError: OtpErrorModel
                            try {
                                val s = response.errorBody()!!.string()
                                mError = gson1.fromJson(s, OtpErrorModel::class.java)
                                if (mError.error.code.equals(400)) {
                                    GlobalMethods.showError(this@PurchaseTransferLinesActivity, mError.error.message.value)
                                }
                                if (mError.error.message.value != null) {
                                    GlobalMethods.showError(this@PurchaseTransferLinesActivity, mError.error.message.value)
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

                override fun onFailure(call: Call<ModelSeries>, t: Throwable) {
                    Log.e("login_api_failure-----", t.toString())
                    materialProgressDialog.dismiss()
                    when (t) {
                        is SocketTimeoutException -> {
                            GlobalMethods.showError(this@PurchaseTransferLinesActivity, "Connection timed out. Please try again.")
                        }

                        is IOException -> {
                            GlobalMethods.showError(this@PurchaseTransferLinesActivity, "Network error. Please check your internet connection.")
                        }

                        else -> {
                            GlobalMethods.showError(this@PurchaseTransferLinesActivity, "Something went wrong: ${t.localizedMessage}")
                        }
                    }
//                                Prefs.clear()
                    Log.e("DBURL==>", "onFailure: " + Prefs.getString(AppConstants.DBUrl, ""))

                    //Toast.makeText(this@PurchaseTransferLinesActivity, t.message, Toast.LENGTH_SHORT)
                }

            })
        }
    }

    private fun callTaxListApi() {
        val apiConfig = ApiConstantForURL()
        QuantityNetworkClient.updateBaseUrlFromConfig(apiConfig)
        val networkClient = QuantityNetworkClient.create(this)
        networkClient.getTaxList().apply {
            enqueue(object : Callback<TaxListModel> {
                override fun onResponse(call: Call<TaxListModel>, response: Response<TaxListModel>) {
                    try {
                        if (response.isSuccessful) {
                            materialProgressDialog.dismiss()
                            var responseModel = response.body()!!
                            taxList = responseModel.value as ArrayList<TaxListModel.Value>
                            setAdapter(taxList)
                        } else {
                            materialProgressDialog.dismiss()

                            Prefs.clear()

                            val gson1 = GsonBuilder().create()
                            var mError: OtpErrorModel
                            try {
                                val s = response.errorBody()!!.string()
                                mError = gson1.fromJson(s, OtpErrorModel::class.java)
                                if (mError.error.code.equals(400)) {
                                    GlobalMethods.showError(this@PurchaseTransferLinesActivity, mError.error.message.value)
                                }
                                if (mError.error.message.value != null) {
                                    GlobalMethods.showError(this@PurchaseTransferLinesActivity, mError.error.message.value)
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

                override fun onFailure(call: Call<TaxListModel>, t: Throwable) {
                    Log.e("login_api_failure-----", t.toString())
                    materialProgressDialog.dismiss()
                    when (t) {
                        is SocketTimeoutException -> {
                            GlobalMethods.showError(this@PurchaseTransferLinesActivity, "Connection timed out. Please try again.")
                        }

                        is IOException -> {
                            GlobalMethods.showError(this@PurchaseTransferLinesActivity, "Network error. Please check your internet connection.")
                        }

                        else -> {
                            GlobalMethods.showError(this@PurchaseTransferLinesActivity, "Something went wrong: ${t.localizedMessage}")
                        }
                    }
//                                Prefs.clear()
                    Log.e("DBURL==>", "onFailure: " + Prefs.getString(AppConstants.DBUrl, ""))

                    //Toast.makeText(this@PurchaseTransferLinesActivity, t.message, Toast.LENGTH_SHORT)
                }

            })
        }
    }

    private fun callWarehouseListApi() {
        val apiConfig = ApiConstantForURL()
        QuantityNetworkClient.updateBaseUrlFromConfig(apiConfig)
        val networkClient = QuantityNetworkClient.create(this)
        networkClient.getWarehouse().apply {
            enqueue(object : Callback<GetWarehouseModel> {
                override fun onResponse(call: Call<GetWarehouseModel>, response: Response<GetWarehouseModel>) {
                    try {
                        if (response.isSuccessful) {
                            materialProgressDialog.dismiss()
                            var responseModel = response.body()!!
                            whList = responseModel.value as ArrayList<GetWarehouseModel.Value>
                            setWarehouseAdapter(whList)
                        } else {
                            materialProgressDialog.dismiss()

                            Prefs.clear()

                            val gson1 = GsonBuilder().create()
                            var mError: OtpErrorModel
                            try {
                                val s = response.errorBody()!!.string()
                                mError = gson1.fromJson(s, OtpErrorModel::class.java)
                                if (mError.error.code.equals(400)) {
                                    GlobalMethods.showError(this@PurchaseTransferLinesActivity, mError.error.message.value)
                                }
                                if (mError.error.message.value != null) {
                                    GlobalMethods.showError(this@PurchaseTransferLinesActivity, mError.error.message.value)
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

                override fun onFailure(call: Call<GetWarehouseModel>, t: Throwable) {
                    Log.e("login_api_failure-----", t.toString())
                    materialProgressDialog.dismiss()
                    when (t) {
                        is SocketTimeoutException -> {
                            GlobalMethods.showError(this@PurchaseTransferLinesActivity, "Connection timed out. Please try again.")
                        }

                        is IOException -> {
                            GlobalMethods.showError(this@PurchaseTransferLinesActivity, "Network error. Please check your internet connection.")
                        }

                        else -> {
                            GlobalMethods.showError(this@PurchaseTransferLinesActivity, "Something went wrong: ${t.localizedMessage}")
                        }
                    }
//                                Prefs.clear()
                    Log.e("DBURL==>", "onFailure: " + Prefs.getString(AppConstants.DBUrl, ""))

                    //Toast.makeText(this@PurchaseTransferLinesActivity, t.message, Toast.LENGTH_SHORT)
                }

            })
        }
    }

    private fun callFreightTypeApi() {
        val apiConfig = ApiConstantForURL()
        QuantityNetworkClient.updateBaseUrlFromConfig(apiConfig)
        val networkClient = QuantityNetworkClient.create(this)
        networkClient.getFreightType().apply {
            enqueue(object : Callback<FreightTypeModel> {
                override fun onResponse(call: Call<FreightTypeModel>, response: Response<FreightTypeModel>) {
                    try {
                        if (response.isSuccessful) {
                            materialProgressDialog.dismiss()
                            var responseModel = response.body()!!
                            freightType = responseModel.value as ArrayList<FreightTypeModel.Value>
                        } else {
                            materialProgressDialog.dismiss()

                            Prefs.clear()

                            val gson1 = GsonBuilder().create()
                            var mError: OtpErrorModel
                            try {
                                val s = response.errorBody()!!.string()
                                mError = gson1.fromJson(s, OtpErrorModel::class.java)
                                if (mError.error.code.equals(400)) {
                                    GlobalMethods.showError(this@PurchaseTransferLinesActivity, mError.error.message.value)
                                }
                                if (mError.error.message.value != null) {
                                    GlobalMethods.showError(this@PurchaseTransferLinesActivity, mError.error.message.value)
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

                override fun onFailure(call: Call<FreightTypeModel>, t: Throwable) {
                    Log.e("login_api_failure-----", t.toString())
                    materialProgressDialog.dismiss()
                    when (t) {
                        is SocketTimeoutException -> {
                            GlobalMethods.showError(this@PurchaseTransferLinesActivity, "Connection timed out. Please try again.")
                        }

                        is IOException -> {
                            GlobalMethods.showError(this@PurchaseTransferLinesActivity, "Network error. Please check your internet connection.")
                        }

                        else -> {
                            GlobalMethods.showError(this@PurchaseTransferLinesActivity, "Something went wrong: ${t.localizedMessage}")
                        }
                    }
//                                Prefs.clear()
                    Log.e("DBURL==>", "onFailure: " + Prefs.getString(AppConstants.DBUrl, ""))

                    //Toast.makeText(this@PurchaseTransferLinesActivity, t.message, Toast.LENGTH_SHORT)
                }

            })
        }
    }

    private fun setSeriesSinner(value: List<ModelSeries.Value>) {
        val seriesList = value.map { it.SeriesName.toString() }
        val adapter = ArrayAdapter(
            this@PurchaseTransferLinesActivity,
            android.R.layout.simple_spinner_dropdown_item,
            seriesList
        )

        activityFormBinding.acSeries.setAdapter(adapter)
        //binding.acScanType.hint = "Select DB"

        // Handle item selection
        activityFormBinding.acSeries.setOnItemClickListener { _, _, position, _ ->
            selectedSeriesName = value[position].SeriesName
            selectedSeries = value[position].Series

            // Set the text of the AutoCompleteTextView with the selected item
            activityFormBinding.acSeries.setText(selectedSeriesName, false) //

        }
    }

    var productionOrderLineList_temp: MutableList<PurchaseRequestModel.StockTransferLines> = mutableListOf()

    private fun setFilteredList(arrayList: java.util.ArrayList<PurchaseRequestModel.StockTransferLines>): MutableList<PurchaseRequestModel.StockTransferLines> {

        var position = -1
        for ((index, item) in arrayList.withIndex()) {
            if (item is PurchaseRequestModel.StockTransferLines && item.RemainingOpenQuantity.toDouble() > 0 && item.RemainingOpenQuantity != "0.0") {
                productionOrderLineList_temp.add(item)
            }

        }
        return productionOrderLineList_temp

    }

    //todo override function for save items posting
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onApiResponseStock(
        response: HashMap<String, ArrayList<ScanedOrderBatchedItems.Value>>, listResponse: ArrayList<PurchaseRequestModel.StockTransferLines>,
        quantityResponse: HashMap<String, ArrayList<String>>, serialQuantityResponse: java.util.HashMap<String, ArrayList<String>>, noneQuantityResponse: java.util.HashMap<String, ArrayList<String>>
    ) {
        Log.e("hashmap--->", quantityResponse.toString())
        Log.e("hashmap--->bp123", listResponse.toString())

        //activityFormBinding.chipSave.isEnabled = false
        //activityFormBinding.chipSave.isCheckable = false

        hashMapBatchList = response
        hashmapBatchQuantityList = quantityResponse
        serialHashMapQuantityList = serialQuantityResponse
        noneHashMapQuantityList = noneQuantityResponse


        postInventorystock(inventoryItem!!, listResponse)

    }


    lateinit var batchList: List<ScanedOrderBatchedItems.Value>
    lateinit var batchQuantityList: MutableList<String>


    //todo here save issue for production lines items of order...
    @SuppressLint("SuspiciousIndentation")
    @RequiresApi(Build.VERSION_CODES.O)
    private fun postInventorystock(inventoryItem: PurchaseRequestModel.Value, list: MutableList<PurchaseRequestModel.StockTransferLines>) {
        var ii = 0
        val currentDate = LocalDateTime.now()
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")
        val formattedDate = currentDate.format(formatter)
        val docCurrency = activityFormBinding.etCurrency.text.toString()
        var postedJson = JsonObject()
        if (activityFormBinding.etVendorRefNo.text.toString().isNullOrEmpty()) {
            GlobalMethods.showError(this@PurchaseTransferLinesActivity, "Please enter vendor ref. no.")
            activityFormBinding.etVendorRefNo.requestFocus()
            return
        } else if (selectedSeries.isNullOrEmpty()) {
            GlobalMethods.showError(this@PurchaseTransferLinesActivity, "Please select document series.")
            return
        } else if (activityFormBinding.etPostingDate.text.toString().isNullOrEmpty()) {
            GlobalMethods.showError(this@PurchaseTransferLinesActivity, "Please select doc date.")
            return
        } else if (docCurrency.isNullOrEmpty()) {
            if (inventoryItem.GroupCode.equals("104")) {
                GlobalMethods.showError(this@PurchaseTransferLinesActivity, "Please select currency.")
                return
            }
        }

        val docDate = GlobalMethods.convert_dd_MM_yyyy_into_yyyy_MM_dd(activityFormBinding.etPostingDate.text.toString())

        val invoiceDate = GlobalMethods.convert_dd_MM_yyyy_into_yyyy_MM_dd(activityFormBinding.etInvoiceDate.text.toString())


        postedJson.addProperty("Series", selectedSeries)//552
        postedJson.addProperty("DocDate", docDate)
        postedJson.addProperty("DocCurrency", docCurrency)
        postedJson.addProperty("DocDueDate", inventoryItem.DocDueDate)
        postedJson.addProperty("TaxDate", invoiceDate)
        postedJson.addProperty("CardCode", inventoryItem.CardCode)
        postedJson.addProperty("AttachmentEntry", inventoryItem.AttachmentEntry)
        postedJson.addProperty("Rounding", "N")
        postedJson.addProperty("RoundingDiffAmount", 0.00)
        postedJson.addProperty("BPL_IDAssignedToInvoice", inventoryItem.BPLID)
        postedJson.addProperty("NumAtCard", activityFormBinding.etVendorRefNo.text.toString())  //inventoryItem.NumAtCard
        postedJson.addProperty("Comments", activityFormBinding.etRemarks.text.toString())  // added by Vinod @28Apr,2025
        postedJson.addProperty("U_Type", inventoryItem.DocType)  //inventoryItem.NumAtCard
        postedJson.addProperty("U_WMSPOST", "Y") //U_WMSPOST tagged (added by Vinod @13Aug,2025)
        postedJson.addProperty("U_WMSUSER", sessionManagement.getUsername(this@PurchaseTransferLinesActivity))  //WMS userName tagged (added by Vinod @25Apr,2025)


        if (networkConnection.getConnectivityStatusBoolean(applicationContext)) {

            var StockTransferLines = JsonArray()


            for (i in list.indices) {

                var quantity = 0.000
                quantity = GlobalMethods.sumBatchQuantity(
                    i,
                    hashmapBatchQuantityList.get("Item" + i)!!
                )

                val jsonObject = JsonObject()
                jsonObject.addProperty("BaseEntry", list[i].DocEntry)
                jsonObject.addProperty("LineNum", list[i].LineNum)
                jsonObject.addProperty("Price", list[i].Price)
                jsonObject.addProperty("Quantity", list[i].totalOpenDefault)// list[i].RemainingOpenQuantity
                jsonObject.addProperty("RateField", list[i].RateField)
                jsonObject.addProperty("TaxCode", list[i].TaxCode)
                jsonObject.addProperty("ItemCode", list[i].ItemCode)
                jsonObject.addProperty("WhsCode", list[i].WarehouseCode)
                jsonObject.addProperty("BaseLine", list[i].LineNum)
                jsonObject.addProperty("U_Size", list[i].Size)

                val stockBin = JsonArray()
                val batchBin = JsonArray()
                val serialBin = JsonArray()

                batchList = hashMapBatchList.get("Item" + i)!!
                batchQuantityList = hashmapBatchQuantityList.get("Item" + i)!!

                // 👉 1. Group binAllocationJSONs by BinAbsEntry to sum Quantity for stockBin
                val groupedBins = list[i].binAllocationJSONs
                    .groupBy { it.BinAbsEntry }
                    .mapValues { entry ->
                        entry.value.sumOf { it.Quantity.toDoubleOrNull() ?: 0.0 }
                    }

                for ((binAbsEntry, totalQty) in groupedBins) {
                    val jsonLinesObject = JsonObject()
                    jsonLinesObject.addProperty("BinAbsEntry", binAbsEntry)
                    jsonLinesObject.addProperty("Quantity", totalQty)
                    stockBin.add(jsonLinesObject)
                }

                if (list[i].BinManaged.equals("Y", true)) {

                    for (j in list[i].binAllocationJSONs.indices) {
                        var jsonLinesObject = JsonObject()
                        var batchObject = JsonObject()
                        var serialObject = JsonObject()

                        /* jsonLinesObject.addProperty("BinAbsEntry", list[i].binAllocationJSONs[j].BinAbsEntry)
                         jsonLinesObject.addProperty("Quantity", list[i].binAllocationJSONs[j].Quantity)
                         stockBin.add(jsonLinesObject)*/

                        if (list[i].ItemType.equals("BATCH", true) && list[i].ScanType.equals("N", true)) {

                            batchObject.addProperty("batchNumber", list[i].binAllocationJSONs[j].BatchNum)
                            batchObject.addProperty("Quantity", list[i].binAllocationJSONs[j].Quantity)

                            batchObject.addProperty("ManufacturerSerialNumber", list[i].binAllocationJSONs[j].ManufacturerSerialNumber)
                            batchObject.addProperty("InternalSerialNumber", list[i].binAllocationJSONs[j].InternalSerialNumber)
                            batchObject.addProperty("ExpiryDate", list[i].binAllocationJSONs[j].ExpiryDate)
                            batchObject.addProperty("ManufacturingDate", list[i].binAllocationJSONs[j].ManufacturingDate)

                            batchBin.add(batchObject)

                        } else if (list[i].ScanType.equals("N", true) && list[i].ItemType.equals("SERIAL", true)) {
                            serialObject.addProperty("InternalSerialNumber", list[i].binAllocationJSONs[j].BinLocation)
                            serialObject.addProperty("Quantity", list[i].binAllocationJSONs[j].Quantity)

                            serialBin.add(serialObject)

                        }
                    }

                    jsonObject.add("binAllocationJSONs", stockBin)

                    jsonObject.add("manualBatch", batchBin)
                    jsonObject.add("serialManual", serialBin)

                    //StockTransferLines.add(jsonObject)

                    if (list[i].binAllocationJSONs.size > 0 && list[i].BinManaged.equals("Y", true)) {
                        StockTransferLines.add(jsonObject)
                        Log.e(" IF At=>" + i, list[i].binAllocationJSONs.size.toString())

                    } else if (list[i].ScanType == "N" && batchList.size > 0) {
                        StockTransferLines.add(jsonObject)
                        Log.e(" EL IF At=>" + i, list[i].binAllocationJSONs.size.toString())
                    }

                } else {
                    val stockBin = JsonArray()
                    jsonObject.add("binAllocationJSONs", stockBin)
                    jsonObject.add("serialManual", stockBin)
                    jsonObject.add("manualBatch", stockBin)
                    StockTransferLines.add(jsonObject)
                    Log.e(" OUT EL At=>" + i, list[i].binAllocationJSONs.size.toString())
                }

            }

            postedJson.add("DocumentLines", StockTransferLines)

            FreightBottomDialogFragment(freightType, taxList, postedJson.toString(), onUpdateFreightData = { it, rounding, roundOffValue ->

                Log.i("FREIGHT_CHARGES", "Call back from Freight Bottom Dialog: ${toSimpleJson(AppConstants.freightDataList)}")
                val finalPostingJson = if (AppConstants.freightDataList.size > 0 || (rounding.isNotEmpty())) addDocumentAdditionalExpenses(postedJson.toString(), it, rounding, roundOffValue) else
                    postedJson

                Log.i("FREIGHT_CHARGES", "Call back => Final Posting Json:\n ${toSimpleJson(finalPostingJson)}")

                var apiConfig = ApiConstantForURL()

                NetworkClients.updateBaseUrlFromConfig(apiConfig)

                QuantityNetworkClient.updateBaseUrlFromConfig(apiConfig)
                if (true) {
                    materialProgressDialog.show()
                    val networkClient = QuantityNetworkClient.create(this@PurchaseTransferLinesActivity)
                    networkClient.GRPO_Posting(finalPostingJson).apply {
                        enqueue(object : Callback<PurchasePostResponse> {
                            override fun onResponse(
                                call: Call<PurchasePostResponse>,
                                response: Response<PurchasePostResponse>
                            ) {
                                try {
                                    activityFormBinding.chipSave.isEnabled = true
                                    activityFormBinding.chipSave.isCheckable = true

                                    AppConstants.IS_SCAN = false
                                    materialProgressDialog.dismiss()
                                    staticString = ""
                                    Log.e("success---BP---", "==>" + response.code())
                                    if (response.code() == 201 || response.code() == 200) {
                                        AppConstants.freightDataList.clear()
                                        Log.e("success------", "Successful!")
                                        Log.d(
                                            "Doc_Num",
                                            "onResponse: " + response.body()!!.DocNum.toString()
                                        )
                                        //GlobalMethods.showSuccess(this@PurchaseTransferLinesActivity, "Post Successfully. " + response.body()!!.DocNum.toString())
                                        showSuccessDialog(
                                            context = this@PurchaseTransferLinesActivity,
                                            title = "Goods Receipt PO",
                                            successMsg = "Goods receipt PO post successfully with docnum ",
                                            docNum = response.body()?.DocNum.toString(),
                                            cancelable = true
                                        ) {
                                            finish()
                                        }
                                    } else {
                                        materialProgressDialog.dismiss()
                                        val gson1 = GsonBuilder().create()
                                        var mError: OtpErrorModel
                                        try {
                                            val s = response.errorBody()!!.string()
                                            mError = gson1.fromJson(s, OtpErrorModel::class.java)
                                            if (mError.error.code.equals(400)) {
                                                GlobalMethods.showError(
                                                    this@PurchaseTransferLinesActivity,
                                                    mError.error.message.value
                                                )
                                            }
                                            if (mError.error.message.value != null) {
                                                GlobalMethods.showError(
                                                    this@PurchaseTransferLinesActivity,
                                                    mError.error.message.value
                                                )
                                                Log.e("json_error------", mError.error.message.value)
                                            }
                                        } catch (e: IOException) {
                                            e.printStackTrace()
                                        }

                                    }
                                } catch (e: Exception) {
                                    activityFormBinding.chipSave.isEnabled = true
                                    activityFormBinding.chipSave.isCheckable = true
                                    materialProgressDialog.dismiss()
                                    e.printStackTrace()
                                    Log.e("catch---------", e.toString())
                                }

                            }

                            override fun onFailure(call: Call<PurchasePostResponse>, t: Throwable) {
                                activityFormBinding.chipSave.isEnabled = true
                                activityFormBinding.chipSave.isCheckable = true
                                Log.e("orderLines_failure-----", t.toString())
                                materialProgressDialog.dismiss()
                            }

                        })
                    }
                } else {
                    Toast.makeText(
                        this@PurchaseTransferLinesActivity,
                        "Api block",
                        Toast.LENGTH_SHORT
                    ).show()
                }

            }).show(supportFragmentManager, "ServiceAreaActivity")
            /*val json = addDocumentAdditionalExpenses(postedJson.toString())
            Log.e("success--PayLoad==>", "==>" + json.toString())*/


        } else {
            materialProgressDialog.dismiss()
            handler.post {
                Toast.makeText(
                    this@PurchaseTransferLinesActivity,
                    "No Network Connection",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    fun addDocumentAdditionalExpenses(
        originalJson: String,
        expensesList: ArrayList<FreightDataModel.DocumentAdditionalExpenses>,
        rounding: String,
        roundOffValue: Double
    ): JsonObject {
        val gson = Gson()

        // Parse original JSON to JsonObject
        val parentObject = JsonParser.parseString(originalJson).asJsonObject
        parentObject.apply {
            addProperty("Rounding", rounding)
            addProperty("RoundingDiffAmount", roundOffValue)
        }

        // Convert expensesList to JsonArray
        val expenseArray = JsonArray()
        expensesList.forEach { item ->
            val expenseJson = JsonObject().apply {
                addProperty("ExpenseCode", item.ExpenseCode)
                addProperty("LineTotal", item.LineTotal)
                addProperty("TaxLiable", item.TaxLiable)
                addProperty("TaxCode", item.TaxCode)
                addProperty("TaxSum", item.TaxSum)
                addProperty("LineGross", item.LineGross)
                addProperty("DistributionMethod", item.DistributionMethod)
            }
            expenseArray.add(expenseJson)
        }

        // Add the array to the parent object
        parentObject.add("DocumentAdditionalExpenses", expenseArray)

        return parentObject
    }

    fun addDocumentAdditionalExpenses(originalJson: String): JsonObject {
        // Parse original string to JSONObject
        val parentObject = JSONObject(originalJson)

        // Create the additional expense item
        val expenseItem = JSONObject().apply {
            put("ExpenseCode", "FREIGHT")
            put("LineTotal", 100.0)
            put("TaxLiable", "tYES")
            put("TaxCode", "IGST@12")
            put("TaxSum", 12.0)
            put("LineGross", 112.0)
            put("DistributionMethod", "aedm_None")
        }

        // Add to a JSONArray
        val expenseArray = JSONArray().apply {
            put(expenseItem)
        }

        // Add to the original object
        parentObject.put("DocumentAdditionalExpenses", expenseArray)

        // Convert org.json.JSONObject to com.google.gson.JsonObject
        return JsonParser.parseString(parentObject.toString()).asJsonObject
    }

    val handler = Handler(Looper.getMainLooper())


    //todo getting BPL_ID Number ....
    private fun getBPL_IDNumber() {
        if (networkConnection.getConnectivityStatusBoolean(applicationContext)) {
            try {
                materialProgressDialog.show()
                var apiConfig = ApiConstantForURL()

                NetworkClients.updateBaseUrlFromConfig(apiConfig)

                QuantityNetworkClient.updateBaseUrlFromConfig(apiConfig)

                val networkClient = NetworkClients.create(this@PurchaseTransferLinesActivity)
                var batch = "Quality"//WIP
                networkClient.doGetBplID(
                    "BusinessPlaceID,WarehouseCode",
                    "WarehouseCode eq '" + sessionManagement.getWarehouseCode(this) + "'"
                ).apply {
                    enqueue(object : Callback<WarehouseBPL_IDModel> {
                        override fun onResponse(
                            call: Call<WarehouseBPL_IDModel>,
                            response: Response<WarehouseBPL_IDModel>
                        ) {
                            try {
                                materialProgressDialog.dismiss()
                                if (response.isSuccessful) {
                                    var responseModel = response.body()!!
                                    if (!responseModel.value.isNullOrEmpty()) {
                                        BPLIDNum = responseModel.value[0].BusinessPlaceID
//                                        getWarehouseCode = responseModel.value[0].WarehouseCode
                                        sessionManagement.setWarehouseCode(
                                            this@PurchaseTransferLinesActivity,
                                            responseModel.value[0].WarehouseCode
                                        )
//                                        Toast.makeText(this@InventoryTransferLinesActivity, BPLIDNum.toString(), Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(
                                            this@PurchaseTransferLinesActivity,
                                            "Not Found!",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                } else {
                                    materialProgressDialog.dismiss()
                                    val gson1 = GsonBuilder().create()
                                    var mError: OtpErrorModel
                                    try {
                                        val s = response.errorBody()!!.string()
                                        mError = gson1.fromJson(s, OtpErrorModel::class.java)
                                        if (mError.error.code.equals(400)) {
                                            GlobalMethods.showError(
                                                this@PurchaseTransferLinesActivity,
                                                mError.error.message.value
                                            )
                                        }
                                        if (mError.error.message.value != null) {
                                            GlobalMethods.showError(
                                                this@PurchaseTransferLinesActivity,
                                                mError.error.message.value
                                            )
                                            Log.e("json_error------", mError.error.message.value)
                                        }
                                    } catch (e: IOException) {
                                        e.printStackTrace()
                                    }
                                }

                            } catch (e: InvocationTargetException) {
                                materialProgressDialog.dismiss()
                                e.printStackTrace()
                                Log.e("error---------", e.toString())
                            }
                        }

                        override fun onFailure(call: Call<WarehouseBPL_IDModel>, t: Throwable) {
                            Log.e("scannedItemFailure-----", t.toString())
                            materialProgressDialog.dismiss()
                        }

                    })
                }

            } catch (e: Exception) {
                e.printStackTrace()
                Log.e("e-----------", e.toString())
            }

        } else {
            materialProgressDialog.dismiss()
            Toast.makeText(applicationContext, "No Network Connection", Toast.LENGTH_SHORT).show()
        }

    }


    //todo getting api list response from adapter to activity trough interface...
    override fun passList(dataList: List<ScanedOrderBatchedItems.Value>) {
        batchList = dataList
    }

    //todo onActivity function override for qr code scanning in adapter..
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == RESULT_OK) {
            Log.e("Result==>B", data?.getStringExtra("batch_code").toString())
            PurchaseTransferItemAdapter.onActivityResult(requestCode, resultCode, data)
        }
    }

    //todo query for series..
    fun getSeriesValue(docDate: String): String {
        var series = ""
        if (connection != null) {
            var statement: Statement? = null
            try {
                statement = connection!!.createStatement()
                var resultSet =
                    statement.executeQuery("Select  T0. Series as SeriesCode, T0.SeriesName  From NNM1 T0 WHERE T0.ObjectCode ='60'  and T0.Indicator=(select distinct Indicator from OFPR where '$docDate' between F_RefDate and T_RefDate ) and T0.Locked='N'")
                while (resultSet.next()) {
                    Log.e("ConStatus", "Success=>" + resultSet.getString(1))
                    //todo remove zero digits from quantity...
                    series = resultSet.getString(1)
                    Log.e("series", "Success=>" + series)
                }

            } catch (e: SQLException) {
                e.printStackTrace()
            }
        } else {
            Log.e("Result=>", "Connection is null")
        }
        return series
    }


    fun deleteCache(context: Activity) {
        try {
            val dir: File = context.getCacheDir()
            deleteDir(dir)
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
    }

    fun deleteDir(dir: File?): Boolean {
        return if (dir != null && dir.isDirectory) {
            val children = dir.list()
            for (i in children.indices) {
                val success = deleteDir(File(dir, children[i]))
                if (!success) {
                    return false
                }
            }
            dir.delete()
        } else if (dir != null && dir.isFile) {
            dir.delete()
        } else {
            false
        }
    }


    private val barcode = StringBuffer()

    override fun dispatchKeyEvent(event: KeyEvent?): Boolean {

        if (event?.action == KeyEvent.ACTION_DOWN) {
            val pressedKey = event.unicodeChar.toChar()
            barcode.append(pressedKey)
        }
        if (event?.action == KeyEvent.ACTION_DOWN && event?.keyCode == KeyEvent.KEYCODE_ENTER) {
            barcode.delete(0, barcode.length)
        }

        return super.dispatchKeyEvent(event)
    }

}