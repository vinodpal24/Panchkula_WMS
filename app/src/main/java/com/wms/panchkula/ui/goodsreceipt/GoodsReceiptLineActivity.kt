package com.wms.panchkula.ui.goodsreceipt


import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.wms.panchkula.R
import com.wms.panchkula.Retrofit_Api.ApiConstantForURL
import com.wms.panchkula.Retrofit_Api.NetworkClients
import com.wms.panchkula.Retrofit_Api.QuantityNetworkClient
import com.wms.panchkula.Global_Classes.AppConstants
import com.wms.panchkula.Global_Classes.MaterialProgressDialog
import com.wms.panchkula.SessionManagement.SessionManagement
import com.webapp.internetconnection.CheckNetwoorkConnection
import com.wms.panchkula.ui.goodsreceipt.adapter.GoodReceiptAdapter
import com.wms.panchkula.ui.goodsreceipt.model.GetItemstModel
import com.wms.panchkula.databinding.ActivityInventoryOrderLineBinding
import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.pixplicity.easyprefs.library.Prefs
import com.wms.panchkula.Global_Classes.GlobalMethods
import com.wms.panchkula.Global_Classes.GlobalMethods.showSuccessDialog
import com.wms.panchkula.Global_Notification.NetworkConnection
import com.wms.panchkula.Model.OtpErrorModel
import com.wms.panchkula.ui.inventoryOrder.Model.InventoryPostResponse
import com.wms.panchkula.ui.inventoryTransfer.model.Warehouse_BPLID
import com.wms.panchkula.ui.issueForProductionOrder.Model.ScanedOrderBatchedItems
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.IOException
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class GoodsReceiptLineActivity : AppCompatActivity() {

    lateinit var binding: ActivityInventoryOrderLineBinding
    private var requestListModel_gl: ArrayList<GetItemstModel.Value> = ArrayList()
    lateinit var materialProgressDialog: MaterialProgressDialog
    lateinit var checkNetwoorkConnection: CheckNetwoorkConnection
    private var requestAdapter: GoodReceiptAdapter? = null
    private lateinit var sessionManagement: SessionManagement
    private lateinit var networkConnection: NetworkConnection
    val handler = Handler(Looper.getMainLooper())

    var page = 0
    var apicall: Boolean = true
    var isScrollingpage: Boolean = false
    var limit = 100
    var flag: String = ""
    lateinit var layoutManager: RecyclerView.LayoutManager


    companion object {
        private const val TAG = "DemoActivity"
    }

    var docType = ""
    fun docSpinner() {
        binding.docView.visibility = View.GONE
        val warehouseCodes = resources.getStringArray(R.array.doc_type)


        // Set the adapter for the spinner
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, warehouseCodes)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)


        // Apply the adapter to the Spinner
        binding.edDocType.setAdapter(adapter)
        binding.edDocType.setOnItemSelectedListener(object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parentView: AdapterView<*>,
                selectedItemView: View,
                position: Int,
                id: Long
            ) {
                // You can get the selected value with:
                val selectedCode = parentView.getItemAtPosition(position) as String
                docType = selectedCode
                // Do something with the selected item
            }

            override fun onNothingSelected(parentView: AdapterView<*>?) {
                // Handle the case where nothing is selected
            }
        })

    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityInventoryOrderLineBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.hide()
//        todo new code for test good items
//        getDataToPostOnJson()
//        getPostJson()
        binding.scanView.visibility = View.VISIBLE
        binding.qrTitle.setText(" Warehouse : ")
        binding.edBatchCodeScan.setHint("Warehouse")
        materialProgressDialog = MaterialProgressDialog(this@GoodsReceiptLineActivity)
        checkNetwoorkConnection = CheckNetwoorkConnection(application)
        sessionManagement = SessionManagement(this@GoodsReceiptLineActivity)
        networkConnection = NetworkConnection()

        title = "Goods Receipt"

        docSpinner()
        try {
            val intent = intent
            requestListModel_gl = intent.getSerializableExtra("selectedList") as ArrayList<GetItemstModel.Value>

            setInvoiceOrderAdapter()

        } catch (e: IOException) {
            Log.e(com.wms.panchkula.ui.invoiceOrder.UI.TAG, "onCreate:===> " + e.message)

            e.printStackTrace()
        }


        binding.ivOnback.setOnClickListener {
            onBackPressed()
            finish()
            AppConstants.scannedItemForGood.clear()
        }
        binding.chipSave.setOnClickListener {

            //var batchInDate = BatchScannedData.split(",")[5].replace("-","")
            // getQuantityForSuggestion(textMain, itemCodeMain, batchInDate )


            // callGoodsPostingApi()
            postInventorystock(requestListModel_gl)
        }

        binding.chipCancel.setOnClickListener {
            onBackPressed()
            finish()
            AppConstants.scannedItemForGood.clear()
        }

        binding.edBatchCodeScan.addTextChangedListener {
            val text = binding.edBatchCodeScan.text.toString().trim()
            if (text.trim().isNotEmpty()) {
                var arr = text.split(",")
                if (arr.size > 0) {
                    WhareHouse = arr[0].toString().trim()
                    binAbsEntry = arr[2].toString().trim()
                    getBPLID(binding.edBatchCodeScan.text.toString().trim().split(",")[0].toString(), "GOODSRECEIPT")
                } else {
                    Toast.makeText(this@GoodsReceiptLineActivity, "Please scan warehouse QR", Toast.LENGTH_SHORT).show()

                }
                // binding.edFromWhareHouse.setText("")
            }

        }


    }


    override fun onBackPressed() {
        super.onBackPressed()
        finish()
        AppConstants.scannedItemForGood.clear()
    }

    var WhareHouse = ""
    var binAbsEntry = ""
    var BPLID = ""
    var BinManaged = ""
    var series = ""
    private fun getBPLID(warehouseCode: String, DocType: String) {
        if (networkConnection.getConnectivityStatusBoolean(this)) {
            materialProgressDialog.show()
            var apiConfig = ApiConstantForURL()

            NetworkClients.updateBaseUrlFromConfig(apiConfig)

            QuantityNetworkClient.updateBaseUrlFromConfig(apiConfig)

            val networkClient = QuantityNetworkClient.create(this)
            val bplId = if (Prefs.getString(AppConstants.BPLID, "").isNotEmpty()) Prefs.getString(AppConstants.BPLID, "") else ""

            networkClient.doGetBPLID(warehouseCode, DocType, bplId)
                .apply {
                    enqueue(object : Callback<Warehouse_BPLID> {
                        override fun onResponse(
                            call: Call<Warehouse_BPLID>,
                            response: Response<Warehouse_BPLID>
                        ) {
                            try {
                                materialProgressDialog.dismiss()
                                if (response.isSuccessful) {
                                    Log.e("response---------", response.body().toString())

                                    var responseModel = response.body()!!
                                    if (responseModel.value.size > 0 && !responseModel.value.isNullOrEmpty()) {

                                        BPLID = responseModel.value[0].BPLID
                                        BinManaged = responseModel.value[0].BinManaged
                                        series = responseModel.value[0].Series


                                    } else {
                                        GlobalMethods.showError(this@GoodsReceiptLineActivity, "Invalid Batch Code")
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
                                            GlobalMethods.showError(
                                                this@GoodsReceiptLineActivity,
                                                mError.error.message.value
                                            )
                                        }
                                        if (mError.error.message.value != null) {
                                            GlobalMethods.showError(
                                                this@GoodsReceiptLineActivity,
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
                            }
                        }

                        override fun onFailure(call: Call<Warehouse_BPLID>, t: Throwable) {
                            Log.e("scanItemApiFailed-----", t.toString())
                            materialProgressDialog.dismiss()
                        }

                    })
                }
        } else {
            materialProgressDialog.dismiss()
            Toast.makeText(this@GoodsReceiptLineActivity, "No Network Connection", Toast.LENGTH_SHORT)
                .show()
        }
    }

    //todo calling api adapter here---
    private fun setInvoiceOrderAdapter() {
        layoutManager = LinearLayoutManager(this)
        binding.rvProductionOrderList.layoutManager = layoutManager
        requestAdapter = GoodReceiptAdapter(this, requestListModel_gl)
        binding.rvProductionOrderList.adapter = requestAdapter


        //todo adapter on item click listener....
        requestAdapter?.OnItemClickListener { list, pos ->

            Log.e("warehouse", "onCreate:InOrder ")


        }
        requestAdapter?.notifyDataSetChanged()


    }


    @RequiresApi(Build.VERSION_CODES.O)
    private fun postInventorystock(list: MutableList<GetItemstModel.Value>) {

        val currentDate = LocalDateTime.now()
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")
        val formattedDate = currentDate.format(formatter)
        var postedJson = JsonObject()

        postedJson.addProperty("Series", series)
        postedJson.addProperty("DocDate", formattedDate)
        postedJson.addProperty("DocDueDate", formattedDate)
        postedJson.addProperty("TaxDate", formattedDate)
        postedJson.addProperty("BPL_IDAssignedToInvoice", BPLID)
        postedJson.addProperty("Comments", binding.etRemarks.text.toString())  // added by Vinod @28Apr,2025
        postedJson.addProperty("DocObjectCode", "oInventoryGenEntry")
        postedJson.addProperty("U_Type", "")
        postedJson.addProperty("U_WMSPOST", "Y") //U_WMSPOST tagged (added by Vinod @13Aug,2025)
        postedJson.addProperty("U_WMSUSER", sessionManagement.getUsername(this@GoodsReceiptLineActivity))  //WMS userName tagged (added by Vinod @25Apr,2025)

        if (networkConnection.getConnectivityStatusBoolean(applicationContext)) {
            materialProgressDialog.show()

            var StockTransferLines = JsonArray()

            var line = 0;
            for (i in list.indices) {
                line = line + 1
                val jsonObject = JsonObject()
                jsonObject.addProperty("LineNum", line.toString())
                jsonObject.addProperty("ItemCode", list[i].ItemCode)
                jsonObject.addProperty("UnitPrice", list[i].UnitPrice)
                jsonObject.addProperty("Quantity", list[i].Quantity)//list[i].totakPktQty
                jsonObject.addProperty("U_Size", list[i].Size)

                jsonObject.addProperty("WarehouseCode", binding.edBatchCodeScan.text.toString().trim().split(",")[0].toString())


                val stockBatch = JsonArray()
                if (list[i].ItemType.equals("BATCH", true)) {

                    var jsonLinesObject = JsonObject()

                    jsonLinesObject.addProperty("BatchNumber", list[i].BatchNo)
                    jsonLinesObject.addProperty("Quantity", list[i].Quantity)

                    stockBatch.add(jsonLinesObject)

                }
                jsonObject.add("BatchNumbers", stockBatch)
                val stockBatch_Temp = JsonArray()
                jsonObject.add("SerialNumbers", stockBatch_Temp)

                val binArray = JsonArray()
                if (BinManaged.equals("Y", true)) {
                    Log.e("BinSum==>", binAbsEntry.toString())
                    var binObject = JsonObject()

                    binObject.addProperty("BinAbsEntry", binAbsEntry)
                    binObject.addProperty("SerialAndBatchNumbersBaseLine", i.toString())
                    binObject.addProperty("Quantity", list[i].Quantity)
                    binArray.add(binObject)
                    jsonObject.add("DocumentLinesBinAllocations", binArray)

                } else {
                    jsonObject.add("DocumentLinesBinAllocations", binArray)

                }


                //  jsonObject.add("binAllocationJSONs", getJsonArray(list[i].binAllocationJSONs,batchList.size))

                if (list.size > 0)
                    StockTransferLines.add(jsonObject)

            }

            postedJson.add("DocumentLines", StockTransferLines)



            Log.e("success--PayLoad==>", "==>" + postedJson.toString())

            var apiConfig = ApiConstantForURL()

            NetworkClients.updateBaseUrlFromConfig(apiConfig)

            QuantityNetworkClient.updateBaseUrlFromConfig(apiConfig)

            val networkClient = QuantityNetworkClient.create(this@GoodsReceiptLineActivity)
            networkClient.doGoodsReceiptTransfer(postedJson).apply {
                enqueue(object : Callback<InventoryPostResponse> {
                    override fun onResponse(
                        call: Call<InventoryPostResponse>,
                        response: Response<InventoryPostResponse>
                    ) {
                        try {


                            AppConstants.IS_SCAN = false
                            materialProgressDialog.dismiss()
                            Log.e("success---BP---", "==>" + response.code())
                            if (response.code() == 201 || response.code() == 200) {
                                Log.e("success------", "Successful!")
                                AppConstants.selectedList.clear()
                                requestListModel_gl.clear()
                                if (requestAdapter != null) {
                                    requestAdapter?.notifyDataSetChanged()
                                }

                                Log.d("Doc_Num", "onResponse: " + response.body()!!.DocNum.toString())
                                //GlobalMethods.showSuccess(this@GoodsReceiptLineActivity, "Post Successfully. " + response.body()!!.DocNum.toString())
                                showSuccessDialog(
                                    context = this@GoodsReceiptLineActivity,
                                    title = "Goods Receipt",
                                    successMsg = "Goods receipt order post successfully with docnum ",
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
                                            this@GoodsReceiptLineActivity,
                                            mError.error.message.value
                                        )
                                    }
                                    if (mError.error.message.value != null) {
                                        GlobalMethods.showError(
                                            this@GoodsReceiptLineActivity,
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

                    override fun onFailure(call: Call<InventoryPostResponse>, t: Throwable) {

                        Log.e("orderLines_failure-----", t.toString())
                        materialProgressDialog.dismiss()
                    }

                })
            }

        } else {
            materialProgressDialog.dismiss()
            handler.post {
                Toast.makeText(
                    this@GoodsReceiptLineActivity,
                    "No Network Connection",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    lateinit var batchList: List<ScanedOrderBatchedItems.Value>
    lateinit var batchQuantityList: MutableList<String>


}