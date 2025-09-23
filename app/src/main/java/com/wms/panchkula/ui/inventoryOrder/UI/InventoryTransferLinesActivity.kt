package  com.wms.panchkula.ui.invoiceOrder.UI

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.*
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.wms.panchkula.Global_Classes.AppConstants
import com.wms.panchkula.Global_Classes.GlobalMethods
import com.wms.panchkula.Global_Classes.MaterialProgressDialog
import com.wms.panchkula.Global_Notification.NetworkConnection
import com.wms.panchkula.Model.OtpErrorModel
import com.wms.panchkula.Retrofit_Api.NetworkClients
import com.wms.panchkula.Retrofit_Api.QuantityNetworkClient
import com.wms.panchkula.SessionManagement.SessionManagement
import com.wms.panchkula.databinding.ActivityInventoryOrderLineBinding
import com.wms.panchkula.interfaces.PassList
import com.wms.panchkula.Retrofit_Api.ApiConstantForURL
import com.wms.panchkula.ui.inventoryOrder.Model.InventoryPostResponse
import com.wms.panchkula.ui.inventoryOrder.Model.InventoryRequestModel
import com.wms.panchkula.ui.inventoryOrder.adapter.InventoryTransferItemAdapter_ITR_GRPO
import com.wms.panchkula.ui.issueForProductionOrder.Model.ScanedOrderBatchedItems
import com.wms.panchkula.ui.issueForProductionOrder.Model.WarehouseBPL_IDModel
import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.pixplicity.easyprefs.library.Prefs
import com.wms.panchkula.Global_Classes.GlobalMethods.showSuccessDialog
import com.wms.panchkula.Global_Classes.GlobalMethods.toEditable
import com.wms.panchkula.Model.ModelSeries
import com.wms.panchkula.ui.purchase.model.PurchaseRequestModel
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.io.IOException
import java.lang.reflect.InvocationTargetException
import java.net.SocketTimeoutException
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException
import java.sql.Statement
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter


const val TAG = "InventoryTransferLinesA"

class InventoryTransferLinesActivity : AppCompatActivity(), PassList, InventoryTransferItemAdapter_ITR_GRPO.AdapterCallback {

    var inventoryItem: InventoryRequestModel.Value? = null

    private lateinit var activityFormBinding: ActivityInventoryOrderLineBinding

    //    private lateinit var InventoryTransferItemAdapter: DemoAdapter
    private lateinit var InventoryTransferItemAdapter: InventoryTransferItemAdapter_ITR_GRPO
    private lateinit var productionOrderLineList_gl: ArrayList<InventoryRequestModel.StockTransferLines>
    private var selectedSeriesName = ""
    private var selectedSeries = ""

    var position: Int? = 0
    lateinit var networkConnection: NetworkConnection
    lateinit var materialProgressDialog: MaterialProgressDialog
    private lateinit var sessionManagement: SessionManagement
    private var BPLIDNum = 0
    private var valueList: List<InventoryRequestModel.StockTransferLines> = ArrayList()
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
        title = "Form Screen"
        try {
            inventoryItem = intent.getSerializableExtra("inventReqModel") as InventoryRequestModel.Value
        } catch (e: Exception) {
            Toast.makeText(this, e.message, Toast.LENGTH_SHORT).show()
        }

        deleteCache(this)
        activityFormBinding.layoutQtySeries.visibility = View.VISIBLE
        supportActionBar?.hide()

        Log.d("checking", "Working Tarun")

        activityFormBinding.ivLaserCode.setFocusable(true)
        activityFormBinding.ivLaserCode.requestFocus()

        getDocSeries()

        Handler(Looper.getMainLooper()).postDelayed({
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
            if (imm != null && currentFocus != null) {
                imm.hideSoftInputFromWindow(currentFocus!!.windowToken, 0)
            }
        }, 200)


        //todo initialization...
        networkConnection = NetworkConnection()
        materialProgressDialog = MaterialProgressDialog(this@InventoryTransferLinesActivity)
        sessionManagement = SessionManagement(this@InventoryTransferLinesActivity)


        //todo get arguments data...
        try {
            val intent = intent
            productionOrderLineList_gl = intent.getSerializableExtra("productionLinesList") as ArrayList<InventoryRequestModel.StockTransferLines>

            position = intent.extras?.getInt("pos")
            activityFormBinding.tvTitle.text = "Request No : " + inventoryItem!!.DocNum
            activityFormBinding.edBatchCodeScan.text = inventoryItem!!.NumAtCard.toEditable()

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

    private fun getDocSeries() {
        val apiConfig = ApiConstantForURL()
        QuantityNetworkClient.updateBaseUrlFromConfig(apiConfig)
        val networkClient = QuantityNetworkClient.create(this)
        val bplId = if (Prefs.getString(AppConstants.BPLID, "").isNotEmpty()) Prefs.getString(AppConstants.BPLID, "") else ""
        networkClient.getDocSeries(bplId, "67").apply {
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
                                    GlobalMethods.showError(this@InventoryTransferLinesActivity, mError.error.message.value)
                                }
                                if (mError.error.message.value != null) {
                                    GlobalMethods.showError(this@InventoryTransferLinesActivity, mError.error.message.value)
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
                            GlobalMethods.showError(this@InventoryTransferLinesActivity, "Connection timed out. Please try again.")
                        }

                        is IOException -> {
                            GlobalMethods.showError(this@InventoryTransferLinesActivity, "Network error. Please check your internet connection.")
                        }

                        else -> {
                            GlobalMethods.showError(this@InventoryTransferLinesActivity, "Something went wrong: ${t.localizedMessage}")
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
            this@InventoryTransferLinesActivity,
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
        productionOrderLineList_gl = setFilteredList(productionOrderLineList_gl) as ArrayList<InventoryRequestModel.StockTransferLines>
        /* = java.util.ArrayList<com.soothe.sapApplication.ui.issueForProductionOrder.Model.InventoryRequestModel.StockTransferLines> */

        val layoutManager: RecyclerView.LayoutManager = LinearLayoutManager(this)
        activityFormBinding.rvProductionOrderList.layoutManager = layoutManager
        //todo parse save button in adapter constructor for click listener on adapter...

        InventoryTransferItemAdapter = InventoryTransferItemAdapter_ITR_GRPO(
            this@InventoryTransferLinesActivity,
            productionOrderLineList_gl,
            networkConnection,
            materialProgressDialog,
            this@InventoryTransferLinesActivity,
            activityFormBinding.chipSave
        )

        activityFormBinding.rvProductionOrderList.adapter = InventoryTransferItemAdapter


    }

    var productionOrderLineList_temp: MutableList<InventoryRequestModel.StockTransferLines> = mutableListOf()

    private fun setFilteredList(arrayList: java.util.ArrayList<InventoryRequestModel.StockTransferLines>): MutableList<InventoryRequestModel.StockTransferLines> {

        var position = -1
        for ((index, item) in arrayList.withIndex()) {
            if (item is InventoryRequestModel.StockTransferLines && item.RemainingOpenQuantity.toDouble() > 0 && item.RemainingOpenQuantity != "0.0") {
                productionOrderLineList_temp.add(item)
            }


        }
        return productionOrderLineList_temp

    }

    //todo override function for save items posting
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onApiResponseStock(
        response: HashMap<String, ArrayList<ScanedOrderBatchedItems.Value>>, listResponse: ArrayList<InventoryRequestModel.StockTransferLines>,
        quantityResponse: HashMap<String, ArrayList<String>>, serialQuantityResponse: java.util.HashMap<String, ArrayList<String>>, noneQuantityResponse: java.util.HashMap<String, ArrayList<String>>
    ) {
        Log.e("hashmap--->", quantityResponse.toString())
        Log.e("hashmap--->", "ItemData==>" + listResponse.toString())



        hashMapBatchList = response
        hashmapBatchQuantityList = quantityResponse
        serialHashMapQuantityList = serialQuantityResponse
        noneHashMapQuantityList = noneQuantityResponse
        Log.e("hashmap--->", hashMapBatchList.toString())
        Log.e("batchQuantityList-->", hashmapBatchQuantityList.toString())
        Log.e("serialQtyList1234-->", serialHashMapQuantityList.toString())
        Log.e("noneQtyList1234-->", noneHashMapQuantityList.toString())

        postInventorystock(inventoryItem!!, listResponse)

    }


    lateinit var batchList: List<ScanedOrderBatchedItems.Value>
    lateinit var batchQuantityList: MutableList<String>
    /*private fun getJsonArray(list: ArrayList<PurchaseRequestModel.binAllocationJSONs>,batcharraySize: Int): JsonArray {

        val stockBin = JsonArray()
        var baseLine =0
        if(batcharraySize==0){
            baseLine = -1
        }else{
            baseLine = 0
        }
        if (list!=null||!list.isEmpty()){
        for (i in list.indices) {


            var fromObject = JsonObject()
            if (!list.get(i).BinAbsEntry.isEmpty()) {
                fromObject.addProperty("BinAbsEntry", list.get(i).BinAbsEntry)
                fromObject.addProperty("Quantity", list.get(i).Quantity)
                fromObject.addProperty("BinActionType", "batFromWarehouse",)
                fromObject.addProperty("SerialAndBatchNumbersBaseLine", baseLine)
                stockBin.add(fromObject)
            }




            var ToObject = JsonObject()

            //ToObject.addProperty("BinAbsEntry",list.get(i).ToBinAbsEntry) //previous
            ToObject.addProperty("BinAbsEntry",list.get(i).BatchNum)
            ToObject.addProperty("Quantity",list.get(i).Quantity)
            ToObject.addProperty("BinActionType",    "batToWarehouse",)
            ToObject.addProperty("SerialAndBatchNumbersBaseLine",baseLine)

            stockBin.add(ToObject)
        }}

        return stockBin
    }*/

    private fun getJsonArray(
        list: ArrayList<PurchaseRequestModel.binAllocationJSONs>,
        batcharraySize: Int
    ): JsonArray {
        val stockBin = JsonArray()
        val baseLine = if (batcharraySize == 0) -1 else 0

        if (!list.isNullOrEmpty()) {

            // --- FROM OBJECT LOGIC: Group by BinAbsEntry ---
            val fromGrouped = list
                .filter { !it.BinAbsEntry.isNullOrEmpty() }
                .groupBy { it.BinAbsEntry }
                .mapValues { entry ->
                    entry.value.sumOf { it.Quantity.toDoubleOrNull() ?: 0.0 }
                }

            for ((binAbsEntry, totalQty) in fromGrouped) {
                val fromObject = JsonObject().apply {
                    addProperty("BinAbsEntry", binAbsEntry)
                    addProperty("Quantity", totalQty.toString())
                    addProperty("BinActionType", "batFromWarehouse")
                    addProperty("SerialAndBatchNumbersBaseLine", baseLine)
                }
                stockBin.add(fromObject)
            }

            // --- TO OBJECT LOGIC: Group by BatchNum (equivalent to BinAbsEntry) ---
            val toGrouped = list
                .filter { !it.BatchNum.isNullOrEmpty() }
                .groupBy { it.BatchNum }
                .mapValues { entry ->
                    entry.value.sumOf { it.Quantity.toDoubleOrNull() ?: 0.0 }
                }

            for ((batchNum, totalQty) in toGrouped) {
                val toObject = JsonObject().apply {
                    addProperty("BinAbsEntry", batchNum)
                    addProperty("Quantity", totalQty.toString())
                    addProperty("BinActionType", "batToWarehouse")
                    addProperty("SerialAndBatchNumbersBaseLine", baseLine)
                }
                stockBin.add(toObject)
            }
        }

        return stockBin
    }


    //todo here save issue for production lines items of order...
    @RequiresApi(Build.VERSION_CODES.O)
    private fun postInventorystock(inventoryItem: InventoryRequestModel.Value, list: MutableList<InventoryRequestModel.StockTransferLines>) {
        var ii = 0
        val currentDate = LocalDateTime.now()
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")
        val formattedDate = currentDate.format(formatter)
        if (selectedSeries.isNullOrEmpty()){
            GlobalMethods.showError(this@InventoryTransferLinesActivity, "Please select document series.")
            return
        }
        var postedJson = JsonObject()
        postedJson.addProperty("Series", selectedSeries)//552
        postedJson.addProperty("DocDate", formattedDate)
        postedJson.addProperty("DueDate", inventoryItem.DueDate)
        postedJson.addProperty("CardCode", inventoryItem.CardCode)
        postedJson.addProperty("Comments", inventoryItem.Comments)
        postedJson.addProperty("FromWarehouse", inventoryItem.FromWarehouse)
        postedJson.addProperty("TaxDate", inventoryItem.TaxDate)
        postedJson.addProperty("DocObjectCode", "67")
        postedJson.addProperty("BPLID", inventoryItem.BPLID)
        postedJson.addProperty("U_Type", inventoryItem.DocType)
        postedJson.addProperty("U_WMSPOST", "Y")
        postedJson.addProperty("U_WMSUSER", sessionManagement.getUsername(this@InventoryTransferLinesActivity))  //WMS userName tagged (added by Vinod @25Apr,2025)

        if (networkConnection.getConnectivityStatusBoolean(applicationContext)) {

            var StockTransferLines = JsonArray()


            for (i in list.indices) {
                if (list[i].Batch != null && list[i].Batch.equals("Y")) {
                    if (list[i].isScanned > 0) {

                        var quantity = 0.000
                        quantity = GlobalMethods.sumBatchQuantity(i, hashmapBatchQuantityList.get("Item" + i)!!)

                        val jsonObject = JsonObject()
                        jsonObject.addProperty("BaseEntry", list[i].DocEntry)
                        jsonObject.addProperty("BaseLine", list[i].LineNum)
                        //jsonObject.addProperty("BaseType",        list[i].BaseType)
                        jsonObject.addProperty("FromWarehouseCode", list[i].FromWarehouseCode)
                        jsonObject.addProperty("Price", list[i].Price)
                        jsonObject.addProperty("Quantity", quantity)//list[i].totakPktQty
                        jsonObject.addProperty("UnitPrice", list[i].UnitPrice)
                        jsonObject.addProperty("U_Size", list[i].Size)

                        jsonObject.addProperty("BaseType", "PurchaseDeliveryNotes")


                        Log.e("isScanned==>", "" + list[i].isScanned)

                        val stockBatch = JsonArray()


                        batchList = hashMapBatchList.get("Item" + i)!!
                        batchQuantityList = hashmapBatchQuantityList.get("Item" + i)!!
                        var iii = 0;
                        if (list[i].isScanned > 0) {

                            for (i in batchList.indices) {
                                for (j in i until batchQuantityList.size) {
                                    var jsonLinesObject = JsonObject()

                                    jsonLinesObject.addProperty("BatchNumber", batchList[i].Batch)
                                    jsonLinesObject.addProperty("SystemSerialNumber", batchList[i].SystemNumber)
                                    jsonLinesObject.addProperty("Quantity", batchQuantityList[j])
                                    iii++;
                                    stockBatch.add(jsonLinesObject)
                                    break
                                }
                            }

                            Log.d("binAllocationJSONs", "binAllocationJSONs:=> ${list[i].binAllocationJSONs}")
                            if (list[i].binAllocationJSONs.isNullOrEmpty()) {
                                GlobalMethods.showError(this@InventoryTransferLinesActivity, "\"Please select a bin location before proceeding with batch item selection.")
                                return
                            }
                            jsonObject.add("BatchNumbers", stockBatch)
                            jsonObject.addProperty("WarehouseCode", list[i].binAllocationJSONs[0].WarehouseCode)
                            postedJson.addProperty("ToWarehouse", list[i].binAllocationJSONs[0].WarehouseCode)

                            jsonObject.add("StockTransferLinesBinAllocations", getJsonArray(list[i].binAllocationJSONs, batchList.size))

                            if (batchList.size > 0)
                                StockTransferLines.add(jsonObject)

                        }

                    }

                } else if (list[i].Serial != null && list[i].Serial.equals("Y")) {
                    if (list[i].isScanned > 0) {

                        var quantity = 0.000
                        quantity = GlobalMethods.sumBatchQuantity(i, serialHashMapQuantityList.get("Item" + i)!!)

                        val jsonObject = JsonObject()
                        jsonObject.addProperty("BaseEntry", list[i].DocEntry)
                        jsonObject.addProperty("BaseLine", list[i].LineNum)
                        jsonObject.addProperty("BaseType", list[i].BaseType)
                        jsonObject.addProperty("FromWarehouseCode", list[i].FromWarehouseCode)
                        jsonObject.addProperty("Price", list[i].Price)
                        jsonObject.addProperty("Quantity", quantity)//list[i].totakPktQty
                        jsonObject.addProperty("UnitPrice", list[i].UnitPrice)

                        jsonObject.addProperty("BaseType", "PurchaseDeliveryNotes")


                        Log.e("isScanned==>", "" + list[i].isScanned)

                        val SerialNumbersArray = JsonArray()

                        batchList = hashMapBatchList.get("Item" + i)!!
//                        batchQuantityList = serialHashMapQuantityList.get("Item" + i)!!
                        batchQuantityList = serialHashMapQuantityList.get("Item" + i)!!

                        if (list[i].isScanned > 0) {

                            for (i in batchList.indices) {
                                for (j in i until batchQuantityList.size) {
                                    var jsonLinesObject = JsonObject()

                                    jsonLinesObject.addProperty("SystemSerialNumber", batchList[i].SystemNumber)
                                    jsonLinesObject.addProperty("InternalSerialNumber", batchList[i].SerialNumber)
                                    jsonLinesObject.addProperty("Quantity", "1")

                                    SerialNumbersArray.add(jsonLinesObject)
                                    break
                                }
                            }

                            jsonObject.add("SerialNumbers", SerialNumbersArray)
                            if (list[i].binAllocationJSONs.isNullOrEmpty()) {
                                GlobalMethods.showError(this@InventoryTransferLinesActivity, "\"Please select a bin location before proceeding with batch item selection.")
                                return
                            }
                            jsonObject.addProperty("WarehouseCode", list[i].binAllocationJSONs[0].WarehouseCode)
                            postedJson.addProperty("ToWarehouse", list[i].binAllocationJSONs[0].WarehouseCode)

                            jsonObject.add("StockTransferLinesBinAllocations", getJsonArray(list[i].binAllocationJSONs, SerialNumbersArray.size()))

                            if (batchList.size > 0)
                                StockTransferLines.add(jsonObject)

                        }

                    }

                } else if (list[i].None != null && list[i].None.equals("Y")) {

                    if (list[i].isScanned > 0) {

                        var quantity = 0.000

                        quantity = GlobalMethods.sumBatchQuantity(i, noneHashMapQuantityList.get("Item" + i)!!)

                        val jsonObject = JsonObject()
                        jsonObject.addProperty("BaseEntry", list[i].DocEntry)
                        jsonObject.addProperty("BaseLine", list[i].LineNum)
                        jsonObject.addProperty("BaseType", list[i].BaseType)
                        jsonObject.addProperty("FromWarehouseCode", list[i].FromWarehouseCode)
                        jsonObject.addProperty("Price", list[i].Price)
                        jsonObject.addProperty("Quantity", quantity)//list[i].totakPktQty
                        jsonObject.addProperty("WarehouseCode", list[i].WarehouseCode)
                        jsonObject.addProperty("BaseType", "PurchaseDeliveryNotes")
                        if (list[i].binAllocationJSONs.isNullOrEmpty()) {
                            GlobalMethods.showError(this@InventoryTransferLinesActivity, "\"Please select a bin location before proceeding with batch item selection.")
                            return
                        }
                        jsonObject.addProperty("WarehouseCode", list[i].binAllocationJSONs[0].WarehouseCode)
                        postedJson.addProperty("ToWarehouse", list[i].binAllocationJSONs[0].WarehouseCode)

                        jsonObject.add("StockTransferLinesBinAllocations", getJsonArray(list[i].binAllocationJSONs, 3))


                        StockTransferLines.add(jsonObject)


                    }

                }

            }

            postedJson.add("StockTransferLines", StockTransferLines)
            Log.e("success--PayLoad==>", "==>" + postedJson.toString())
            materialProgressDialog.show()
            activityFormBinding.chipSave.isEnabled = false
            activityFormBinding.chipSave.isCheckable = false

            var apiConfig = ApiConstantForURL()

            NetworkClients.updateBaseUrlFromConfig(apiConfig)

            QuantityNetworkClient.updateBaseUrlFromConfig(apiConfig)

            val networkClient = NetworkClients.create(this@InventoryTransferLinesActivity)
            networkClient.dostockTransfer(postedJson).apply {
                enqueue(object : Callback<InventoryPostResponse> {
                    override fun onResponse(
                        call: Call<InventoryPostResponse>,
                        response: Response<InventoryPostResponse>
                    ) {
                        try {
                            activityFormBinding.chipSave.isEnabled = true
                            activityFormBinding.chipSave.isCheckable = true

                            AppConstants.IS_SCAN = false
                            materialProgressDialog.dismiss()
                            Log.e("success---BP---", "==>" + response.code())
                            if (response.code() == 201 || response.code() == 200) {
                                Log.e("success------", "Successful!")
                                Log.d("Doc_Num", "onResponse: " + response.body()!!.DocNum.toString())
                                //GlobalMethods.showSuccess(this@InventoryTransferLinesActivity, "Post Successfully. " + response.body()!!.DocNum.toString())
                                showSuccessDialog(
                                    context = this@InventoryTransferLinesActivity,
                                    title = "Inventory Transfer GRPO",
                                    successMsg = "Inventory transfer GRPO post successfully with docnum ",
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
                                            this@InventoryTransferLinesActivity,
                                            mError.error.message.value
                                        )
                                    }
                                    if (mError.error.message.value != null) {
                                        GlobalMethods.showError(
                                            this@InventoryTransferLinesActivity,
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

                    override fun onFailure(call: Call<InventoryPostResponse>, t: Throwable) {
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
                    this@InventoryTransferLinesActivity,
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

                val networkClient = NetworkClients.create(this@InventoryTransferLinesActivity)
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
                                            this@InventoryTransferLinesActivity,
                                            responseModel.value[0].WarehouseCode
                                        )
                                        setAdapter()
//                                        Toast.makeText(this@InventoryTransferLinesActivity, BPLIDNum.toString(), Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(
                                            this@InventoryTransferLinesActivity,
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
                                                this@InventoryTransferLinesActivity,
                                                mError.error.message.value
                                            )
                                        }
                                        if (mError.error.message.value != null) {
                                            GlobalMethods.showError(
                                                this@InventoryTransferLinesActivity,
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
            Log.e("Result==>B", data?.getStringExtra("batch_code").toString())
            InventoryTransferItemAdapter.onActivityResult(requestCode, resultCode, data)
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
        val url =
            "jdbc:jtds:sqlserver://" + AppConstants.IP + ":" + AppConstants.PORT + "/" + AppConstants.COMPANY_DB
        ActivityCompat.requestPermissions(
            this as Activity,
            arrayOf<String>(Manifest.permission.INTERNET),
            PackageManager.PERMISSION_GRANTED
        )
        val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)
        try {
            Class.forName(AppConstants.Classes)
            connection =
                DriverManager.getConnection(url, AppConstants.USERNAME, AppConstants.PASSWORD)
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