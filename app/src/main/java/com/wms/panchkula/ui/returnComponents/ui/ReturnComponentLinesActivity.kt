package com.wms.panchkula.ui.returnComponents.ui

import android.app.Dialog
import android.content.ContentValues.TAG
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.pixplicity.easyprefs.library.Prefs
import com.wms.panchkula.Global_Classes.AppConstants
import com.wms.panchkula.Global_Classes.GlobalMethods
import com.wms.panchkula.Global_Classes.GlobalMethods.getCurrentDate_dd_MM_yyyy
import com.wms.panchkula.Global_Classes.GlobalMethods.showSuccessDialog
import com.wms.panchkula.Global_Classes.GlobalMethods.toEditable
import com.wms.panchkula.Global_Classes.GlobalMethods.toPrettyJson
import com.wms.panchkula.Global_Classes.GlobalMethods.toSimpleJson
import com.wms.panchkula.Global_Classes.MaterialProgressDialog
import com.wms.panchkula.Global_Notification.NetworkConnection
import com.wms.panchkula.Model.OtpErrorModel
import com.wms.panchkula.R
import com.wms.panchkula.Retrofit_Api.ApiConstantForURL
import com.wms.panchkula.Retrofit_Api.NetworkClients
import com.wms.panchkula.Retrofit_Api.QuantityNetworkClient
import com.wms.panchkula.SessionManagement.SessionManagement
import com.wms.panchkula.databinding.ActivityReturnComponentLinesBinding
import com.wms.panchkula.ui.goodsreceipt.model.IssueFromModel
import com.wms.panchkula.ui.issueForProductionOrder.Model.InventoryGenExitsModel
import com.wms.panchkula.ui.issueForProductionOrder.Model.ProductionListModel
import com.wms.panchkula.ui.purchase.model.PurchaseRequestModel
import com.wms.panchkula.ui.returnComponents.adapter.ReturnComponentItemAdapter
import com.wms.panchkula.ui.returnComponents.adapter.ReturnComponentsLinesAdapter
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.IOException

class ReturnComponentLinesActivity : AppCompatActivity() {
    private lateinit var binding: ActivityReturnComponentLinesBinding
    lateinit var materialProgressDialog: MaterialProgressDialog
    private lateinit var sessionManagement: SessionManagement
    private lateinit var productionOrderLineList_gl: ArrayList<ProductionListModel.ProductionOrderLine>
    private lateinit var productionOrderValueItem_gl: ProductionListModel.Value
    private lateinit var returnComponentLinesAdapter: ReturnComponentsLinesAdapter
    var tempList: ArrayList<ProductionListModel.ProductionOrderLine> = ArrayList()
    lateinit var networkConnection: NetworkConnection
    var productionOrderLinePos: Int = 0
    lateinit var clickedProductionLineItem: ProductionListModel.ProductionOrderLine


    var position: Int? = 0
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReturnComponentLinesBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initViews()
        clickListener()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun initViews() {
        supportActionBar?.hide()

        //todo initialization...
        networkConnection = NetworkConnection()
        materialProgressDialog = MaterialProgressDialog(this@ReturnComponentLinesActivity)
        sessionManagement = SessionManagement(this@ReturnComponentLinesActivity)
        //todo get arguments data...
        try {
            val intent = intent
            productionOrderLineList_gl = intent.getSerializableExtra("productionLinesList") as ArrayList<ProductionListModel.ProductionOrderLine>
            productionOrderValueItem_gl = intent.getSerializableExtra("productionValueItem") as ProductionListModel.Value //todo getting list selected item values and lines only not all size data..
            position = intent.extras?.getInt("pos")

            GlobalMethods.ProdDocEntry = productionOrderValueItem_gl.AbsoluteEntry
            setAdapter()
            binding.etPostingDate.text = getCurrentDate_dd_MM_yyyy().toEditable()
        } catch (e: IOException) {
            Log.e(TAG, "onCreate:===> " + e.message)
            e.printStackTrace()
        }
    }

    fun setAdapter() {
        //todo removing order line if BaseQuantity value in negative.... and also this way is removing IndexOutOfBoundException from list....

        for (i in 0 until productionOrderLineList_gl.size) {
            try {
                val plannedQty = productionOrderLineList_gl[i].PlannedQuantity
                val issuedQty = productionOrderLineList_gl[i].IssuedQuantity
                val openQty = plannedQty - issuedQty
                if (productionOrderLineList_gl[i].BaseQuantity > 0.0 && openQty > 0.0) {
                    tempList.add(productionOrderLineList_gl[i])
                }
            } catch (e: IndexOutOfBoundsException) {
                e.printStackTrace()
            }
        }

        Log.e("RETURN_COMPONENT", "productionOrderLineList_gl=> ${toPrettyJson(productionOrderLineList_gl)}\n")
        Log.d("RETURN_COMPONENT", "tempList=> ${toPrettyJson(tempList)}")

        if (tempList.size > 0) {
            binding.ivNoDataFound.visibility = View.GONE
            binding.rvProductionOrderList.visibility = View.VISIBLE
            binding.btnLinearLayout.visibility = View.VISIBLE

            val layoutManager: RecyclerView.LayoutManager = LinearLayoutManager(this)
            binding.rvProductionOrderList.layoutManager = layoutManager
            //todo parse save button in adapter constructor for click listener on adapter...
            returnComponentLinesAdapter = ReturnComponentsLinesAdapter(
                this@ReturnComponentLinesActivity,
                tempList,
                onReturnItemClicked = { adapterPosition, item, tvReturnQty ->
                    productionOrderLinePos = adapterPosition
                    clickedProductionLineItem = item
                    Log.e("RETURN_COMPONENT", "lineItemPosition (adapterPosition)=> $productionOrderLinePos\nLineItem: ${toSimpleJson(item)}")
                    if (item.Batch.equals("Y", true)) {
                        GetBatchFromIssueFromProd(tvReturnQty, item, GlobalMethods.ProdDocEntry, adapterPosition,"Batch")
                    } else if (item.Serial.equals("Y", true)) {
                        GetBatchFromIssueFromProd(tvReturnQty, item, GlobalMethods.ProdDocEntry, adapterPosition,"Serial")
                    } else {
                        GetBatchFromIssueFromProd(tvReturnQty, item, GlobalMethods.ProdDocEntry, adapterPosition,"None")
                    }
                }
            )//getWarehouseCode
            binding.rvProductionOrderList.adapter = returnComponentLinesAdapter
        } else {
            binding.ivNoDataFound.visibility = View.VISIBLE
            binding.rvProductionOrderList.visibility = View.GONE
            binding.btnLinearLayout.visibility = View.GONE
        }

    }


    @RequiresApi(Build.VERSION_CODES.O)
    private fun clickListener() {

        binding.apply {
            tvItemNo.text = productionOrderValueItem_gl.ItemNo
            ivOnBack.setOnClickListener {
                onBackPressed()
            }

            chipCancel.setOnClickListener {
                onBackPressed()
            }

            chipSave.setOnClickListener {
                callReturnComponentPostApi()
            }

            etPostingDate.setOnClickListener {
                GlobalMethods.datePicker(this@ReturnComponentLinesActivity, binding.etPostingDate)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun callReturnComponentPostApi() {

        if (networkConnection.getConnectivityStatusBoolean(applicationContext)) {

            val postedJson = createProductionOrderPostJson(tempList)

            if (false)
                return

            materialProgressDialog.show()
            val apiConfig = ApiConstantForURL()
            NetworkClients.updateBaseUrlFromConfig(apiConfig)
            val networkClient = NetworkClients.create(this@ReturnComponentLinesActivity)
            networkClient.doGetInventoryGenEntries(postedJson).apply {
                enqueue(object : Callback<InventoryGenExitsModel> {
                    override fun onResponse(
                        call: Call<InventoryGenExitsModel>,
                        response: Response<InventoryGenExitsModel>
                    ) {
                        try {
                            binding.chipSave.isEnabled = true
                            binding.chipSave.isCheckable = true
                            materialProgressDialog.dismiss()
                            if (response.isSuccessful) {
                                if (response.code() == 201) {
                                    Log.e("success------", "Successful!")
                                    //GlobalMethods.showSuccess(this@ReturnComponentLinesActivity, "Return Components Order Post Successfully.")
                                    showSuccessDialog(
                                        context = this@ReturnComponentLinesActivity,
                                        title = "Return Components",
                                        successMsg = "Return components post successfully with docnum ",
                                        docNum = response.body()?.DocNum.toString(),
                                        cancelable = true
                                    ) {
                                        finish()
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
                                            this@ReturnComponentLinesActivity,
                                            mError.error.message.value
                                        )
                                    }
                                    if (mError.error.message.value != null) {
                                        GlobalMethods.showError(
                                            this@ReturnComponentLinesActivity,
                                            mError.error.message.value
                                        )
                                        Log.e("json_error------", mError.error.message.value)
                                    }
                                } catch (e: IOException) {
                                    e.printStackTrace()
                                }

                            }
                        } catch (e: Exception) {
                            binding.chipSave.isEnabled = true
                            binding.chipSave.isCheckable = true
                            materialProgressDialog.dismiss()
                            e.printStackTrace()
                            Log.e("catch---------", e.toString())
                        }

                    }

                    override fun onFailure(call: Call<InventoryGenExitsModel>, t: Throwable) {
                        binding.chipSave.isEnabled = true
                        binding.chipSave.isCheckable = true
                        Log.e("orderLines_failure-----", t.toString())
                        materialProgressDialog.dismiss()
                    }

                })
            }
        } else {
            Toast.makeText(
                this@ReturnComponentLinesActivity,
                "No Network Connection",
                Toast.LENGTH_SHORT
            ).show()
        }

    }

    /*private fun saveProductionOrderLinesItems(productionOrderLineListGl: ArrayList<ProductionListModel.ProductionOrderLine>, position: Int?) {
        var comments = productionOrderValueItem_gl.Remarks.toString()
        var docDate = productionOrderValueItem_gl.PostingDate
        var absoluteEntry = productionOrderValueItem_gl.AbsoluteEntry

        var series = productionOrderValueItem_gl.ReceiptSeriesCode

        //materialProgressDialog.show()
        val bplId = if (Prefs.getString(AppConstants.BPLID, "").isNotEmpty()) Prefs.getString(AppConstants.BPLID, "") else ""
        val postedJson = JsonObject()
        postedJson.addProperty("Series", series) //series
        postedJson.addProperty("DocDate", GlobalMethods.getCurrentDateFormatted()) //todo current date will send here---
        postedJson.addProperty("DocObjectCode", "oInventoryGenEntry")
        postedJson.addProperty("BPL_IDAssignedToInvoice", bplId)
        //postedJson.addProperty("Comments", comments)
        postedJson.addProperty("U_WMS", "Yes") //series

        val documentLinesArray = JsonArray()

        clickedProductionLineItem?.let {

            val totalQuantity = it.binAllocationJSONs.sumOf { it.Quantity.toDoubleOrNull() ?: 0.0 }
            val documentLine = JsonObject()
            documentLine.addProperty("WarehouseCode", it.Warehouse)
            documentLine.addProperty("Quantity", totalQuantity)
            documentLine.addProperty("BaseType", 202)
            documentLine.addProperty("BaseEntry", absoluteEntry)
            documentLine.addProperty("BaseLine", it.LineNumber)

            val batchArray = JsonArray()

            it.binAllocationJSONs.forEach { batch ->
                val quantity = batch.Quantity?.trim()

                val batchObj = JsonObject()
                batchObj.addProperty("BatchNumber", batch.BatchNum ?: "")
                batchObj.addProperty("Quantity", quantity?.toDouble())
                //batchObj.addProperty("BaseLineNumber", it.LineNumber)
                if (batch.BatchNum.isNotEmpty()) {
                    batchObj.addProperty("SerialAndBatchNumbersBaseLine", "0")
                } else {
                    batchObj.addProperty("SerialAndBatchNumbersBaseLine", "-1")
                }
                batchArray.add(batchObj)
            }

            documentLine.add("BatchNumbers", batchArray)
            documentLinesArray.add(documentLine)
            postedJson.add("DocumentLines", documentLinesArray)
            Log.e("RETURN_COMPONENT", "Post Payload==>" + postedJson.toString())
        }
    }*/

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createProductionOrderPostJson(
        productionOrderLineListGl: ArrayList<ProductionListModel.ProductionOrderLine>
    ): JsonObject {
        val comments = productionOrderValueItem_gl.Remarks.toString()
        //val docDate = productionOrderValueItem_gl.PostingDate
        val absoluteEntry = productionOrderValueItem_gl.AbsoluteEntry
        val series = productionOrderValueItem_gl.ReceiptSeriesCode

        var docDate = GlobalMethods.convert_dd_MM_yyyy_into_yyyy_MM_dd(binding.etPostingDate.text.toString())
        if (docDate.isEmpty()) {
            docDate = GlobalMethods.getCurrentDateFormatted()
        }

        val bplId = Prefs.getString(AppConstants.BPLID, "")
        val postedJson = JsonObject()
        postedJson.addProperty("Series", series)
        postedJson.addProperty("DocDate", docDate)
        postedJson.addProperty("DocObjectCode", "oInventoryGenEntry")
        postedJson.addProperty("BPL_IDAssignedToInvoice", bplId)
        postedJson.addProperty("U_WMS", "Yes")
        postedJson.addProperty("U_WMSPOST", "Y") //U_WMSPOST tagged (added by Vinod @13Aug,2025)
        postedJson.addProperty("U_WMSUSER", sessionManagement.getUsername(this@ReturnComponentLinesActivity))  //WMS userName tagged (added by Vinod @13Aug,2025)

        val documentLinesArray = JsonArray()

        productionOrderLineListGl.forEach { lineItem ->
            val lineNumber = lineItem.LineNumber?.toDoubleOrNull()?.toInt()
            Log.e("RETURN_COMPONENT", "lineItem=> ${lineItem.LineNumber}")
            val binList = lineItem.binAllocationJSONs ?: emptyList()

            val totalQuantity = binList
                .filter { !it.Quantity.isNullOrBlank() }
                .sumOf { it.Quantity?.toDoubleOrNull() ?: 0.0 }

            if (totalQuantity > 0) {
                val documentLine = JsonObject()
                documentLine.addProperty("WarehouseCode", lineItem.Warehouse)
                documentLine.addProperty("Quantity", totalQuantity)
                documentLine.addProperty("BaseType", 202)
                documentLine.addProperty("BaseEntry", absoluteEntry)
                documentLine.addProperty("BaseLine", lineNumber)

                val batchArray = JsonArray()
                if (lineItem.Batch.equals("Y", true)) {
                    val validBatches = binList
                        .filter { !it.Quantity.isNullOrBlank() && !it.BatchNum.isNullOrBlank() }
                        .groupBy { it.BatchNum }
                    if (validBatches.isNotEmpty()) {

                        var baseLineIndex = 0  // Start index for SerialAndBatchNumbersBaseLine
                        validBatches.forEach { (batchNum, items) ->
                            val totalQty = items.sumOf { it.Quantity?.toDoubleOrNull() ?: 0.0 }

                            if (totalQty > 0) {
                                val batchObj = JsonObject()
                                val sysNumber = items.firstOrNull()?.InternalSerialNumber
                                batchObj.addProperty("BatchNumber", batchNum)
                                batchObj.addProperty("Quantity", totalQty)
                                //batchObj.addProperty("SerialAndBatchNumbersBaseLine", lineNumber)
                                batchObj.addProperty("SystemSerialNumber", sysNumber)
                                batchArray.add(batchObj)
                                baseLineIndex++ // Increment index for next batch
                            }
                        }
                    }
                }


                // Attach batchArray (either with objects or empty) to DocumentLine
                documentLine.add("BatchNumbers", batchArray)
                documentLinesArray.add(documentLine)
            }
        }

        postedJson.add("DocumentLines", documentLinesArray)
        Log.e("RETURN_COMPONENT", "Return Component Payload==>\n${postedJson}")
        return postedJson
    }


    private fun GetBatchFromIssueFromProd(tvReturnQty: TextView, item: ProductionListModel.ProductionOrderLine, ProdDocEntry: String, productionOrderLinePos: Int, ItemType: String) {

        if (networkConnection.getConnectivityStatusBoolean(this)) {
            materialProgressDialog.show()
            var apiConfig = ApiConstantForURL()

            NetworkClients.updateBaseUrlFromConfig(apiConfig)

            QuantityNetworkClient.updateBaseUrlFromConfig(apiConfig)

            val networkClient = QuantityNetworkClient.create(this)
            networkClient.GetBatchFromIssueFromProd(item.ItemNo, ProdDocEntry, ItemType)
                .apply {
                    enqueue(object : Callback<IssueFromModel> {
                        override fun onResponse(
                            call: Call<IssueFromModel>,
                            response: Response<IssueFromModel>
                        ) {
                            try {
                                materialProgressDialog.dismiss()
                                if (response.isSuccessful) {
                                    Log.e("response---------", response.body().toString())

                                    var responseModel = response.body()!!
                                    if (responseModel.value.size > 0 && !responseModel.value.isNullOrEmpty()) {


                                        //Log.i("RETURN_COMPONENT", "GetBatchFromIssueFromProd=> ${toSimpleJson(responseModel.value)}")
                                        //ReturnItemDialogFragment(responseModel.value).show(supportFragmentManager, "Dialog")
                                        openReturnComponentDialog(this@ReturnComponentLinesActivity, productionOrderLinePos, responseModel.value, tvReturnQty,item)

                                    } else {
                                        GlobalMethods.showError(this@ReturnComponentLinesActivity, "Invalid  Response.")
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
                                                this@ReturnComponentLinesActivity,
                                                mError.error.message.value
                                            )
                                        }
                                        if (mError.error.message.value != null) {
                                            GlobalMethods.showError(
                                                this@ReturnComponentLinesActivity,
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

                        override fun onFailure(call: Call<IssueFromModel>, t: Throwable) {
                            Log.e("scanItemApiFailed-----", t.toString())
                            materialProgressDialog.dismiss()
                        }

                    })
                }
        } else {
            materialProgressDialog.dismiss()
            Toast.makeText(this@ReturnComponentLinesActivity, "No Network Connection", Toast.LENGTH_SHORT)
                .show()
        }
    }

    private fun openReturnComponentDialog(
        context: Context,
        productionOrderLinePos: Int,
        value: ArrayList<IssueFromModel.Value>, tvReturnQty: TextView,
        item: ProductionListModel.ProductionOrderLine
    ) {

        val dialog = Dialog(context)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(true)
        dialog.setContentView(R.layout.dialog_return_component_items)
        dialog.setCancelable(false)
        // Ensure the background is transparent
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        // Set dialog width to MATCH_PARENT
        dialog.window?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT
        )
        //dialog.show()


        val chipSave: Chip = dialog.findViewById(R.id.chipSave)
        val chipCancel: Chip = dialog.findViewById(R.id.chipCancel)
        val rvReturnComponentItems: RecyclerView = dialog.findViewById(R.id.rvReturnComponentItems)
        Log.e("RETURN_COMPONENT", "openDynamicFieldsDialog=> GetBatchFromIssueFromProd List: ${toSimpleJson(value)}\nItem Position => $productionOrderLinePos")
        rvReturnComponentItems.run {

            layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
            val returnComponentItemsAdapter = ReturnComponentItemAdapter(context, value,item)
            adapter = returnComponentItemsAdapter
            returnComponentItemsAdapter?.notifyDataSetChanged()
            //setHasFixedSize(true)
        }


        chipSave.setOnClickListener {

            val myArrayList = ArrayList<PurchaseRequestModel.binAllocationJSONs>()

            Log.i("RETURN_COMPONENT", "binAllocationJSONs List before selection => ${toSimpleJson(myArrayList)}")
            for (j in value.indices) {

                val issue = value[j]

                Log.w("RETURN_COMPONENT", "IssueFromModelArraylist item[$j] => ${toSimpleJson(issue)}")

                val binAllocationJSONs = PurchaseRequestModel.binAllocationJSONs(
                    "",
                    "",
                    issue.Batch,
                    issue.EnteredQTY ?: "0",
                    "",
                    "",
                    "",
                    issue.SysNumber,
                    "",
                    ""
                )
                Log.i("RETURN_COMPONENT", "binAllocationJSONs item[$j]=> ${toSimpleJson(binAllocationJSONs)}")

                myArrayList.add(binAllocationJSONs)
                Log.i("RETURN_COMPONENT", "binAllocationJSONs List after add item[$j]=> ${toSimpleJson(myArrayList)}")
            }

            Log.w("RETURN_COMPONENT", "binAllocationJSONs list Actual=> ${toSimpleJson(tempList[productionOrderLinePos].binAllocationJSONs)}")
            tempList[productionOrderLinePos].binAllocationJSONs = arrayListOf()
            Log.w("RETURN_COMPONENT", "binAllocationJSONs list arrayListOf()=> ${toSimpleJson(tempList[productionOrderLinePos].binAllocationJSONs)}")
            val filterList = myArrayList.filter {
                !it.Quantity.isNullOrBlank() && it.Quantity.trim() != "0"
            }

            tempList[productionOrderLinePos].binAllocationJSONs.addAll(filterList)

            if (!tempList[productionOrderLinePos].binAllocationJSONs.isNullOrEmpty()) {
                val returnQty = tempList[productionOrderLinePos].binAllocationJSONs.sumOf { it.Quantity.toDoubleOrNull() ?: 0.0 } ?: 0.0
                tvReturnQty.text = returnQty.toString()
            }
            returnComponentLinesAdapter?.notifyItemChanged(productionOrderLinePos)

            Log.e("RETURN_COMPONENT", "binAllocationJSONs list after add myArrayList=> ${toSimpleJson(tempList[productionOrderLinePos].binAllocationJSONs)}")
            Log.e("RETURN_COMPONENT", "return componentLines list after add myArrayList=> ${toPrettyJson(tempList)}")
            dialog.dismiss()

        }
        chipCancel.setOnClickListener {
            dialog.dismiss()
        }
        dialog.show()

    }
}