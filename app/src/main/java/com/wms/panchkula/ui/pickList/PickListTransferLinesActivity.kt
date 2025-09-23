package com.wms.panchkula.ui.pickList

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.*
import android.util.Log
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
import com.wms.panchkula.Global_Classes.AppConstants
import com.wms.panchkula.Global_Classes.GlobalMethods
import com.wms.panchkula.Global_Classes.MaterialProgressDialog
import com.wms.panchkula.Global_Notification.NetworkConnection
import com.wms.panchkula.Model.OtpErrorModel
import com.wms.panchkula.SessionManagement.SessionManagement
import com.wms.panchkula.interfaces.PassList
import com.wms.panchkula.ui.invoiceOrder.UI.TAG
import com.wms.panchkula.ui.issueForProductionOrder.Model.ScanedOrderBatchedItems
import com.wms.panchkula.ui.issueForProductionOrder.Model.WarehouseBPL_IDModel
import com.wms.panchkula.ui.pickList.model.PickListsResponse
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


class PickListTransferLinesActivity : AppCompatActivity(), PassList,
    PickListTransferItemAdapter.AdapterCallback {
    companion object {
        // Declare the static variable
        var staticString: String = ""
    }

    var inventoryItem: PurchaseRequestModel.Value? = null

    private lateinit var activityFormBinding: ActivityInventoryOrderLineBinding

    //    private lateinit var PurchaseTransferItemAdapter: DemoAdapter
    private lateinit var PurchaseTransferItemAdapter: PickListTransferItemAdapter
    private lateinit var productionOrderLineList_gl: ArrayList<PurchaseRequestModel.StockTransferLines>

    var position: Int? = 0
    lateinit var networkConnection: NetworkConnection
    lateinit var materialProgressDialog: MaterialProgressDialog
    private lateinit var sessionManagement: SessionManagement
    private var BPLIDNum = 0
    private var valueList: List<PurchaseRequestModel.StockTransferLines> = ArrayList()
    private var connection: Connection? = null
    var openQty = 0.0

    //todo batch scan and quantity list interface override...
    var hashMapBatchList: HashMap<String, ArrayList<ScanedOrderBatchedItems.Value>> =
        HashMap<String, ArrayList<ScanedOrderBatchedItems.Value>>()
    var hashmapBatchQuantityList: HashMap<String, ArrayList<String>> =
        HashMap<String, ArrayList<String>>()
    var serialHashMapQuantityList: java.util.HashMap<String, ArrayList<String>> =
        java.util.HashMap<String, ArrayList<String>>()
    var noneHashMapQuantityList: java.util.HashMap<String, ArrayList<String>> =
        java.util.HashMap<String, ArrayList<String>>()


    @RequiresApi(33)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activityFormBinding = ActivityInventoryOrderLineBinding.inflate(layoutInflater)
        setContentView(activityFormBinding.root)
        activityFormBinding.scanView.visibility = View.VISIBLE
        title = "Form Screen"
        try {
            inventoryItem =
                intent.getSerializableExtra("inventReqModel") as PurchaseRequestModel.Value
        } catch (e: Exception) {
            Toast.makeText(this, e.message, Toast.LENGTH_SHORT).show()
        }

        deleteCache(this)

        supportActionBar?.hide()

        Log.d("checking", "Working Tarun")

        activityFormBinding.ivLaserCode.setFocusable(true)
        activityFormBinding.ivLaserCode.requestFocus()

        Handler(Looper.getMainLooper()).postDelayed({
            val imm = getSystemService(INPUT_METHOD_SERVICE) as? InputMethodManager
            if (imm != null && currentFocus != null) {
                imm.hideSoftInputFromWindow(currentFocus!!.windowToken, 0)
            }
        }, 200)


        //todo initialization...
        networkConnection = NetworkConnection()
        materialProgressDialog = MaterialProgressDialog(this@PickListTransferLinesActivity)
        sessionManagement = SessionManagement(this@PickListTransferLinesActivity)


        //todo get arguments data...
        try {
            val intent = intent
            productionOrderLineList_gl =
                intent.getSerializableExtra("productionLinesList") as ArrayList<PurchaseRequestModel.StockTransferLines>

            position = intent.extras?.getInt("pos")
            activityFormBinding.tvTitle.text = "Sales Order : " + inventoryItem!!.DocNum
            Log.e(TAG, "onCreate:===> " + productionOrderLineList_gl.size)

            setAdapter();

        } catch (e: IOException) {
            Log.e(TAG, "onCreate:===> " + e.message)

            e.printStackTrace()
        }



        activityFormBinding.ivOnback.setOnClickListener {
            onBackPressed()
        }


        //todo cancel lines...
        activityFormBinding.chipCancel.setOnClickListener {
            onBackPressed()
        }


    }


    //todo set adapter....
    fun setAdapter() {

        for (i in 0 until productionOrderLineList_gl.size) {
            var j: Int = i
            try {

            } catch (e: IndexOutOfBoundsException) {
                e.printStackTrace()
            }
        }


        activityFormBinding.ivNoDataFound.visibility = View.GONE
        activityFormBinding.rvProductionOrderList.visibility = View.VISIBLE
        activityFormBinding.btnLinearLayout.visibility = View.VISIBLE
        //  productionOrderLineList_gl = setFilteredList(productionOrderLineList_gl) as ArrayList<PurchaseRequestModel.StockTransferLines> /* = java.util.ArrayList<com.soothe.sapApplication.ui.issueForProductionOrder.Model.PurchaseRequestModel.StockTransferLines> */

        val layoutManager: RecyclerView.LayoutManager = LinearLayoutManager(this)
        activityFormBinding.rvProductionOrderList.layoutManager = layoutManager
        //todo parse save button in adapter constructor for click listener on adapter...

        PurchaseTransferItemAdapter = PickListTransferItemAdapter(
            this@PickListTransferLinesActivity,
            productionOrderLineList_gl,
            networkConnection,
            materialProgressDialog,
            this@PickListTransferLinesActivity,
            activityFormBinding.chipSave
        )

        activityFormBinding.rvProductionOrderList.adapter = PurchaseTransferItemAdapter


    }

    var productionOrderLineList_temp: MutableList<PurchaseRequestModel.StockTransferLines> =
        mutableListOf()

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
        listResponse: ArrayList<PurchaseRequestModel.StockTransferLines>,
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


        postInventorystock(inventoryItem!!, listResponse)

    }

    lateinit var batchList: List<ScanedOrderBatchedItems.Value>
    lateinit var batchQuantityList: MutableList<String>


    //todo here save issue for production lines items of order...
    @SuppressLint("SuspiciousIndentation")
    @RequiresApi(Build.VERSION_CODES.O)
    private fun postInventorystock(
        inventoryItem: PurchaseRequestModel.Value,
        list: MutableList<PurchaseRequestModel.StockTransferLines>
    ) {
        var ii = 0
        val currentDate = LocalDateTime.now()
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")
        val formattedDate = currentDate.format(formatter)
        var postedJson = JsonObject()

        postedJson.addProperty("ObjectType", "156")
        postedJson.addProperty("PickDate", formattedDate)


/*
            postedJson.addProperty("Series", inventoryItem.Series)//552
            postedJson.addProperty("DocDate", formattedDate)
            postedJson.addProperty("DocDueDate", inventoryItem.DocDueDate)
            postedJson.addProperty("TaxDate", inventoryItem.TaxDate)
            postedJson.addProperty("CardCode", inventoryItem.CardCode)
            postedJson.addProperty("BPL_IDAssignedToInvoice", inventoryItem.BPLID)
            postedJson.addProperty("NumAtCard", activityFormBinding.edBatchCodeScan.text.toString())  //inventoryItem.NumAtCard
            postedJson.addProperty("DocObjectCode", "oInvoices")  //inventoryItem.NumAtCard
            postedJson.addProperty("DocumentSubType", "bod_GSTTaxInvoice")  //inventoryItem.NumAtCard
            postedJson.addProperty("DocType", "dDocument_Items")  //inventoryItem.NumAtCard

            postedJson.addProperty("PayToCode", inventoryItem.PayToCode)
            postedJson.addProperty("ShipToCode", inventoryItem.ShipToCode)*/














        if (networkConnection.getConnectivityStatusBoolean(applicationContext)) {
            materialProgressDialog.show()

            var StockTransferLines = JsonArray()


            Log.d("sdjnjkcnb", "postInventorystock: ${list.size}")
            for (i in list.indices) {

                /*if (list[i].ItemType != null && list[i].ItemType.equals("BATCH") ){
                }*/
                /*if (list[i].isScanned > 0) {  }*/

                var quantity = 0.000
                quantity = GlobalMethods.sumBatchQuantity(
                    i, hashmapBatchQuantityList.get("Item" + i)!!
                )

                val jsonObject = JsonObject()

                jsonObject.addProperty("BaseObjectType", "17")
                jsonObject.addProperty("OrderEntry", list[i].DocEntry)
                jsonObject.addProperty("OrderRowID", list[i].LineNum)
                jsonObject.addProperty("ReleasedQuantity", list[i].RemainingOpenQuantity)


      /*          jsonObject.addProperty("BaseEntry", list[i].DocEntry)
                jsonObject.addProperty("LineNum", list[i].LineNum)
                jsonObject.addProperty("Price", list[i].Price)
                jsonObject.addProperty("Quantity", list[i].RemainingOpenQuantity)
                jsonObject.addProperty("ItemCode", list[i].ItemCode)
                jsonObject.addProperty("WarehouseCode", list[i].WarehouseCode)
                jsonObject.addProperty("BaseLine", list[i].LineNum)
                jsonObject.addProperty("BaseType", "17")
                jsonObject.addProperty("TaxCode", list[i].TaxCode)*/


                val stockBin = JsonArray()
                val batchBin = JsonArray()
                val serialBin = JsonArray()

                batchList = hashMapBatchList.get("Item" + i)!!
                batchQuantityList = hashmapBatchQuantityList.get("Item" + i)!!

                if (list[i].BinManaged.equals("Y", true)) {

                    for (j in list[i].binAllocationJSONs.indices) {
                        var jsonLinesObject = JsonObject()
                        var batchObject = JsonObject()
                        var serialObject = JsonObject()


//                        commit by tarun
                        jsonLinesObject.addProperty(
                            "BinAbsEntry", list[i].binAllocationJSONs[j].BinAbsEntry
                        )
                        jsonLinesObject.addProperty(
                            "Quantity", list[i].binAllocationJSONs[j].Quantity
                        )
                        jsonLinesObject.addProperty("SerialAndBatchNumbersBaseLine", i)

                        stockBin.add(jsonLinesObject)
                        Log.e("ItemType T=>", list[i].ItemType)

                        if (list[i].ItemType.equals("BATCH", true)) {
                            Log.e("ItemType BATCH=>", list[i].binAllocationJSONs[j].BinLocation)
                            batchObject.addProperty(
                                "BatchNumber", list[i].binAllocationJSONs[j].BinLocation
                            )
                            batchObject.addProperty(
                                "Quantity", list[i].binAllocationJSONs[j].Quantity
                            )

                            batchBin.add(batchObject)

                        } else if (list[i].ItemType.equals("SERIAL", true)) {
                            serialObject.addProperty(
                                "InternalSerialNumber", list[i].binAllocationJSONs[j].BinLocation
                            )
                            serialObject.addProperty(
                                "Quantity", list[i].binAllocationJSONs[j].Quantity
                            )

                            serialBin.add(serialObject)

                        }
                    }


           /*         jsonObject.add("DocumentLinesBinAllocations", stockBin)
                    jsonObject.add("BatchNumbers", batchBin)
                    jsonObject.add("SerialNumbers", serialBin)*/

                    jsonObject.add("DocumentLinesBinAllocations", JsonArray())
                    jsonObject.add("BatchNumbers", JsonArray())
                    jsonObject.add("SerialNumbers", JsonArray())

                    if (list[i].binAllocationJSONs.size > 0 && list[i].BinManaged.equals(
                            "Y", true
                        )
                    ) {
                        StockTransferLines.add(jsonObject)
                        Log.e(" IF At=>" + i, list[i].binAllocationJSONs.size.toString())

                    }

                }
                else {
                    val stockBin = JsonArray()
                    jsonObject.add("DocumentLinesBinAllocations", stockBin)
                    jsonObject.add("SerialNumbers", stockBin)
                    jsonObject.add("BatchNumbers", stockBin)
                    StockTransferLines.add(jsonObject)
                    Log.e(" OUT EL At=>" + i, list[i].binAllocationJSONs.size.toString())
                }


            }

            postedJson.add("PickListsLines", StockTransferLines)

            Log.e("success--PayLoad==>", "==>" + postedJson.toString())

            /*  var apiConfig = ApiConstantForURL()

              NetworkClients.updateBaseUrlFromConfig(apiConfig)

              QuantityNetworkClient.updateBaseUrlFromConfig(apiConfig)*/


            var apiConfig = ApiConstantForURL()

            NetworkClients.updateBaseUrlFromConfig(apiConfig)

            QuantityNetworkClient.updateBaseUrlFromConfig(apiConfig)

/*            val networkClient = QuantityNetworkClient.create(this@PickListTransferLinesActivity)
            networkClient.ARDraft_Posting(postedJson).apply {
                enqueue(object : Callback<PurchasePostResponse> {
                    override fun onResponse(
                        call: Call<PurchasePostResponse>, response: Response<PurchasePostResponse>
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
                                    "Doc_Num", "onResponse: " + response.body()!!.DocNum.toString()
                                )
                                GlobalMethods.showSuccess(
                                    this@PickListTransferLinesActivity,
                                    "Post Successfully. " + response.body()!!.DocNum.toString()
                                )
                                onBackPressed()
                            } else {
                                materialProgressDialog.dismiss()
                                val gson1 = GsonBuilder().create()
                                var mError: OtpErrorModel
                                try {
                                    val s = response.errorBody()!!.string()
                                    mError = gson1.fromJson(s, OtpErrorModel::class.java)
                                    if (mError.error.code.equals(400)) {
                                        GlobalMethods.showError(
                                            this@PickListTransferLinesActivity,
                                            mError.error.message.value
                                        )
                                    }
                                    if (mError.error.message.value != null) {
                                        GlobalMethods.showError(
                                            this@PickListTransferLinesActivity,
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
            }*/

            val networkClient = NetworkClients.create(this@PickListTransferLinesActivity)
            networkClient.PickList_Posting(postedJson).apply {

                enqueue(object : Callback<PickListsResponse?> {
                    override fun onResponse(
                        call: Call<PickListsResponse?>,
                        response: Response<PickListsResponse?>
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
                                    "Doc_Num", "onResponse: " + response.body()!!.OwnerCode.toString()
                                )
                                GlobalMethods.showSuccess(
                                    this@PickListTransferLinesActivity,
                                    "Post Successfully. " + response.body()!!.OwnerCode.toString()
                                )
                                onBackPressed()
                            } else {
                                materialProgressDialog.dismiss()
                                val gson1 = GsonBuilder().create()
                                var mError: OtpErrorModel
                                try {
                                    val s = response.errorBody()!!.string()
                                    mError = gson1.fromJson(s, OtpErrorModel::class.java)
                                    if (mError.error.code.equals(400)) {
                                        GlobalMethods.showError(
                                            this@PickListTransferLinesActivity,
                                            mError.error.message.value
                                        )
                                    }
                                    if (mError.error.message.value != null) {
                                        GlobalMethods.showError(
                                            this@PickListTransferLinesActivity,
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

                    override fun onFailure(call: Call<PickListsResponse?>, t: Throwable) {
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
                    this@PickListTransferLinesActivity, "No Network Connection", Toast.LENGTH_SHORT
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

                val networkClient = NetworkClients.create(this@PickListTransferLinesActivity)
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
                                            this@PickListTransferLinesActivity,
                                            responseModel.value[0].WarehouseCode
                                        )
                                        setAdapter()
//                                        Toast.makeText(this@InventoryTransferLinesActivity, BPLIDNum.toString(), Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(
                                            this@PickListTransferLinesActivity,
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
                                                this@PickListTransferLinesActivity,
                                                mError.error.message.value
                                            )
                                        }
                                        if (mError.error.message.value != null) {
                                            GlobalMethods.showError(
                                                this@PickListTransferLinesActivity,
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


}