package com.wms.panchkula.ui.issueForProductionOrder.UI.productionOrderLines

import android.Manifest
import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.*
import android.util.Log
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.wms.panchkula.Global_Classes.AppConstants
import com.wms.panchkula.ui.issueForProductionOrder.Adapter.ProductionOrderLinesAdapter
import com.wms.panchkula.Global_Classes.GlobalMethods
import com.wms.panchkula.Global_Classes.MaterialProgressDialog
import com.wms.panchkula.Global_Notification.NetworkConnection
import com.wms.panchkula.Model.*
import com.wms.panchkula.Retrofit_Api.NetworkClients
import com.wms.panchkula.Retrofit_Api.QuantityNetworkClient
import com.wms.panchkula.SessionManagement.SessionManagement
import com.wms.panchkula.databinding.ActivityProductionOrderLinesBinding
import com.wms.panchkula.interfaces.PassList
import com.wms.panchkula.Retrofit_Api.ApiConstantForURL
import com.wms.panchkula.ui.issueForProductionOrder.Model.InventoryGenExitsModel
import com.wms.panchkula.ui.issueForProductionOrder.Model.ProductionListModel
import com.wms.panchkula.ui.issueForProductionOrder.Model.ScanedOrderBatchedItems
import com.wms.panchkula.ui.issueForProductionOrder.Model.WarehouseBPL_IDModel
import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.wms.panchkula.Adapter.ErrorDialogAdapter
import com.wms.panchkula.Global_Classes.GlobalMethods.convert_dd_MM_yyyy_into_yyyy_MM_dd
import com.wms.panchkula.Global_Classes.GlobalMethods.convert_yyyy_MM_dd_T_hh_mm_ss_into_ddMMYYYY
import com.wms.panchkula.Global_Classes.GlobalMethods.showSuccessDialog
import com.wms.panchkula.Global_Classes.GlobalMethods.toPrettyJson
import com.wms.panchkula.Global_Classes.GlobalMethods.toSimpleJson
import com.wms.panchkula.R
import com.wms.panchkula.ui.production.model.batchCode.StageStatusUpdateRequest
import com.wms.panchkula.ui.production.model.batchCode.StageUpdateRequest
import com.wms.panchkula.ui.purchase.model.PurchaseRequestModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.io.IOException
import java.lang.reflect.InvocationTargetException
import java.net.HttpURLConnection
import java.net.URL
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException
import java.sql.Statement
import java.util.*
import kotlin.collections.ArrayList


private const val TAG = "ProductionOrderLinesAct"

class ProductionOrderLinesActivity : AppCompatActivity(), PassList, ProductionOrderLinesAdapter.AdapterCallback {

    private lateinit var activityFormBinding: ActivityProductionOrderLinesBinding
    private var productionOrderLinesAdapter: ProductionOrderLinesAdapter? = null
    private lateinit var productionOrderLineList_gl: ArrayList<ProductionListModel.ProductionOrderLine>

    private var errorList = ArrayList<ErrorItemDetails>()

    //    private lateinit var productionOrderValueList_gl: ArrayList<ProductionListModel.Value>
    private lateinit var productionOrderValueList_gl: ProductionListModel.Value
    var position: Int? = 0
    lateinit var networkConnection: NetworkConnection
    lateinit var materialProgressDialog: MaterialProgressDialog
    private lateinit var sessionManagement: SessionManagement
    private var BPLIDNum = 0
    private var valueList: List<ProductionListModel.Value> = ArrayList()
    private var connection: Connection? = null
    private lateinit var postingDate: String
    private lateinit var stageId: String
    var openQty = 0.0

    //todo batch scan and quantity list interface override...
    var hashMapBatchList: HashMap<String, ArrayList<ScanedOrderBatchedItems.Value>> = HashMap<String, ArrayList<ScanedOrderBatchedItems.Value>>()
    var hashmapBatchQuantityList: HashMap<String, ArrayList<String>> = HashMap<String, ArrayList<String>>()
    var serialHashMapQuantityList: HashMap<String, ArrayList<String>> = HashMap<String, ArrayList<String>>()
    var noneHashMapQuantityList: java.util.HashMap<String, kotlin.collections.ArrayList<String>> = java.util.HashMap<String, java.util.ArrayList<String>>()


    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activityFormBinding = ActivityProductionOrderLinesBinding.inflate(layoutInflater)
        setContentView(activityFormBinding.root)
        title = "Form Screen"

        deleteCache(this)

        supportActionBar?.hide()

        //todo initialization...
        networkConnection = NetworkConnection()
        materialProgressDialog = MaterialProgressDialog(this@ProductionOrderLinesActivity)
        sessionManagement = SessionManagement(this@ProductionOrderLinesActivity)


        val delayMillis = 1000 // 1 second
        handler.postDelayed({
//            setSqlServer()
        }, delayMillis.toLong())


        //todo get arguments data...
        try {
            val intent = intent
            productionOrderLineList_gl = intent.getSerializableExtra("productionLinesList") as ArrayList<ProductionListModel.ProductionOrderLine>
            productionOrderValueList_gl =
                intent.getSerializableExtra("productionValueList") as ProductionListModel.Value //todo getting list selected item values and lines only not all size data..
            position = intent.extras?.getInt("pos")
            stageId = intent.extras?.getString("stageId").toString()
            postingDate = convert_yyyy_MM_dd_T_hh_mm_ss_into_ddMMYYYY(productionOrderValueList_gl.PostingDate)
            valueList = listOf(productionOrderValueList_gl)

            Log.i("PO_LIST", "productionOrderLineList_gl: ${toPrettyJson(productionOrderLineList_gl)}")

        } catch (e: IOException) {
            Log.e(TAG, "onCreate:===> " + e.message)
            e.printStackTrace()
        }

//        getWarehouseCode = intent.extras?.getString("warehouseCode")!!


        //todo calling BPLID here...
        getBPL_IDNumber()

        activityFormBinding.tvItemNo.text = productionOrderValueList_gl.ItemNo


        activityFormBinding.ivOnback.setOnClickListener {
            onBackPressed()
        }

        activityFormBinding.etDocDate.setOnClickListener {
            GlobalMethods.disableDatesBetweenPoDateAndToday(this@ProductionOrderLinesActivity, activityFormBinding.etDocDate, postingDate)
        }


        //todo cancel lines...
        activityFormBinding.chipCancel.setOnClickListener {
            onBackPressed()
        }


    }

    /*fun showErrorDialog(context: Context, itemList: ArrayList<ErrorItemDetails>) {
        val dialog = Dialog(context)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.error_dialog_layout)

        val recyclerView = dialog.findViewById<RecyclerView>(R.id.rvErrorDialog)
        val btnOk = dialog.findViewById<Button>(R.id.btnOk)
        recyclerView.layoutManager = LinearLayoutManager(context,LinearLayoutManager.VERTICAL,false)

        val adapter = ErrorDialogAdapter(this@ProductionOrderLinesActivity, itemList)
        recyclerView.adapter = adapter

        btnOk.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }*/

    private fun showErrorDialog(
        context: Context, value: ArrayList<ErrorItemDetails>
    ) {

        val dialog = Dialog(context)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(true)
        dialog.setContentView(R.layout.error_dialog_layout)
        dialog.setCancelable(false)
        // Ensure the background is transparent
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        // Set dialog width to MATCH_PARENT
        dialog.window?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT
        )
        //dialog.show()
        val recyclerView = dialog.findViewById<RecyclerView>(R.id.rvErrorDialog)
        val btnOk = dialog.findViewById<Button>(R.id.btnOk)



        Log.e("RETURN_COMPONENT", "openDynamicFieldsDialog=> List ${toPrettyJson(value)}")
        recyclerView.run {

            layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
            val errorDialogAdapter = ErrorDialogAdapter(this@ProductionOrderLinesActivity, value)
            adapter = errorDialogAdapter
            errorDialogAdapter?.notifyDataSetChanged()
            setHasFixedSize(true)
        }

        btnOk.setOnClickListener {
            dialog.dismiss()
        }
        dialog.show()

    }


    var tempList: ArrayList<ProductionListModel.ProductionOrderLine> = ArrayList()

    //todo set adapter....
    fun setAdapter() {
        //todo removing order line if BaseQuantity value in negative.... and also this way is removing IndexOutOfBoundException from list....

        for (i in 0 until productionOrderLineList_gl.size) {
            var j: Int = i
            try {
                val plannedQty = productionOrderLineList_gl[j].PlannedQuantity
                val issuedQty = productionOrderLineList_gl[j].IssuedQuantity
                val openQty = plannedQty - issuedQty
                //if (productionOrderLineList_gl[j].BaseQuantity > 0.0 && openQty > 0.0) {
                tempList.add(productionOrderLineList_gl[j])
                //}
            } catch (e: IndexOutOfBoundsException) {
                e.printStackTrace()
            }
        }

        var width = productionOrderValueList_gl.U_Width
        var length = productionOrderValueList_gl.U_Length
        var gsm = productionOrderValueList_gl.U_GSM

        if (tempList.size > 0) {
            activityFormBinding.ivNoDataFound.visibility = View.GONE
            activityFormBinding.rvProductionOrderList.visibility = View.VISIBLE
            activityFormBinding.btnLinearLayout.visibility = View.VISIBLE

            val layoutManager: RecyclerView.LayoutManager = LinearLayoutManager(this)
            activityFormBinding.rvProductionOrderList.layoutManager = layoutManager
            //todo parse save button in adapter constructor for click listener on adapter...
            productionOrderLinesAdapter = ProductionOrderLinesAdapter(
                this@ProductionOrderLinesActivity,
                tempList,
                networkConnection,
                materialProgressDialog,
                this@ProductionOrderLinesActivity,
                activityFormBinding.chipSave,
                width,
                length,
                gsm
            )//getWarehouseCode
            activityFormBinding.rvProductionOrderList.adapter = productionOrderLinesAdapter
            productionOrderLinesAdapter?.notifyDataSetChanged()
        } else {
            activityFormBinding.ivNoDataFound.visibility = View.VISIBLE
            activityFormBinding.rvProductionOrderList.visibility = View.GONE
            activityFormBinding.btnLinearLayout.visibility = View.GONE
        }
    }

    private fun callUpdateStageStatusApi(requestModel: StageStatusUpdateRequest) {
        if (networkConnection.getConnectivityStatusBoolean(applicationContext)) {
            materialProgressDialog.show()
            var apiConfig = ApiConstantForURL()

            NetworkClients.updateBaseUrlFromConfig(apiConfig)

            val networkClient = NetworkClients.create(this@ProductionOrderLinesActivity)

            networkClient.updateStageStatus(productionOrderValueList_gl.AbsoluteEntry, requestModel).apply {
                enqueue(object : Callback<Void> {
                    override fun onResponse(
                        call: Call<Void>,
                        response: Response<Void>
                    ) {
                        try {
                            materialProgressDialog.dismiss()
                            if (response.isSuccessful && response.code() == 204) {
                                Log.d("success------", "U_Status updated successfully")
                                /* runOnUiThread {
                                     GlobalMethods.showSuccess(
                                         this@ProductionOrderLinesActivity,
                                         "Stage updated successfully."
                                     )
                                 }

                                 Handler(Looper.getMainLooper()).postDelayed({
                                     finish()
                                 }, 1000)*/
                            } else {
                                materialProgressDialog.dismiss()
                                val gson1 = GsonBuilder().create()
                                var mError: OtpErrorModel
                                try {
                                    val s = response.errorBody()!!.string()
                                    mError = gson1.fromJson(s, OtpErrorModel::class.java)
                                    if (mError.error.code.equals(400)) {
                                        GlobalMethods.showError(
                                            this@ProductionOrderLinesActivity,
                                            mError.error.message.value
                                        )
                                    }
                                    if (mError.error.message.value != null) {
                                        GlobalMethods.showError(
                                            this@ProductionOrderLinesActivity,
                                            mError.error.message.value
                                        )
                                        Log.e("json_error------", mError.error.message.value)
                                    }
                                } catch (e: IOException) {
                                    e.printStackTrace()
                                }

                            }
                        } catch (e: Exception) {
                            materialProgressDialog.dismiss()
                            e.printStackTrace()
                            Log.e("catch---------", e.toString())
                        }

                    }

                    override fun onFailure(call: Call<Void>, t: Throwable) {
                        Log.e("orderLines_failure-----", t.toString())
                        materialProgressDialog.dismiss()
                    }

                })
            }
        } else {
            materialProgressDialog.dismiss()
            GlobalMethods.showError(this@ProductionOrderLinesActivity, "No network connection")
        }
    }

    //todo getting batch order lines and quantity data from adapter to activity...
    override fun onApiResponse(
        response: HashMap<String, ArrayList<ScanedOrderBatchedItems.Value>>,
        quantityResponse: HashMap<String, ArrayList<String>>,
        serialQuantityResponse: HashMap<String, ArrayList<String>>,
        noneQuantityResponse: java.util.HashMap<String, kotlin.collections.ArrayList<String>>
    ) {
        /*for ((k, v) in response) {
            Log.e("adpResponse---->", k + "-->" + v.toString())
            hashMapBatchList.put(k, v)
        }*/

        hashMapBatchList = response
        hashmapBatchQuantityList = quantityResponse
        serialHashMapQuantityList = serialQuantityResponse
        noneHashMapQuantityList = noneQuantityResponse
        Log.e("ISSUE_PRODUCTION", "hashmap---> ${toSimpleJson(hashMapBatchList)}")
        Log.e("ISSUE_PRODUCTION", "batchQuantityList--> ${toSimpleJson(hashmapBatchQuantityList)}")
        Log.e("ISSUE_PRODUCTION", "batchQuantityList--> ${toSimpleJson(serialHashMapQuantityList)}")
        saveProductionOrderLinesItems()

    }

    lateinit var batchList: List<ScanedOrderBatchedItems.Value>
    lateinit var batchQuantityList: ArrayList<String>

    //todo here save issue for production lines items of order...

    private fun saveProductionOrderLinesItems() {

        val comments = valueList[0].Remarks.toString()
        if (activityFormBinding.etDocDate.text.toString().trim().isEmpty()) {
            GlobalMethods.showError(this@ProductionOrderLinesActivity, "Please Select Posting Date")
            return
        }
        val docDate = convert_dd_MM_yyyy_into_yyyy_MM_dd(activityFormBinding.etDocDate.text.toString().trim())//valueList[0].PostingDate
        val absoluteEntry = valueList[0].AbsoluteEntry

        if (networkConnection.getConnectivityStatusBoolean(applicationContext)) {

            val postedJson = JsonObject().apply {
                addProperty("BPL_IDAssignedToInvoice", BPLIDNum)
                addProperty("Comments", comments)
                addProperty("DocDate", docDate)
                addProperty("Series", valueList[0].SeriesCode)
                addProperty("U_WMSPOST", "Y") //U_WMSPOST tagged (added by Vinod @13Aug,2025)
                addProperty("U_WMSUSER", sessionManagement.getUsername(this@ProductionOrderLinesActivity))  //WMS userName tagged (added by Vinod @13Aug,2025)
            }

            val DocumentLinesArray = JsonArray()
            Log.i("ISSUE_PRODUCTION", "Scan DocLineItem => ${toSimpleJson(tempList)}")
            errorList.clear()
            for (i in tempList.indices) {
                val item = tempList[i]
                if (item.binAllocationJSONs.isNullOrEmpty() && item.BinManaged == "Y" && item.isScanned > 0) {
                    errorList.add(ErrorItemDetails(i + 1, item.ItemNo, item.BatchNumber.toString(), item.ItemName.toString()))
                }
            }

            for (i in tempList.indices) {
                val item = tempList[i]

                if (item.isScanned <= 0) continue

                val floatString = item.LineNumber
                val baseLine = floatString.toDouble().toInt()
                val jsonObject = JsonObject().apply {
                    addProperty("BaseEntry", absoluteEntry)
                    addProperty("BaseLine", baseLine)
                    addProperty("BaseType", "202")
                    addProperty("WarehouseCode", item.Warehouse)
                    addProperty("Factor1", 0.0)
                    addProperty("Factor2", 0.0)
                    addProperty("Factor3", 1)
                    addProperty("Factor4", 0.0)
                }

                // === Batch Case ===
                if (item.Batch != null && item.Batch == "Y") {
                    /*if (item.binAllocationJSONs.isNullOrEmpty()){
                        GlobalMethods.showError(this@ProductionOrderLinesActivity,"Please select a bin location before proceeding with batch item selection.")
                        return
                    }
                    val binArray = getAggregatedBinArray(item.binAllocationJSONs, "0")*/
                    Log.e("ISSUE_PRODUCTION", "binAllocationJSONs => ${toSimpleJson(item.binAllocationJSONs)}")
                    val binArray = if (item.binAllocationJSONs.isNullOrEmpty()) {
                        if (item.BinManaged == "Y") {
                            //Toast.makeText(this@ProductionOrderLinesActivity, "Please select a bin location before proceeding with batch item selection.", Toast.LENGTH_SHORT).show()
                            showErrorDialog(this@ProductionOrderLinesActivity, errorList)
                            //ErrorDialogFragment(errorList).show(supportFragmentManager, ErrorDialogFragment.TAG)
                            return
                        } else
                            JsonArray() // Provide empty array if no bin data
                    } else {
                        getAggregatedBinArray(item.binAllocationJSONs, "0")
                    }

                    val batchList = hashMapBatchList["Item$i"] ?: continue
                    val batchQuantityList = hashmapBatchQuantityList["Item$i"] ?: continue

                    // Batch Numbers
                    val BatchNumbersArray = JsonArray()
                    var u_width = 0.0
                    var u_length = 0.0
                    var u_gsm = 0.0

                    for (j in batchList.indices) {
                        for (k in j until batchQuantityList.size) {
                            val batch = batchList[j]
                            u_width = batch.U_Width
                            u_length = batch.U_Length
                            u_gsm = batch.U_GSM

                            val jsonLinesObject = JsonObject().apply {
                                addProperty("BatchNumber", batch.Batch)
                                addProperty("SystemSerialNumber", batch.SystemNumber)
                                addProperty("Quantity", batchQuantityList[k])
                            }
                            BatchNumbersArray.add(jsonLinesObject)
                            break
                        }
                    }

                    val quantity = GlobalMethods.sumBatchQuantity(i, batchQuantityList)
                    jsonObject.addProperty("Quantity", quantity)
                    jsonObject.addProperty("Factor1", u_width)
                    jsonObject.addProperty("Factor2", u_length)
                    jsonObject.addProperty("Factor4", u_gsm)
                    jsonObject.add("BatchNumbers", BatchNumbersArray)
                    jsonObject.add("DocumentLinesBinAllocations", binArray)

                    if (batchList.isNotEmpty()) {
                        DocumentLinesArray.add(jsonObject)
                    }
                }

                // === Serial Case ===
                else if (item.Serial != null && item.Serial == "Y") {
                    /*if (item.binAllocationJSONs.isNullOrEmpty()){
                        GlobalMethods.showError(this@ProductionOrderLinesActivity,"Please select a bin location before proceeding with Serial item selection.")
                        return
                    }
                    val binArray = getAggregatedBinArray(item.binAllocationJSONs, "0")*/

                    val binArray = if (item.binAllocationJSONs.isNullOrEmpty()) {
                        if (item.BinManaged == "Y") {
                            //Toast.makeText(this@ProductionOrderLinesActivity, "Please select a bin location before proceeding with batch item selection.", Toast.LENGTH_SHORT).show()
                            showErrorDialog(this@ProductionOrderLinesActivity, errorList)
                            //ErrorDialogFragment(errorList).show(supportFragmentManager, ErrorDialogFragment.TAG)
                            return
                        } else
                            JsonArray() // Provide empty array if no bin data
                    } else {
                        getAggregatedBinArray(item.binAllocationJSONs, "0")
                    }

                    val serialList = hashMapBatchList["Item$i"] ?: continue
                    val serialQuantityList = serialHashMapQuantityList["Item$i"] ?: continue

                    val SerialNumbersArray = JsonArray()
                    var u_width = 0.0
                    var u_length = 0.0
                    var u_gsm = 0.0

                    for (j in serialList.indices) {
                        for (k in j until serialQuantityList.size) {
                            val serial = serialList[j]
                            u_width = serial.U_Width
                            u_length = serial.U_Length
                            u_gsm = serial.U_GSM

                            val jsonLinesObject = JsonObject().apply {
                                addProperty("SystemSerialNumber", serial.SystemNumber)
                                addProperty("InternalSerialNumber", serial.SerialNumber)
                                addProperty("Quantity", "1")
                            }
                            SerialNumbersArray.add(jsonLinesObject)
                            break
                        }
                    }

                    val quantity = GlobalMethods.sumBatchQuantity(i, serialQuantityList)
                    jsonObject.addProperty("Quantity", quantity)
                    jsonObject.addProperty("Factor1", u_width)
                    jsonObject.addProperty("Factor2", u_length)
                    jsonObject.addProperty("Factor4", u_gsm)
                    jsonObject.add("SerialNumbers", SerialNumbersArray)
                    jsonObject.add("DocumentLinesBinAllocations", binArray)

                    if (serialList.isNotEmpty()) {
                        DocumentLinesArray.add(jsonObject)
                    }
                }

                // === None Case ===
                else if (item.None != null && item.None == "Y") {
                    /*if (item.binAllocationJSONs.isNullOrEmpty()){
                        GlobalMethods.showError(this@ProductionOrderLinesActivity,"Please select a bin location before proceeding with None item selection.")
                        return
                    }
                    val binArray = getAggregatedBinArray(item.binAllocationJSONs, "-1")*/

                    val binArray = if (item.binAllocationJSONs.isNullOrEmpty()) {
                        if (item.BinManaged == "Y") {
                            //Toast.makeText(this@ProductionOrderLinesActivity, "Please select a bin location before proceeding with batch item selection.", Toast.LENGTH_SHORT).show()
                            showErrorDialog(this@ProductionOrderLinesActivity, errorList)
                            //ErrorDialogFragment(errorList).show(supportFragmentManager, ErrorDialogFragment.TAG)
                            return
                        } else
                            JsonArray() // Provide empty array if no bin data
                    } else {
                        getAggregatedBinArray(item.binAllocationJSONs, "0")
                    }

                    val noneQuantityList = noneHashMapQuantityList["Item$i"] ?: continue

                    val quantity = GlobalMethods.sumBatchQuantity(i, noneQuantityList)
                    jsonObject.addProperty("Quantity", quantity)
                    jsonObject.add("DocumentLinesBinAllocations", binArray)

                    DocumentLinesArray.add(jsonObject)
                }
            }

            postedJson.add("DocumentLines", DocumentLinesArray)

            Log.e("ISSUE_PRODUCTION", "Final Payload JSON :$postedJson")

            /*activityFormBinding.chipSave.isEnabled = false
            activityFormBinding.chipSave.isCheckable = false*/

            if (false)
                return

            materialProgressDialog.show()
            var apiConfig = ApiConstantForURL()

            NetworkClients.updateBaseUrlFromConfig(apiConfig)

            QuantityNetworkClient.updateBaseUrlFromConfig(apiConfig)
            // materialProgressDialog.dismiss()

            val networkClient = NetworkClients.create(this@ProductionOrderLinesActivity)
            networkClient.doGetInventoryGenExits(postedJson).apply {
                enqueue(object : Callback<InventoryGenExitsModel> {
                    override fun onResponse(
                        call: Call<InventoryGenExitsModel>,
                        response: Response<InventoryGenExitsModel>
                    ) {
                        try {
                            activityFormBinding.chipSave.isEnabled = true
                            activityFormBinding.chipSave.isCheckable = true
                            materialProgressDialog.dismiss()

                            if (response.isSuccessful) {
                                if (response.code() == 201) {
                                    Log.e("success------", "Successful!")
                                    // If validation passes, prepare stage request
                                    val stageStatusRequest = StageStatusUpdateRequest()

                                    // Add stage dynamically
                                    stageStatusRequest.ProductionOrdersStages.add(
                                        StageStatusUpdateRequest.ProductionOrderStage(
                                            StageID = stageId.toInt(),
                                            U_Status = "Yes"
                                        )
                                    )
                                    callUpdateStageStatusApi(stageStatusRequest)
                                    val docNum = response.body()?.DocNum ?: "N/A"

                                    showSuccessDialog(
                                        context = this@ProductionOrderLinesActivity,
                                        title = "Issue Production",
                                        successMsg = "Issue Production Order Post Successfully with docnum ",
                                        docNum = docNum,
                                        cancelable = true
                                    ) {
                                        finish() // or use finish() if preferred
                                    }
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
                                            this@ProductionOrderLinesActivity,
                                            mError.error.message.value
                                        )
                                    }
                                    if (mError.error.message.value != null) {
                                        GlobalMethods.showError(
                                            this@ProductionOrderLinesActivity,
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

                    override fun onFailure(call: Call<InventoryGenExitsModel>, t: Throwable) {
                        activityFormBinding.chipSave.isEnabled = true
                        activityFormBinding.chipSave.isCheckable = true
                        Log.e("orderLines_failure-----", t.toString())
                        materialProgressDialog.dismiss()
                    }

                })
            }


        } else {
            materialProgressDialog.dismiss()
            handler.post {
                Toast.makeText(
                    this@ProductionOrderLinesActivity,
                    "No Network Connection",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun getAggregatedBinArray(
        binAllocations: ArrayList<PurchaseRequestModel.binAllocationJSONs>,
        baseLineValue: String // pass "0" or dynamic value as needed
    ): JsonArray {
        val binArray = JsonArray()

        for (jj in binAllocations.indices) {
            val bin = binAllocations[jj]

            val entry = bin.BinAbsEntry
            val quantity = bin.Quantity

            if (!entry.isNullOrEmpty() && !quantity.isNullOrEmpty()) {
                val binObject = JsonObject().apply {
                    addProperty("BinAbsEntry", entry)
                    addProperty("Quantity", quantity)
                    addProperty("SerialAndBatchNumbersBaseLine", jj)
                }
                binArray.add(binObject)
            }
        }

        return binArray
    }


    /*private fun getAggregatedBinArray(
        binAllocations: ArrayList<PurchaseRequestModel.binAllocationJSONs>,
        baseLineValue: String
    ): JsonArray {
        val binArray = JsonArray()

        binAllocations.forEachIndexed { index, bin ->
            val entry = bin.BinAbsEntry?.toIntOrNull()
            val quantity = bin.Quantity?.toDoubleOrNull()

            if (entry != null && quantity != null) {
                val binObject = JsonObject().apply {
                    addProperty("BinAbsEntry", entry)
                    addProperty("Quantity", quantity)
                    addProperty("SerialAndBatchNumbersBaseLine", index)  // Using index here
                }
                binArray.add(binObject)
            }
        }

        return binArray
    }*/


    /* private fun getAggregatedBinArray(
         binAllocations: ArrayList<PurchaseRequestModel.binAllocationJSONs>,
         baseLineValue: String
     ): JsonArray {
         val binMap = mutableMapOf<Int, Double>()

         for (bin in binAllocations) {
             val entryStr = bin.BinAbsEntry
             val quantityStr = bin.Quantity

             // Convert safely to Int and Double
             val entry = entryStr?.toIntOrNull()
             val quantity = quantityStr?.toDoubleOrNull()

             if (entry != null && quantity != null) {
                 val currentQty = binMap[entry] ?: 0.0
                 binMap[entry] = currentQty + quantity
             }
         }

         val binArray = JsonArray()
         for ((entry, totalQty) in binMap) {
             val binObject = JsonObject().apply {
                 addProperty("BinAbsEntry", entry)
                 addProperty("Quantity", totalQty)
                 addProperty("SerialAndBatchNumbersBaseLine", baseLineValue)
             }
             binArray.add(binObject)
         }

         return binArray
     }*/


    val handler = Handler(Looper.getMainLooper())


    //todo getting BPL_ID Number ....
    private fun getBPL_IDNumber() {
        if (networkConnection.getConnectivityStatusBoolean(applicationContext)) {
            try {
                materialProgressDialog.show()
                var apiConfig = ApiConstantForURL()

                NetworkClients.updateBaseUrlFromConfig(apiConfig)

                QuantityNetworkClient.updateBaseUrlFromConfig(apiConfig)

                val networkClient = NetworkClients.create(this@ProductionOrderLinesActivity)
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
                                        // sessionManagement.setWarehouseCode(this@ProductionOrderLinesActivity, responseModel.value[0].WarehouseCode)
                                        setAdapter()
//                                        Toast.makeText(this@ProductionOrderLinesActivity, BPLIDNum.toString(), Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(
                                            this@ProductionOrderLinesActivity,
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
                                                this@ProductionOrderLinesActivity,
                                                mError.error.message.value
                                            )
                                        }
                                        if (mError.error.message.value != null) {
                                            GlobalMethods.showError(
                                                this@ProductionOrderLinesActivity,
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

        if (resultCode == Activity.RESULT_OK) {
            Log.e("Result==>", data?.getStringExtra("batch_code").toString())
            productionOrderLinesAdapter?.onActivityResult(requestCode, resultCode, data)
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

    //TODO set sql server for query...
    private fun setSqlServer() {
        val url = "jdbc:jtds:sqlserver://" + AppConstants.IP + ":" + AppConstants.PORT + "/" + sessionManagement.getCompanyDB(this)
        ActivityCompat.requestPermissions(this, arrayOf<String>(Manifest.permission.INTERNET), PackageManager.PERMISSION_GRANTED)
        val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)
        try {
            Class.forName(AppConstants.Classes)
            connection = DriverManager.getConnection(url, AppConstants.USERNAME, AppConstants.PASSWORD)
            Log.e("ConStatus", "Success$connection")

        } catch (e: ClassNotFoundException) {
            e.printStackTrace()
            Log.e("ConStatus", "Error")
        } catch (e: SQLException) {
            e.printStackTrace()
            Log.e("ConStatus", "Failure")
        }
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

    inner class BolicyTask : AsyncTask<Void, Void, String>() {

        override fun doInBackground(vararg params: Void?): String {
            val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
            StrictMode.setThreadPolicy(policy)

            return "Task completed"
        }

        override fun onPostExecute(result: String) {
            setSqlServer()
        }
    }


    suspend fun makeNetworkRequest(): String {
        // Use Retrofit or another networking library here
        // For simplicity, let's pretend we're using a simple HTTPURLConnection
        val url = URL("jdbc:jtds:sqlserver://" + AppConstants.IP + ":" + AppConstants.PORT + "/" + sessionManagement.getCompanyDB(this))
        val connection = url.openConnection() as HttpURLConnection

        return try {
            val inputStream = connection.inputStream
            val result = inputStream.bufferedReader().readText()
            result
        } finally {
            connection.disconnect()
        }
    }


    companion object {
        private const val INTERNET_PERMISSION_REQUEST_CODE = 1
    }


    private fun checkPermission(permission: String): Boolean {
        return ActivityCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestInternetPermission() {
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.INTERNET), INTERNET_PERMISSION_REQUEST_CODE)
    }

    private fun configureStrictMode() {
        val handler = Handler(Looper.getMainLooper())
        handler.post {
            val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
            StrictMode.setThreadPolicy(policy)
        }

        GlobalScope.launch(Dispatchers.IO) {
            // Perform your network operation here
            // For example, make an HTTP request using a library like Retrofit
            val url = makeNetworkRequest()
            try {
                Class.forName(AppConstants.Classes)
                connection = DriverManager.getConnection(url, AppConstants.USERNAME, AppConstants.PASSWORD)
                Log.e("ConStatus", "Success$connection")


            } catch (e: ClassNotFoundException) {
                e.printStackTrace()
                Log.e("ConStatus", "Error")
            } catch (e: SQLException) {
                e.printStackTrace()
                Log.e("ConStatus", "Failure")
            }
            // Once the operation is complete, you can update the UI if needed
            launch(Dispatchers.Main) {

            }
        }


        /*  val url = "jdbc:jtds:sqlserver://" + AppConstants.IP + ":" + AppConstants.PORT + "/" + AppConstants.COMPANY_DB
          Log.e("QSL==>", "Success$url")
          try {
              Class.forName(AppConstants.Classes)
              connection = DriverManager.getConnection(url, AppConstants.USERNAME, AppConstants.PASSWORD)
              Log.e("ConStatus", "Success$connection")


          } catch (e: ClassNotFoundException) {
              e.printStackTrace()
              Log.e("ConStatus", "Error")
          } catch (e: SQLException) {
              e.printStackTrace()
              Log.e("ConStatus", "Failure")
          }*/

        // Now, you can perform network-related operations with the INTERNET permission
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == INTERNET_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, configure StrictMode and perform network-related operations
                configureStrictMode()
            } else {
                // Permission denied, handle the denial (e.g., show a message to the user)
            }
        }
    }

}