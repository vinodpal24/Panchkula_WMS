package com.wms.panchkula.ui.production.ui.rfp

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.*
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.wms.panchkula.Retrofit_Api.ApiConstantForURL
import com.wms.panchkula.Retrofit_Api.NetworkClients
import com.wms.panchkula.Retrofit_Api.QuantityNetworkClient
import com.wms.panchkula.databinding.ActivityInventoryOrderLineBinding
import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.pixplicity.easyprefs.library.Prefs
import com.webapp.internetconnection.CheckNetwoorkConnection
import com.wms.panchkula.Global_Classes.AppConstants
import com.wms.panchkula.Global_Classes.GlobalMethods
import com.wms.panchkula.Global_Classes.GlobalMethods.convert_dd_MM_yyyy_into_yyyy_MM_dd
import com.wms.panchkula.Global_Classes.GlobalMethods.convert_yyyy_MM_dd_T_hh_mm_ss_into_ddMMYYYY
import com.wms.panchkula.Global_Classes.GlobalMethods.showSuccessDialog
import com.wms.panchkula.Global_Classes.MaterialProgressDialog
import com.wms.panchkula.Global_Notification.NetworkConnection
import com.wms.panchkula.Model.OtpErrorModel
import com.wms.panchkula.SessionManagement.SessionManagement
import com.wms.panchkula.interfaces.PassList
import com.wms.panchkula.ui.invoiceOrder.UI.TAG
import com.wms.panchkula.ui.issueForProductionOrder.Model.ScanedOrderBatchedItems
import com.wms.panchkula.ui.issueForProductionOrder.Model.WarehouseBPL_IDModel
import com.wms.panchkula.ui.login.LoginActivity
import com.wms.panchkula.ui.production.model.rfp.RFPItemAdapter
import com.wms.panchkula.ui.production.model.rfp.RFPResponse
import com.wms.panchkula.ui.purchase.model.PurchasePostResponse
import com.wms.panchkula.ui.purchase.model.PurchaseRequestModel
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.io.IOException
import java.lang.reflect.InvocationTargetException
import java.sql.Connection
import java.sql.SQLException
import java.sql.Statement
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter


class RFPLinesActivity : AppCompatActivity(), PassList,
    RFPItemAdapter.AdapterCallback {
    companion object {
        // Declare the static variable
        var staticString: String = ""
    }

    var inventoryItem: PurchaseRequestModel.Value? = null
    var inventoryItemNew: RFPResponse.Value? = null

    private lateinit var activityFormBinding: ActivityInventoryOrderLineBinding
    private lateinit var postingDate: String

    //    private lateinit var RFPItemAdapter: DemoAdapter
    private lateinit var RFPItemAdapter: RFPItemAdapter

    //  private lateinit var productionOrderLineList_gl: ArrayList<PurchaseRequestModel.StockTransferLines>
    private var RFPLinesList_gl: ArrayList<RFPResponse.Value> = arrayListOf() // Use nullable type

    // private var value: ArrayList<Value> = arrayListOf()
    //var position: Int? = 0
    lateinit var networkConnection: NetworkConnection
    lateinit var checkNetwoorkConnection: CheckNetwoorkConnection
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
        activityFormBinding = ActivityInventoryOrderLineBinding.inflate(layoutInflater)
        setContentView(activityFormBinding.root)
        activityFormBinding.scanView.visibility = View.GONE
        activityFormBinding.layoutDocDate.visibility = View.VISIBLE
        title = "Form Screen"

//todo initialization...
        networkConnection = NetworkConnection()
        checkNetwoorkConnection = CheckNetwoorkConnection(application)
        materialProgressDialog = MaterialProgressDialog(this@RFPLinesActivity)
        sessionManagement = SessionManagement(this@RFPLinesActivity)

        deleteCache(this)
        supportActionBar?.hide()

        try {
            val poNum = intent.getStringExtra("PO_NO")
            if(poNum?.isNotEmpty() == true){
                loadInvoiceRequestItems(poNum)
            }
        } catch (e: Exception) {
            Toast.makeText(this, e.message, Toast.LENGTH_SHORT).show()
        }
        activityFormBinding.ivLaserCode.setFocusable(true)
        activityFormBinding.ivLaserCode.requestFocus()

        Handler(Looper.getMainLooper()).postDelayed({
            val imm = getSystemService(INPUT_METHOD_SERVICE) as? InputMethodManager
            if (imm != null && currentFocus != null) {
                imm.hideSoftInputFromWindow(currentFocus!!.windowToken, 0)
            }
        }, 200)


        //todo get arguments data...
        /*try {
            val intent = intent
            //   productionOrderLineList_gl = intent.getSerializableExtra("productionLinesList") as ArrayList<PurchaseRequestModel.StockTransferLines>
            RFPLinesList_gl = intent.getSerializableExtra("itemList") as? ArrayList<RFPResponse.Value> ?: ArrayList()

            inventoryItemNew = RFPLinesList_gl!!.get(0);
            //position = intent.extras?.getInt("pos")
            activityFormBinding.tvTitle.text = "Production Order : " + inventoryItemNew!!.AbsoluteEntry
            postingDate = convert_yyyy_MM_dd_T_hh_mm_ss_into_ddMMYYYY(inventoryItemNew!!.PostingDate)
            setAdapter()

        } catch (e: IOException) {
            Log.e(TAG, "onCreate:===> " + e.message)

            e.printStackTrace()
        }*/



        activityFormBinding.ivOnback.setOnClickListener {
            onBackPressed()
        }

        //todo cancel lines...
        activityFormBinding.chipCancel.setOnClickListener {
            onBackPressed()
        }
        activityFormBinding.etDocDate.setOnClickListener {
            GlobalMethods.disableDatesBetweenPoDateAndToday(this@RFPLinesActivity, activityFormBinding.etDocDate, postingDate)
        }


    }

    fun loadInvoiceRequestItems(docNum: String) {
        checkNetwoorkConnection.observe(this) { isConnected ->
            if (isConnected) {
                materialProgressDialog.show()
                var apiConfig = ApiConstantForURL()

                NetworkClients.updateBaseUrlFromConfig(apiConfig)

                QuantityNetworkClient.updateBaseUrlFromConfig(apiConfig)

                val networkClient = QuantityNetworkClient.create(this)
                val bplId = if (Prefs.getString(AppConstants.BPLID, "").isNotEmpty()) Prefs.getString(AppConstants.BPLID, "") else ""
                networkClient.getRFPList(bplId, docNum).apply {
                    enqueue(object : Callback<RFPResponse> {
                        override fun onResponse(
                            call: Call<RFPResponse>,
                            response: Response<RFPResponse>
                        ) {
                            try {
                                if (response.isSuccessful) {

                                    materialProgressDialog.dismiss()
                                    var productionListModel1 = response.body()!!
                                    var productionList_gl = productionListModel1.value

                                    if (!productionList_gl.isNullOrEmpty() && productionList_gl.size > 0) {
                                        RFPLinesList_gl.clear()
                                        RFPLinesList_gl.addAll(productionList_gl)
                                        try {
                                            inventoryItemNew = RFPLinesList_gl!!.get(0);
                                            //position = intent.extras?.getInt("pos")
                                            activityFormBinding.tvTitle.text = "Production Order : " + inventoryItemNew!!.AbsoluteEntry
                                            postingDate = convert_yyyy_MM_dd_T_hh_mm_ss_into_ddMMYYYY(inventoryItemNew!!.PostingDate)
                                            setAdapter()

                                        } catch (e: IOException) {
                                            Log.e(TAG, "onCreate:===> " + e.message)

                                            e.printStackTrace()
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
                                            GlobalMethods.showError(this@RFPLinesActivity, mError.error.message.value)
                                        }
                                        if (mError.error.code == 306 && mError.error.message.value != null) {
                                            GlobalMethods.showError(this@RFPLinesActivity, mError.error.message.value)
                                            val mainIntent = Intent(this@RFPLinesActivity, LoginActivity::class.java)
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

                        override fun onFailure(call: Call<RFPResponse>, t: Throwable) {
                            Log.e("issueCard_failure-----", t.toString())
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


    //todo set adapter....
    fun setAdapter() {

        /* for (i in 0 until productionOrderLineList_gl.size) {
             var j: Int = i
             try {

             } catch (e: IndexOutOfBoundsException) {
                 e.printStackTrace()
             }
         }*/


        activityFormBinding.ivNoDataFound.visibility = View.GONE
        activityFormBinding.rvProductionOrderList.visibility = View.VISIBLE
        activityFormBinding.btnLinearLayout.visibility = View.VISIBLE
        //  productionOrderLineList_gl = setFilteredList(productionOrderLineList_gl) as ArrayList<PurchaseRequestModel.StockTransferLines> /* = java.util.ArrayList<com.soothe.sapApplication.ui.issueForProductionOrder.Model.PurchaseRequestModel.StockTransferLines> */

        val layoutManager: RecyclerView.LayoutManager = LinearLayoutManager(this)
        activityFormBinding.rvProductionOrderList.layoutManager = layoutManager
        //todo parse save button in adapter constructor for click listener on adapter...

        RFPItemAdapter = RFPItemAdapter(
            this@RFPLinesActivity,
            RFPLinesList_gl,
            networkConnection,
            materialProgressDialog,
            this@RFPLinesActivity,
            activityFormBinding.chipSave
        )

        activityFormBinding.rvProductionOrderList.adapter = RFPItemAdapter


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
        response: HashMap<String, ArrayList<ScanedOrderBatchedItems.Value>>,
        listResponse: ArrayList<RFPResponse.Value>,
        quantityResponse: HashMap<String, ArrayList<String>>,
        serialQuantityResponse: java.util.HashMap<String, ArrayList<String>>,
        noneQuantityResponse: java.util.HashMap<String, ArrayList<String>>
    ) {
        Log.e("hashmap--->", quantityResponse.toString())

        activityFormBinding.chipSave.isEnabled = false
        activityFormBinding.chipSave.isCheckable = false

        hashMapBatchList = response
        hashmapBatchQuantityList = quantityResponse
        serialHashMapQuantityList = serialQuantityResponse
        noneHashMapQuantityList = noneQuantityResponse


        postInventorystock(inventoryItemNew!!, listResponse)

    }


    lateinit var batchList: List<ScanedOrderBatchedItems.Value>
    lateinit var batchQuantityList: MutableList<String>


    //todo here save issue for production lines items of order...
    @SuppressLint("SuspiciousIndentation")
    @RequiresApi(Build.VERSION_CODES.O)
    private fun postInventorystock(inventoryItem: RFPResponse.Value, list: MutableList<RFPResponse.Value>) {
        var ii = 0
        val currentDate = LocalDateTime.now()
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")
        val formattedDate = currentDate.format(formatter)
        val docDate = convert_dd_MM_yyyy_into_yyyy_MM_dd(activityFormBinding.etDocDate.text.toString().trim())
        var postedJson = JsonObject()
        postedJson.addProperty("Series", inventoryItem.Series)//552
        postedJson.addProperty("DocDate", docDate)
        postedJson.addProperty("BPL_IDAssignedToInvoice", inventoryItem.BPLID)
        postedJson.addProperty("DocObjectCode", "oInventoryGenEntry")  //inventoryItem.NumAtCard
        postedJson.addProperty("U_Type", inventoryItem.DocType)
        postedJson.addProperty("U_WMSPOST", "Y") //U_WMSPOST tagged (added by Vinod @13Aug,2025)
        postedJson.addProperty("U_WMSUSER", sessionManagement.getUsername(this@RFPLinesActivity))  //WMS userName tagged (added by Vinod @13Aug,2025)

        if (networkConnection.getConnectivityStatusBoolean(applicationContext)) {


            var StockTransferLines = JsonArray()


            for (i in list.indices) {
                /*if (list[i].ItemType != null && list[i].ItemType.equals("BATCH") ){

                }*/
                /*if (list[i].isScanned > 0) {  }*/

                var quantity = 0.000
                quantity = GlobalMethods.sumBatchQuantity(
                    i,
                    hashmapBatchQuantityList.get("Item" + i)!!
                )

                val jsonObject = JsonObject()
                jsonObject.addProperty("BaseEntry", list[i].AbsoluteEntry)
                jsonObject.addProperty("BaseType", "202")
                jsonObject.addProperty("Quantity", list[i].RemainingOpenQuantity)
                jsonObject.addProperty("WarehouseCode", list[i].WarehouseCode)


                /*val stockBin = JsonArray()
                 val batchBin = JsonArray()
                 val serialBin = JsonArray()*/

                batchList = hashMapBatchList.get("Item" + i)!!
                batchQuantityList = hashmapBatchQuantityList.get("Item" + i)!!


                val stockBinMap = mutableMapOf<String, JsonObject>() // For grouping by BinAbsEntry
                val batchBin = JsonArray()
                val serialBin = JsonArray()

                for (j in list[i].binAllocationJSONs.indices) {
                    val current = list[i].binAllocationJSONs[j]

                    // ---------- STOCK BIN (group by BinAbsEntry) ----------
                    val existingStockObj = stockBinMap[current.BinAbsEntry]
                    if (existingStockObj != null) {
                        // Update existing quantity
                        val existingQty = existingStockObj.get("Quantity").asDouble
                        existingStockObj.addProperty(
                            "Quantity",
                            (existingQty + current.Quantity.toDoubleOrNull()!!)
                        )
                    } else {
                        // Create new object
                        val stockObj = JsonObject().apply {
                            addProperty("BinAbsEntry", current.BinAbsEntry)
                            addProperty("Quantity", current.Quantity)
                            addProperty("SerialAndBatchNumbersBaseLine", 0)
                        }
                        stockBinMap[current.BinAbsEntry] = stockObj
                    }

                    // ---------- BATCH / SERIAL ----------
                    if (list[i].ItemType.equals("NONE", true) || list[i].ItemType.equals("BATCH", true)) {
                        val batchObject = JsonObject().apply {
                            addProperty("BatchNumber", current.BinLocation)
                            addProperty("Quantity", current.Quantity)
                            addProperty("ExpiryDate", current.ExpiryDate)
                            addProperty("ManufacturingDate", current.ManufacturingDate)
                        }
                        batchBin.add(batchObject)

                    } else if (list[i].ItemType.equals("SERIAL", true)) {
                        val serialObject = JsonObject().apply {
                            addProperty("InternalSerialNumber", current.BinLocation)
                            addProperty("Quantity", current.Quantity)
                        }
                        serialBin.add(serialObject)
                    }
                }

// After loop: collect all merged stock objects
                val stockBin = JsonArray().apply {
                    stockBinMap.values.forEach { add(it) }
                }

                if (list[i].BinManaged.equals("Y", true)) {
                    jsonObject.add("DocumentLinesBinAllocations", stockBin)
                }
                jsonObject.add("BatchNumbers", batchBin)
                jsonObject.add("serialManual", serialBin)


                //StockTransferLines.add(jsonObject)

                if (list[i].binAllocationJSONs.size > 0 && list[i].BinManaged.equals("Y", true)) {
                    StockTransferLines.add(jsonObject)

                } else if (list.size > 0) {
                    StockTransferLines.add(jsonObject)
                }


                /* else
             {
                 val stockBin = JsonArray()
                 jsonObject.add("DocumentLinesBinAllocations", stockBin)
                // jsonObject.add("serialManual", stockBin)
                 jsonObject.add("BatchNumbers", stockBin)
                 StockTransferLines.add(jsonObject)
                 Log.e(" OUT EL At=>"+i,list[i].binAllocationJSONs.size.toString())
             }*/


            }

            postedJson.add("DocumentLines", StockTransferLines)

            Log.e("success--PayLoad==>", "==>" + postedJson.toString())

            /*  var apiConfig = ApiConstantForURL()

           NetworkClients.updateBaseUrlFromConfig(apiConfig)

           QuantityNetworkClient.updateBaseUrlFromConfig(apiConfig)*/

            if (false)
                return

            materialProgressDialog.show()
            var apiConfig = ApiConstantForURL()

            NetworkClients.updateBaseUrlFromConfig(apiConfig)

            QuantityNetworkClient.updateBaseUrlFromConfig(apiConfig)


            val networkClient = NetworkClients.create(this@RFPLinesActivity)
            networkClient.RFP_Posting(postedJson).apply {
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
                                Log.e("success------", "Successful!")
                                Log.d(
                                    "Doc_Num",
                                    "onResponse: " + response.body()!!.DocNum.toString()
                                )
                                //GlobalMethods.showSuccess(this@RFPLinesActivity, "Post Successfully. " + response.body()!!.DocNum.toString())
                                showSuccessDialog(
                                    context = this@RFPLinesActivity,
                                    title = "Receipt from Production",
                                    successMsg = "Receipt from production order post Successfully with docnum ",
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
                                            this@RFPLinesActivity,
                                            mError.error.message.value
                                        )
                                    }
                                    if (mError.error.message.value != null) {
                                        GlobalMethods.showError(
                                            this@RFPLinesActivity,
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
            materialProgressDialog.dismiss()
            handler.post {
                Toast.makeText(
                    this@RFPLinesActivity,
                    "No Network Connection",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
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

                val networkClient = NetworkClients.create(this@RFPLinesActivity)
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
                                            this@RFPLinesActivity,
                                            responseModel.value[0].WarehouseCode
                                        )
                                        setAdapter()
//                                        Toast.makeText(this@InventoryTransferLinesActivity, BPLIDNum.toString(), Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(
                                            this@RFPLinesActivity,
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
                                                this@RFPLinesActivity,
                                                mError.error.message.value
                                            )
                                        }
                                        if (mError.error.message.value != null) {
                                            GlobalMethods.showError(
                                                this@RFPLinesActivity,
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
            RFPItemAdapter.onActivityResult(requestCode, resultCode, data)
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