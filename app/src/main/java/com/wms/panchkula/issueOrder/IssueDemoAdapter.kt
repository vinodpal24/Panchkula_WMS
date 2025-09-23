package com.wms.panchkula.issueOrder

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
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
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.wms.panchkula.Global_Classes.AppConstants
import com.wms.panchkula.Global_Classes.GlobalMethods
import com.wms.panchkula.Global_Classes.MaterialProgressDialog
import com.wms.panchkula.Global_Notification.NetworkConnection
import com.wms.panchkula.Model.GetQuantityModel
import com.wms.panchkula.Model.OtpErrorModel
import com.wms.panchkula.R
import com.wms.panchkula.Retrofit_Api.NetworkClients
import com.wms.panchkula.Retrofit_Api.QuantityNetworkClient
import com.wms.panchkula.SessionManagement.SessionManagement
import com.wms.panchkula.databinding.ProductionOrderLinesAdapterLayoutBinding
import com.wms.panchkula.Retrofit_Api.ApiConstantForURL
import com.wms.panchkula.ui.home.HomeActivity
import com.wms.panchkula.ui.issueForProductionOrder.Model.ProductionListModel
import com.wms.panchkula.ui.issueForProductionOrder.Model.ScanedOrderBatchedItems
import com.wms.panchkula.ui.issueForProductionOrder.UI.qrScannerUi.QRScannerActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.gson.GsonBuilder
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.IOException

class IssueDemoAdapter(var context: IssueOrderDetailDemoActivity, var list: ArrayList<ProductionListModel.ProductionOrderLine>,
                       var networkConnection: NetworkConnection, var materialProgressDialog: MaterialProgressDialog) : RecyclerView.Adapter<IssueDemoAdapter.ViewHolder>() , InnerItemAdapter.OnItemActionListener, InnerItemAdapter.OnDeleteItemClickListener{

    companion object{
        const val HIDE_KEYBOARD_DELAY: Long = 200
    }

    //todo declaration..
    val REQUEST_CODE = 100
    private lateinit var sessionManagement: SessionManagement

    private var scanedBatchedItemsList_gl: ArrayList<ScanedOrderBatchedItems.Value> = ArrayList()
    private lateinit var recyclerView: RecyclerView
    private var pos: Int = 0
    private var itemCode = ""
    var innerItemAdapter: InnerItemAdapter? = null
    var openQty = 0.0
    lateinit var tvOpenQty : TextView
    lateinit var tvTotalScanGW: TextView
    lateinit var tvTotalScanQty: TextView

    init {
        sessionManagement = SessionManagement(context)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ProductionOrderLinesAdapterLayoutBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        with(holder) {
            with(list[position]) {
                binding.tvItemCode.text = ":   " + this.ItemNo
                binding.tvItemName.text = ":   " + this.ItemName
                var qty = this.PlannedQuantity - this.IssuedQuantity
                binding.tvOpenQty.text = ":   " + qty.toString()
                tvTotalScanQty = binding.tvTotalScannQty
                if (this.Batch == "Y" && this.Serial == "N" && this.None == "N") {
                    binding.tvTotalScanGw.text = "Batch"
                } else if (this.Serial == "Y" && this.Batch == "N" && this.None == "N") {
                    binding.tvTotalScanGw.text = "Serial"
                } else if (this.None == "Y" && this.Batch == "N" && this.Serial == "N") {
                    binding.tvTotalScanGw.text = "None"
                }


                //todo HIDE
                Handler(Looper.getMainLooper()).postDelayed({
                    val imm =
                        context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                    if (imm != null && binding.edBatchCodeScan != null) {
                        imm.hideSoftInputFromWindow(binding.edBatchCodeScan.windowToken, 0)
                    }
                }, HIDE_KEYBOARD_DELAY)

                //todo if leaser type choose..
                if (sessionManagement.getScannerType(context) == "LEASER") { //sessionManagement.getLeaserCheck()!! == 1 && sessionManagement.getQRScannerCheck()!! == 0
                    binding.ivScanBatchCode.visibility = View.GONE

                    var itmNo = this.ItemNo

                    binding.edBatchCodeScan.addTextChangedListener(object : TextWatcher {
                        override fun afterTextChanged(s: Editable?) {

                            val text = s.toString().trim()

                            if (text.isNotEmpty()) {
                                try {
                                    val parts = text.toString().split(",")

                                    val lastPart = parts.last()
                                    var itemCode = parts[0]

                                    type = lastPart

                                    //todo set validation for duplicate item
                                    if (type == "Batch") {
                                        val batch = text.split(",")[1]
                                        if (!itemCode.isNullOrEmpty()) {
                                            if (checkDuplicate(AppConstants.scannedItemForIssueOrder, batch)) {
                                                //todo scan call api here...
                                                scanBatchLinesItem(batch, binding.rvBatchItems, pos, itemCode, binding.tvTotalScannQty, type, itmNo, qty)

                                            }
                                        } else {
                                            GlobalMethods.showError(context, "Item code is empty")
                                        }

                                    } else if (type.equals("Serial")) {
                                        val batch = text.split(",")[1]
                                        if (checkDuplicateForSerial(AppConstants.scannedItemForIssueOrder, batch)) {
                                            //todo scan call api here...
//                                            scanSerialLineItem(batch, binding.rvBatchItems, pos, itemCode, binding.tvTotalScannQty, type)
                                        }
                                    } else if (type.equals("NONE") || type.equals("None")) {
                                        var scanItem = text.toString()
                                        val parts = text.toString().split(",")

                                        val lastPart = parts.last()
                                        var itemCode = parts[0]
                                        itemDesc = parts[2]

                                        type = lastPart


                                        if (checkDuplicateForNone(AppConstants.scannedItemForIssueOrder, scanItem)) {
                                            //todo scan call api here...
//                                            callNoneBindFunction(itemCode, binding.rvBatchItems, pos, binding.tvTotalScannQty, itemDesc, scanItem)

                                        }


                                    }

                                    // Clear the EditText and request focus
                                    binding.edBatchCodeScan.setText("")
                                    binding.edBatchCodeScan.requestFocus()

                                    Handler(Looper.getMainLooper()).postDelayed({
                                        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                                        imm?.hideSoftInputFromWindow(binding.edBatchCodeScan.windowToken, 0)
                                    }, HIDE_KEYBOARD_DELAY)
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
                else if (sessionManagement.getScannerType(context) == "QR_SCANNER" || sessionManagement.getScannerType(context) == null) { //|| sessionManagement.getLeaserCheck()!! == 0 && sessionManagement.getQRScannerCheck()!! == 1 || sessionManagement.getLeaserCheck()!! == 0 && sessionManagement.getQRScannerCheck()!! == 0
                    binding.ivScanBatchCode.visibility = View.VISIBLE

                    //TODO click on barcode scanner for popup..
                    binding.ivScanBatchCode.setOnClickListener {
                        var text = binding.edBatchCodeScan.text.toString().trim()
                        recyclerView = binding.rvBatchItems
//                itemCode = this.ItemNo.toString()
                        tvOpenQty = binding.tvOpenQty
                        tvTotalScanGW = binding.tvTotalScanGw
                        openQty = qty

                        if (sessionManagement.getScannerType(context) == null) {
                            showPopupNotChooseScanner()
                        } else if (sessionManagement.getScannerType(context) == "QR_SCANNER") {
                            val intent = Intent(context, QRScannerActivity::class.java)
                            (context as IssueOrderDetailDemoActivity).startActivityForResult(intent, REQUEST_CODE)
                        }

                    }


                }


            }


        }


    }


    override fun getItemCount(): Int {
        return list.size
    }

    init {
        setHasStableIds(true)
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getItemViewType(position: Int): Int {
        return position
    }

    //TODO viewholder...
    class ViewHolder(val binding: ProductionOrderLinesAdapterLayoutBinding) : RecyclerView.ViewHolder(binding.root)


    //TODO duplicatcy checking from list...
    fun checkDuplicate(scanedBatchedItemsList_gl: MutableList<ProductionListModel.ProductionOrderLine>, batchCode: String): Boolean {
        var startus: Boolean = true;
        for (items in scanedBatchedItemsList_gl) {
            if (items.Batch == null) {
                startus = true
            } else if (items.Batch.equals(batchCode)) {
                startus = false
                Toast.makeText(context, "Batch no. Already Exists!", Toast.LENGTH_SHORT).show()
            }
        }
        return startus
    }


    fun checkDuplicateForSerial(scanedBatchedItemsList_gl: MutableList<ProductionListModel.ProductionOrderLine>, batchCode: String): Boolean {
        var startus: Boolean = true;
        for (items in scanedBatchedItemsList_gl) {
            if (items.SerialNumber == null) {
                startus = true
            } else if (items.SerialNumber.equals(batchCode)) {
                startus = false
                Toast.makeText(context, "Serial no. Already Exists!", Toast.LENGTH_SHORT).show()
            }
        }
        return startus
    }


    fun checkDuplicateForNone(scanedBatchedItemsList_gl: MutableList<ProductionListModel.ProductionOrderLine>, batchCode: String): Boolean {
        var startus: Boolean = true;
        for (items in scanedBatchedItemsList_gl) {
            if (items.NoneVal == null) {
                startus = true
            } else if (items.NoneVal.equals(batchCode)) {
                startus = false
                Toast.makeText(context, "None no. Already Exists!", Toast.LENGTH_SHORT).show()
            }
        }
        return startus
    }

    private var itemPo: Int = -1
    private var scanCount: Int = 0

    //TODO get quantity for batch code...
    private fun setScanDataOnItem(
        arrayList: MutableList<ProductionListModel.ProductionOrderLine>,
        itemCode: String
    ): Int {

        var position = -1
        for ((index, item) in arrayList.withIndex()) {
            if (item is ProductionListModel.ProductionOrderLine && item.ItemNo == itemCode) {
                position = index
                break
            }
        }
        return position

    }


    //TODO scan item lines api here....
    private fun scanBatchLinesItem(text: String, rvBatchItems: RecyclerView, position: Int, itemCode: String?, tvTotalScannQty: TextView, type: String, itmNo: String?, qty1: Double) {
        if (networkConnection.getConnectivityStatusBoolean(context)) {
            materialProgressDialog.show()
            var apiConfig = ApiConstantForURL()

            NetworkClients.updateBaseUrlFromConfig(apiConfig)

            QuantityNetworkClient.updateBaseUrlFromConfig(apiConfig)

            val networkClient = NetworkClients.create(context)
            networkClient.doGetBatchNumScanDetails("Batch eq '" + text + "'" + " and ItemCode eq '" + itemCode + "'")
                .apply {
                    enqueue(object : Callback<ScanedOrderBatchedItems> {
                        override fun onResponse(
                            call: Call<ScanedOrderBatchedItems>,
                            response: Response<ScanedOrderBatchedItems>
                        ) {
                            try {
                                materialProgressDialog.dismiss()
                                if (response.isSuccessful) {
                                    Log.e("response---------", response.body().toString())

                                    var responseModel = response.body()!!
                                    response.body()?.let { responseModel ->
                                        if (responseModel.value.size > 0 && !responseModel.value.isNullOrEmpty()) {
                                            var modelResponse = responseModel.value
                                            scanedBatchedItemsList_gl.addAll(modelResponse)

                                            if (responseModel.value.size > 0) {

                                                Log.e("ItemCode==>", "" + responseModel.value[0].ItemCode)
                                                itemPo = setScanDataOnItem(list, responseModel.value[0].ItemCode)
                                                Log.e("ItemPo==>", "" + itemPo)
                                            }

                                            Log.e("itemPo=>", itemPo.toString())

                                            var totalScanQty = tvTotalScannQty.text.toString()
                                            var total = totalScanQty.toIntOrNull() ?: 0

                                            if (itemPo == -1) {
                                                GlobalMethods.showError(context, "Item Code not matched")
                                            } else if (total >= openQty.toDouble()) {
                                                GlobalMethods.showError(context, "Scanning completed for this Item")
                                            }else{
                                                if (responseModel.value.size > 0 && !responseModel.value.isNullOrEmpty()) {
                                                    var modelResponse = responseModel.value
                                                    if (!modelResponse.isNullOrEmpty()) {
                                                        scanedBatchedItemsList_gl.addAll(modelResponse)
                                                    } else {
                                                        GlobalMethods.showError(context, "No scanned items found")
                                                    }

                                                    var itemList_gl: ArrayList<ScanedOrderBatchedItems.Value> = ArrayList()
                                                    itemList_gl.clear()
                                                    itemList_gl.add(responseModel.value[0])

                                                    if (!itemList_gl.isNullOrEmpty()) {
                                                        //todo quantity..

                                                        getQuantityFromApi(text, itemList_gl[0].ItemCode, position, tvTotalScannQty, rvBatchItems, itemList_gl,openQty )

                                                    }
                                                    Log.e("list_size-----", itemList_gl.size.toString())



                                                } else {
                                                    GlobalMethods.showError(context, "Invalid Batch Code")
                                                    Log.e("not_response---------", response.message())
                                                }

                                            }

                                        } else {
                                            GlobalMethods.showError(context, "Invalid Batch Code")
                                            Log.e("not_response---------", response.message())
                                        }
                                    } ?: run {
                                        GlobalMethods.showError(context, "Invalid response body")
                                    }


                                } else {
                                    materialProgressDialog.dismiss()
                                    val gson1 = GsonBuilder().create()
                                    var mError: OtpErrorModel
                                    try {
                                        val s = response.errorBody()!!.string()
                                        mError = gson1.fromJson(s, OtpErrorModel::class.java)
                                        if (mError.error.code.equals(400)) {
                                            GlobalMethods.showError(context, mError.error.message.value)
                                        }
                                        if (mError.error.message.value != null) {
                                            GlobalMethods.showError(context, mError.error.message.value)
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

                        override fun onFailure(call: Call<ScanedOrderBatchedItems>, t: Throwable) {
                            Log.e("scanItemApiFailed-----", t.toString())
                            materialProgressDialog.dismiss()
                        }

                    })
                }
        } else {
            materialProgressDialog.dismiss()
            Toast.makeText(context, "No Network Connection", Toast.LENGTH_SHORT).show()
        }
    }



    //TODO scan item lines api here....
    var tempList: ArrayList<String> = ArrayList()

    private fun getQuantityFromApi(batchCode: String, itemCode: String, position: Int,
        tvTotalScannQty: TextView, rvBatchItems: RecyclerView, itemList_gl: ArrayList<ScanedOrderBatchedItems.Value>, openQty: Double) {
        if (networkConnection.getConnectivityStatusBoolean(context)) {
            materialProgressDialog.show()
            var apiConfig = ApiConstantForURL()

            NetworkClients.updateBaseUrlFromConfig(apiConfig)

            QuantityNetworkClient.updateBaseUrlFromConfig(apiConfig)

            val networkClient = QuantityNetworkClient.create(context)
            networkClient.getQuantityValue(sessionManagement.getCompanyDB(context)!!,batchCode, itemCode, sessionManagement.getWarehouseCode(context)!!)
                .enqueue(object : Callback<GetQuantityModel> {
                    override fun onResponse(call: Call<GetQuantityModel>, response: Response<GetQuantityModel>) {
                        try {
                            materialProgressDialog.dismiss()
                            if (response.isSuccessful) {
                                val responseModel = response.body()!!
                                if (responseModel.value.isNotEmpty() && !responseModel.value[0].Quantity.isNullOrEmpty() && !responseModel.value[0].Quantity.equals("0.0")) {

                                    tempList.clear()

                                    var Quantity = responseModel.value[0].Quantity.toDoubleOrNull()

                                    if (Quantity!! > openQty){
                                        scanCount = list[itemPo].isScanned
                                        ++scanCount;
                                        list[itemPo].isScanned = scanCount

                                    }

                                    Log.e("stringList", "Success=>" + responseModel.value)
                                    var stringList: ArrayList<String> = ArrayList()
                                    stringList.clear()
                                    stringList.add(responseModel.value[0].Quantity)


                                    AppConstants.scannedItemForIssueOrder.add(
                                        ProductionListModel.ProductionOrderLine(
                                            DocEntry = itemList_gl[0].DocEntry.toInt(),
                                            ItemCode = itemCode,
                                            ItemDescription = itemList_gl[0].ItemDescription,
                                            Batch = itemList_gl[0].Batch,
                                            SystemNumber = itemList_gl[0].SystemNumber.toInt(),
                                            SerialNumber = itemList_gl[0].SerialNumber,
                                            ScanType = "Batch",
                                            Quantity ="0",
                                            WareHouseCode ="",
                                            UnitPrice = "",
                                            BatchNumber = itemList_gl[0].Batch,
                                            SystemSerialNumber = itemList_gl[0].SystemNumber,
                                            InternalSerialNumber = itemList_gl[0].SerialNumber,
                                            batchList = responseModel.value as MutableList<ScanedOrderBatchedItems.Value>,
                                            serialList = mutableListOf(),
                                            noneList = mutableListOf(),
                                            binAllocationJSONs = null!!
                                        )
                                    )

                                    Log.e("item_list==>", "BATCH LOCAL LIST>>>: ${AppConstants.scannedItemForIssueOrder}")
                                    Log.e("TAG", " BATCH LOCAL LISTSIZE>>>: ${AppConstants.scannedItemForIssueOrder.size}")


                                    //todo setupNewAdapter for itemarrayaLIst
                                    innerItemAdapter = InnerItemAdapter(context, AppConstants.scannedItemForIssueOrder, tvTotalScannQty,this@IssueDemoAdapter,"IssueOrder")
                                    rvBatchItems.apply {
                                        adapter = innerItemAdapter
                                        layoutManager = LinearLayoutManager(context)
                                        //  setHasFixedSize(true)
                                    }
                                    innerItemAdapter?.setOnDeleteItemClickListener(this@IssueDemoAdapter)
                                    innerItemAdapter!!.notifyDataSetChanged()


                                }

                                else {
                                    GlobalMethods.showError(context, "No Quantity Found of this Production Order.")
                                    innerItemAdapter?.notifyDataSetChanged()
                                }

                            } else {
                                handleErrorResponse(response)
                            }
                        } catch (e: Exception) {
                            materialProgressDialog.dismiss()
                            e.printStackTrace()
                        }
                    }

                    override fun onFailure(call: Call<GetQuantityModel>, t: Throwable) {
                        Log.e("scanItemApiFailed-----", t.toString())
                        materialProgressDialog.dismiss()
                    }
                })
        } else {
            materialProgressDialog.dismiss()
            Toast.makeText(context, "No Network Connection", Toast.LENGTH_SHORT).show()
        }

    }



    private fun handleErrorResponse(response: Response<GetQuantityModel>) {
        materialProgressDialog.dismiss()
        val gson = GsonBuilder().create()
        try {
            val errorBody = response.errorBody()!!.string()
            val errorModel = gson.fromJson(errorBody, OtpErrorModel::class.java)
            errorModel.error.message.value?.let {
                GlobalMethods.showError(context, it)
                Log.e("json_error------", it)
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }


    //TODO scan item lines api here....
    private fun scanSerialLineItem(text: String, rvBatchItems: RecyclerView, position: Int, itemCode: String?, tvTotalScannQty: TextView,
        openQty: Double, type: String) {
        if (networkConnection.getConnectivityStatusBoolean(context)) {
            materialProgressDialog.show()
            var apiConfig = ApiConstantForURL()

            NetworkClients.updateBaseUrlFromConfig(apiConfig)

            QuantityNetworkClient.updateBaseUrlFromConfig(apiConfig)

            val networkClient = NetworkClients.create(context)
            networkClient.doGetSerialNumScanDetails("SerialNumber eq '" + text + "'" + " and ItemCode eq '" + itemCode + "'")
                .apply {
                    enqueue(object : Callback<ScanedOrderBatchedItems> {
                        override fun onResponse(call: Call<ScanedOrderBatchedItems>, response: Response<ScanedOrderBatchedItems>) {
                            try {
                                materialProgressDialog.dismiss()
                                if (response.isSuccessful) {
                                    Log.e("response---------", response.body().toString())

                                    var responseModel = response.body()!!

                                    if (responseModel.value.size > 0) {

                                        Log.e("ItemCode==>", "" + responseModel.value[0].ItemCode)
                                        itemPo = setScanDataOnItem(list, responseModel.value[0].ItemCode)
                                        Log.e("ItemPo==>", "" + itemPo)
                                    }
                                    var totalScanQty = tvTotalScannQty.text.toString()
                                    var total = totalScanQty.toIntOrNull() ?: 0

                                    if (itemPo == -1) {
                                        GlobalMethods.showError(context, "Item Code not matched")
                                    } else if (scanCount >= openQty.toDouble()) {
                                        GlobalMethods.showError(context, "Scanning completed for this Item")
                                    }else{
                                        if (responseModel.value.size > 0 && !responseModel.value.isNullOrEmpty()) {
                                            scanCount = list[itemPo].isScanned
                                            ++scanCount;
                                            list[itemPo].isScanned = scanCount

                                            var modelResponse = responseModel.value
                                            scanedBatchedItemsList_gl.addAll(modelResponse)

                                            var itemList_gl: ArrayList<ScanedOrderBatchedItems.Value> = ArrayList()
                                            itemList_gl.clear()
                                            itemList_gl.add(responseModel.value[0])

//                                            getQuantityFromApiForSerialType(itemCode, itemList_gl, text, type)

                                            var stringList: ArrayList<String> = ArrayList()
                                            stringList.add("1")

                                            //todo adding new serial item in locallstof appconstant
                                            AppConstants.scannedItemForIssueOrder.add(
                                                ProductionListModel.ProductionOrderLine(
                                                    DocEntry = itemList_gl[0].DocEntry.toInt(),
                                                    ItemCode = itemCode!!,
                                                    ItemDescription = itemList_gl[0].ItemDescription,
                                                    Batch = itemList_gl[0].Batch,
                                                    SystemNumber = itemList_gl[0].SystemNumber.toInt(),
                                                    SerialNumber = itemList_gl[0].SerialNumber,
                                                    ScanType = "Serial",
                                                    Quantity ="1",
                                                    WareHouseCode ="",
                                                    UnitPrice = "",
                                                    BatchNumber = itemList_gl[0].Batch,
                                                    SystemSerialNumber = itemList_gl[0].SystemNumber,
                                                    InternalSerialNumber = itemList_gl[0].SerialNumber,
                                                    batchList = mutableListOf(),
                                                    serialList = responseModel.value as MutableList<ScanedOrderBatchedItems.Value>,
                                                    noneList = mutableListOf(),binAllocationJSONs = null!!
                                                )
                                            )

                                            Log.e("Serial_Scanned", "SERAIL LOCAL LIST>>>: ${AppConstants.scannedItemForIssueOrder}")
                                            Log.e("Serial_Scanned", " SERIAL LOCAL LISTSIZE>>>: ${AppConstants.scannedItemForIssueOrder.size}")



                                        } else {
                                            GlobalMethods.showError(context, "Invalid Batch Code")
                                            Log.e("not_response---------", response.message())
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
                                            GlobalMethods.showError(context, mError.error.message.value)
                                        }
                                        if (mError.error.message.value != null) {
                                            GlobalMethods.showError(context, mError.error.message.value)
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

                        override fun onFailure(call: Call<ScanedOrderBatchedItems>, t: Throwable) {
                            Log.e("scanItemApiFailed-----", t.toString())
                            materialProgressDialog.dismiss()
                        }

                    })
                }
        } else {
            materialProgressDialog.dismiss()
            Toast.makeText(context, "No Network Connection", Toast.LENGTH_SHORT).show()
        }
    }





    val handler = Handler(Looper.getMainLooper())

    var type = ""
    var itemDesc = ""

    //todo show popup when not selected scanner type button click popup.
    private fun showPopupNotChooseScanner() {
        val builder = AlertDialog.Builder(context, R.style.CustomAlertDialog).create()
        val view = LayoutInflater.from(context).inflate(R.layout.custom_popup_alert, null)
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
            var intent = Intent(context, HomeActivity::class.java)
            context.startActivity(intent)
            builder.dismiss()
            notifyDataSetChanged()
        }

        builder.setCancelable(true)
        builder.show()
    }


    open fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {

        if (resultCode == Activity.RESULT_OK) {

            Log.e("Result==>", data?.getStringExtra("batch_code").toString())
            if (requestCode == REQUEST_CODE) {
                val result = data?.getStringExtra("batch_code")

               /* val list = itemLineArrayList as List<*>
                Log.e("size===>", list.size.toString())*/
                Log.e("ItemCode===>", itemCode)

                //todo spilt string and get string at 0 index...

                if (result!!.isNotEmpty()) {
                    // Split the string by "~"
                    val parts = result.toString().split(",")

                    val lastPart = parts.last()
                    var itemCode = parts[0]

                    type = lastPart

                    //todo set validation for duplicate item
                    if (tvTotalScanGW.text.equals("Batch")) {
                        if (checkDuplicate(AppConstants.scannedItemForIssueOrder, result.toString().split(",")[1])) {//checkDuplicate(itemLineArrayList, result.toString().split(",")[0])
                            //todo scan call api here...
                            scanBatchLinesItem(result.toString().split(",")[1], recyclerView, pos, itemCode, tvOpenQty,"","",openQty)
                        }
                    }

                else if (type.equals("Serial")) {
                        if (checkDuplicateForSerial(AppConstants.scannedItemForIssueOrder, result.toString().split(",")[1])) {
                            //todo scan call api here...
//                            scanSerialLineItem(result.toString().split(",")[1], recyclerView, pos, itemCode, tvTotalScanQty, type)
                            scanSerialLineItem(result.toString().split(",")[1], recyclerView, pos, itemCode, tvTotalScanQty, openQty, type)

                        }
                    }
                    else if (type.equals("NONE") || type.equals("None")){
                        var scanItem = result.toString().split(",")[0]
                        val parts = result.toString().split(",")

                        val lastPart = parts.last()
                        var itemCode = parts[0]
                        itemDesc = parts[2]

                        type = lastPart

                        if (checkDuplicateForNone(AppConstants.scannedItemForIssueOrder, scanItem)) {
//                            callNoneBindFunction(itemCode, recyclerView, pos, tvTotalScanQty, itemDesc, scanItem, openQty)

                        }

                    }else{
                        GlobalMethods.showMessage(context, "Scan Type is " + type )
                    }


                }

            }

        }
    }

    override fun onQuantityChanged(
        position: Int,
        newQuantity: String,
        tvBatchQuantity: TextInputEditText
    ) {
        TODO("Not yet implemented")
    }

    override fun onItemRemoved(position: Int) {
        TODO("Not yet implemented")
    }

    override fun onWareHouseChanged(
        position: Int,
        newQuantity: String,
        warehouse: String,
        currentItem: ProductionListModel.ProductionOrderLine
    ) {
        TODO("Not yet implemented")
    }

    override fun onDeleteItemClick(
        list: MutableList<ProductionListModel.ProductionOrderLine>,
        quantityHashMap: ArrayList<String>,
        pos: Int,
        batchTypeList: ArrayList<String>,
        serialTypeList: ArrayList<String>
    ) {
        TODO("Not yet implemented")
    }


}