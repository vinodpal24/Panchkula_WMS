package com.wms.panchkula.Adapter

import android.Manifest
import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Handler
import android.os.Looper
import android.os.StrictMode
import android.os.StrictMode.ThreadPolicy
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.wms.panchkula.databinding.ProductionOrderLinesAdapterLayoutBinding
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
//import com.wms.panchkula.databinding.ProductionOrderLinesAdapterLayoutBinding
import com.wms.panchkula.Retrofit_Api.ApiConstantForURL
import com.wms.panchkula.ui.home.HomeActivity
import com.wms.panchkula.ui.inventoryOrder.Model.InventoryRequestModel
import com.wms.panchkula.ui.invoiceOrder.UI.InventoryTransferLinesActivity
import com.wms.panchkula.ui.issueForProductionOrder.Adapter.BatchItemsAdapter
import com.wms.panchkula.ui.issueForProductionOrder.Model.ScanedOrderBatchedItems
import com.wms.panchkula.ui.issueForProductionOrder.UI.qrScannerUi.QRScannerActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.gson.GsonBuilder
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.IOException
import java.sql.*


class DemoAdapter(private val context: Context, var list: ArrayList<InventoryRequestModel.StockTransferLines>, private val networkConnection: NetworkConnection,
                                   private val materialProgressDialog: MaterialProgressDialog, private val callback: AdapterCallback, private val save: Chip) : RecyclerView.Adapter<DemoAdapter.ViewHolder>(), BatchItemsAdapter.OnDeleteItemClickListener {

    //todo declaration..
    private var connection: Connection? = null
    val REQUEST_CODE = 100
    private lateinit var sessionManagement: SessionManagement
    var hashMap: HashMap<String, ArrayList<ScanedOrderBatchedItems.Value>> = HashMap<String, ArrayList<ScanedOrderBatchedItems.Value>>()
    var quantityHashMap: HashMap<String, ArrayList<String>> = HashMap<String, ArrayList<String>>()
//    var serialQuantityHashMap: HashMap<String, ArrayList<String>> = HashMap<String, ArrayList<String>>()
//    var noneQuantityHashMap: HashMap<String, ArrayList<String>> = HashMap<String, ArrayList<String>>()
    private var scanedBatchedItemsList_gl: ArrayList<ScanedOrderBatchedItems.Value> = ArrayList()
    private lateinit var recyclerView: RecyclerView
    private var pos: Int = 0
    private var itemCode = ""
    var batchItemsAdapter: BatchItemsAdapter? = null
    lateinit var tvTotalScanQty: TextView
    lateinit var tvTotalScanGW: TextView
    lateinit var tvOpenQty : TextView

    init {
//        setSqlServer()
        sessionManagement = SessionManagement(context)
    }

    private var totalScanGW: String = ""

    //todo interfaces...
    interface AdapterCallback {

        /*    fun onApiResponseStock(
                response: java.util.HashMap<String, java.util.ArrayList<ScanedOrderBatchedItems.Value>>,
                list: java.util.ArrayList<InventoryRequestModel.StockTransferLines>,quantityResponse: HashMap<String, ArrayList<String>>, serialQuantityResponse: HashMap<String, ArrayList<String>>
            )*/

        fun onApiResponseStock(response: java.util.HashMap<String, ArrayList<ScanedOrderBatchedItems.Value>>,
                               list: ArrayList<InventoryRequestModel.StockTransferLines>, quantityResponse: HashMap<String, ArrayList<String>>        )
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ProductionOrderLinesAdapterLayoutBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        with(holder) {
            with(list[position]) {

                binding.tvItemCode.text = ":   "+this.ItemCode
                binding.tvItemName.text = ":   "+this.ItemDescription
                var qty = this.PlannedQuantity - this.IssuedQuantity
                binding.tvOpenQty.text = ":   "+this.RemainingOpenQuantity

                tvTotalScanQty = binding.tvTotalScannQty

//                tvTotalScanGW = binding.tvTotalScanGw

                totalScanGW = when {
                    this.Batch == "Y" && this.Serial == "N" && this.None == "N" -> "Batch"
                    this.Serial == "Y" && this.Batch == "N" && this.None == "N" -> "Serial"
                    this.None == "Y" && this.Batch == "N" && this.Serial == "N" -> "None"
                    else -> "None"
                }

                binding.tvTotalScanGw.text = "$totalScanGW"

                /*    if (this.Batch == "Y" && this.Serial == "N" && this.None == "N"){
                        binding.tvTotalScanGw.text = ":   Batch"
                        tvTotalScanGW.text = "Batch"
                    }
                    else if (this.Serial == "Y" && this.Batch == "N" && this.None == "N") {
                        binding.tvTotalScanGw.text = ":   Serial"
                        tvTotalScanGW.text = "Serial"
                    }
                    else if (this.None == "Y" && this.Batch == "N" && this.Serial == "N"){
                        binding.tvTotalScanGw.text = ":   None"
                        tvTotalScanGW.text = "None"
                    }*/
//                binding.tvGsm.text = ":   "+ gsm.toString()

                //todo add adapter size in hashmap at once.
                var count = 0
                for (i in 0 until list.size){
                    //TODO set count adapter position size store in list for batch scan...
                    var itemList_gl: ArrayList<ScanedOrderBatchedItems.Value> = ArrayList()
                    if (hashMap.size != list.size) {
                        hashMap.put("Item" + count, itemList_gl)
                    }

                    //TODO set count adapter position size store in list for batch quantity...
                    var stringList: ArrayList<String> = ArrayList()
                    if (quantityHashMap.size != list.size) {
                        quantityHashMap.put("Item" + count, stringList)
                    }


                   /* var serialStringList: ArrayList<String> = ArrayList()
                    if (serialQuantityHashMap.size != list.size) {
                        serialQuantityHashMap.put("Item" + count, serialStringList)
                    }

                    var noneStringList: ArrayList<String> = ArrayList()
                    if (noneQuantityHashMap.size != list.size) {
                        noneQuantityHashMap.put("Item" + count, noneStringList)
                    }*/
                    count ++
                }
                Log.e("count ===> ", count.toString())


                Log.d("scanner_type===>", sessionManagement.getScannerType(context).toString())

                //todo if leaser type choose..

                if (sessionManagement.getScannerType(context) == "LEASER") { //sessionManagement.getLeaserCheck()!! == 1 && sessionManagement.getQRScannerCheck()!! == 0
                    binding.ivScanBatchCode.visibility = View.GONE

                    tvTotalScanGW = binding.tvTotalScanGw

                    binding.edBatchCodeScan.requestFocus()

                    //todo HIDE
                    Handler(Looper.getMainLooper()).postDelayed({
                        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                        if (imm != null && binding.edBatchCodeScan != null) {
                            imm.hideSoftInputFromWindow(binding.edBatchCodeScan.windowToken, 0)
                        }
                    }, 200)


                    binding.edBatchCodeScan.addTextChangedListener(object : TextWatcher {
                        override fun afterTextChanged(s: Editable?) {
                            itemCode = list[position].ItemCode

                            recyclerView = binding.rvBatchItems
                            tvOpenQty = binding.tvOpenQty
                            tvTotalScanQty = binding.tvTotalScannQty
                            tvTotalScanGW.setText(totalScanGW)

                            // Fetch the text from EditText after text has changed
                            val text = s.toString().trim()


                            if (text.isNotEmpty()) {
                                try {
                                    if (binding.tvTotalScanGw.text.equals("Batch")){
                                        //todo getting QR code on 2 index
                                        val batch = text.split(",")[1]
                                        Log.e("text====>", "afterTextChanged: "+batch )


                                        if (checkDuplicate(hashMap.get("Item" + position)!!, batch)) {

                                            //todo scan call api here...
                                            scanBatchLinesItem(batch, binding.rvBatchItems, holder.adapterPosition,  itemCode, binding.tvOpenQty,  binding.tvTotalScannQty, binding.tvTotalScanGw)
                                        }
                                    }else if (binding.tvTotalScanGw.text.equals("Serial")){
                                        //todo getting QR code on 2 index
                                        val batch = text.split(",")[1]
                                        Log.e("text====>", "afterTextChanged: "+batch )


                                        if (checkDuplicateForSerial(hashMap.get("Item" + position)!!, batch)) {

                                            //todo scan call api here...
                                            scanSerialLineItem(batch, binding.rvBatchItems, holder.adapterPosition,  itemCode, binding.tvOpenQty,  binding.tvTotalScannQty, binding.tvTotalScanGw)
                                        }
                                    }
                                    else if (binding.tvTotalScanGw.text.equals("None") || binding.tvTotalScanGw.text.equals("NONE")){
                                        var scanItem = text
                                        val parts = text.split("~")

                                        val lastPart = parts.last()
                                        var itemCode = parts[2]
                                        itemDesc = parts[3]

                                        type = lastPart
                                        if (checkDuplicateForNone(hashMap.get("Item" + position)!!, text)) {
                                            //todo scan call api here...
                                            callNoneBindFunction(itemCode, binding.rvBatchItems, holder.adapterPosition, tvTotalScanQty, itemDesc, scanItem)
                                        }
                                    }


                                    // Clear the EditText and request focus
                                    binding.edBatchCodeScan.setText("")
                                    binding.edBatchCodeScan.requestFocus()

                                    Handler(Looper.getMainLooper()).postDelayed({
                                        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                                        imm?.hideSoftInputFromWindow(binding.edBatchCodeScan.windowToken, 0)
                                    }, 200)
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                        }

                        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                            // No action needed here
                        }

                        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                            // No action needed here
                        }
                    })


                }


                //todo is qr scanner type choose..
                else if (sessionManagement.getScannerType(context) == "QR_SCANNER" || sessionManagement.getScannerType(context) == null) { //|| sessionManagement.getLeaserCheck()!! == 0 && sessionManagement.getQRScannerCheck()!! == 1 || sessionManagement.getLeaserCheck()!! == 0 && sessionManagement.getQRScannerCheck()!! == 0
                    binding.ivScanBatchCode.visibility = View.VISIBLE

                    //TODO click on barcode scanner for popup..
                    binding.ivScanBatchCode.setOnClickListener {
                        var text = binding.edBatchCodeScan.text.toString().trim()
                        recyclerView = binding.rvBatchItems
                        itemCode = this.ItemCode.toString()
                        tvOpenQty = binding.tvOpenQty
                        tvTotalScanQty = binding.tvTotalScannQty

                        tvTotalScanGW = binding.tvTotalScanGw
                        itemCode = this.ItemCode.toString()

                        if (sessionManagement.getScannerType(context) == null) {
                            showPopupNotChooseScanner()
                        }
                        else if (sessionManagement.getScannerType(context) == "QR_SCANNER") {
                            val intent = Intent(context, QRScannerActivity::class.java)
//                            pos = adapterPosition
                            pos = setScanDataOnItem(list, list.get(adapterPosition).ItemCode)
                            (context as InventoryTransferLinesActivity).startActivityForResult(intent, REQUEST_CODE)
                        }

                    }


                }


                //TODO save order lines listener by interface...
                save.setOnClickListener {
                    for (i in 0 until list.size) {
                        if (list[i].isScanned != 0){
                            AppConstants.IS_SCAN = true
                        }
                    }
                    Log.e(ContentValues.TAG, "isItemScan ==> : "+ AppConstants.IS_SCAN )
                    if (AppConstants.IS_SCAN == false ){
                        save.isEnabled = false
                        save.isCheckable = false
                        GlobalMethods.showError(context, "Items Not Scan.")
                    }else{

                        save.isEnabled = false
                        save.isCheckable = false
//                        callback.onApiResponseStock(hashMap, list, quantityHashMap, serialQuantityHashMap, noneQuantityHashMap)
                        callback.onApiResponseStock(hashMap, list, quantityHashMap)
                    }


                }

            }
        }
    }


    init {
        setHasStableIds(true)
    }


    override fun getItemCount(): Int {
        return list.size
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getItemViewType(position: Int): Int {
        return position
    }


    //TODO viewholder...
    class ViewHolder(val binding: ProductionOrderLinesAdapterLayoutBinding) : RecyclerView.ViewHolder(binding.root)


    var hashMapBatchList: java.util.HashMap<String, ArrayList<ScanedOrderBatchedItems.Value>> = java.util.HashMap<String, ArrayList<ScanedOrderBatchedItems.Value>>()
    var hashmapBatchQuantityList: java.util.HashMap<String, ArrayList<String>> = java.util.HashMap<String, ArrayList<String>>()
//    var serialHashMapQuantityList: java.util.HashMap<String, ArrayList<String>> = java.util.HashMap<String, ArrayList<String>>()
//    var noneHashMapQuantityList: java.util.HashMap<String, ArrayList<String>> = java.util.HashMap<String, ArrayList<String>>()


    override fun onDeleteItemClick(list: ArrayList<ScanedOrderBatchedItems.Value>, quantityHashMap1: ArrayList<String>, pos: Int, parentPosition : Int, rvBatchItems : RecyclerView) {

        var batch = ""

        if (!list[pos].Batch.isNullOrEmpty()) {
            batch = list[pos].Batch
        }
        else if (!list[pos].SerialNumber.isNullOrEmpty()) {
            batch = list[pos].SerialNumber
        }

        MaterialAlertDialogBuilder(context)
            .setTitle("Confirm...")
            .setMessage("Do you want to delete " + batch + " Item .")
            .setIcon(R.drawable.ic_trash)
            .setPositiveButton("Confirm",
                DialogInterface.OnClickListener { dialogInterface: DialogInterface?, i1: Int ->
                    Log.e("before_valueList===>", list.size.toString())
                    Log.e("before_batch===>", quantityHashMap1.size.toString())
                    Log.e("quantityHashMap===>", quantityHashMap.get("Item" + pos)!!.size.toString())

                    hashMapBatchList = hashMap
                    hashmapBatchQuantityList = quantityHashMap
                    Log.e("hashmap--->", hashMapBatchList.toString())
                    Log.e("befQuantityHashMap-->", quantityHashMap.toString())

                    list.removeAt(pos)
//                    quantityHashMap1.removeAt(pos)

                    var data = GlobalMethods.sumBatchQuantity(pos, quantityHashMap1!!).toString()
                    Log.e("data_value===>", data.toString())
                    tvTotalScanQty.setText(":   "+data)
                    hashMapBatchList = hashMap
                    hashmapBatchQuantityList = quantityHashMap
                    Log.e("hashmap--->", hashMapBatchList.toString())
                    Log.e("aftQuantityHashMap-->", hashmapBatchQuantityList.toString())

                    Log.e("parentPosition==>", "parentPosition: "+parentPosition )
                    batchItemsAdapter?.notifyDataSetChanged()
                    notifyDataSetChanged()

                })
            .setNegativeButton("Cancel",
                DialogInterface.OnClickListener { dialogInterface, i ->
                    dialogInterface.dismiss()
                })

            .show()
    }



    //TODO duplicatcy checking from list...
    fun checkDuplicate(scanedBatchedItemsList_gl: ArrayList<ScanedOrderBatchedItems.Value>, batchCode: String): Boolean {
        var startus: Boolean = true;
        for (items in scanedBatchedItemsList_gl) {
            if (items.Batch.equals(batchCode)) {
                startus = false
                Toast.makeText(context, "Batch no. Already Exists!", Toast.LENGTH_SHORT).show()
            }
        }
        return startus
    }


    fun checkDuplicateForSerial(scanedBatchedItemsList_gl: ArrayList<ScanedOrderBatchedItems.Value>, batchCode: String): Boolean {
        var startus: Boolean = true;
        for (items in scanedBatchedItemsList_gl) {
            if (items.SerialNumber.equals(batchCode)) {
                startus = false
                Toast.makeText(context, "Serial no. Already Exists!", Toast.LENGTH_SHORT).show()
            }
        }
        return startus
    }

    fun checkDuplicateForNone(scanedBatchedItemsList_gl: ArrayList<ScanedOrderBatchedItems.Value>, batchCode: String): Boolean {
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
        arrayList: java.util.ArrayList<InventoryRequestModel.StockTransferLines>,
        itemCode: String
    ): Int {

        var position = -1
        for ((index, item) in arrayList.withIndex()) {
            if (item is InventoryRequestModel.StockTransferLines && item.ItemCode == itemCode) {
                position = index
                break
            }
        }
        return position

    }


    //TODO scan item lines api here....
    private fun scanBatchLinesItem(text: String, rvBatchItems: RecyclerView, position: Int, itemCode: String?, tvOpenQty: TextView, tvTotalScannQty: TextView, tvTotalScanGw: TextView) {
        if (networkConnection.getConnectivityStatusBoolean(context)) {
            materialProgressDialog.show()
            var apiConfig = ApiConstantForURL()

            NetworkClients.updateBaseUrlFromConfig(apiConfig)

            QuantityNetworkClient.updateBaseUrlFromConfig(apiConfig)

            val networkClient = NetworkClients.create(context)
            networkClient.doGetBatchNumScanDetails("Batch eq '" + text + "'" + " and ItemCode eq '" + itemCode + "'")
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
                                    Log.e("itemPo=>", itemPo.toString())

                                    var totalScanQty = tvTotalScannQty.text.toString()
                                    var total = totalScanQty.toIntOrNull() ?: 0

                                    if (itemPo == -1) {
                                        GlobalMethods.showError(context, "Item Code not matched")
                                    }else if (total >= list[itemPo].RemainingOpenQuantity.toDouble()) {
                                        GlobalMethods.showError(context, "Scanning completed for this Item")
                                    }else{
                                        if (responseModel.value.size > 0 && !responseModel.value.isNullOrEmpty()) {

                                            var modelResponse = responseModel.value
                                            scanedBatchedItemsList_gl.addAll(modelResponse)

                                            var itemList_gl: ArrayList<ScanedOrderBatchedItems.Value> = ArrayList()
                                            itemList_gl.addAll(hashMap.get("Item" + position)!!)
                                            //itemList_gl= hashMap.get("Item"+position)!!
                                            var stringList: ArrayList<String> = ArrayList()
                                            stringList.addAll(quantityHashMap.get("Item" + position)!!)

                                            itemList_gl.add(responseModel.value[0])

//                                            hashMap.put("Item" + position, itemList_gl)


                                            if (!itemList_gl.isNullOrEmpty()) {

                                                Log.e("list_size-----", itemList_gl.size.toString())

                                                //todo quantity..

                                                getQuantityFromApi(text, itemList_gl[0].ItemCode, position, stringList, tvOpenQty, tvTotalScannQty,tvTotalScanGw, rvBatchItems, itemList_gl )

                                            }
                                        }

                                        else {

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


    private fun setAdapter(position: Int, rvBatchItems: RecyclerView, RemainingOpenQuantity: Double, stringList: ArrayList<String>, tvTotalScannQty: TextView, flag: String) {

        if (flag.equals("BatchType")){
            batchItemsAdapter = BatchItemsAdapter(context, hashMap.get("Item" + position)!!,  quantityHashMap.get("Item" + position)!!, "IssueOrder", position, rvBatchItems) { newQuantity, pos , tvBatchQuantity->
                Log.e("Quantity===> ", "onResponse: $newQuantity")
                var QUANTITYVAL = newQuantity

                val value = QUANTITYVAL.toIntOrNull() ?: 0

                if (QUANTITYVAL.isEmpty()){
                    QUANTITYVAL = "0"
                }

                if (!QUANTITYVAL.isNullOrEmpty()) {
                    if (RemainingOpenQuantity != null && value > RemainingOpenQuantity) {
                        GlobalMethods.showError(context, "Value cannot exceed then Open Quantity")
                        tvBatchQuantity.setText("")
                    }
                    else {
                        if (stringList.size > pos) {
                            stringList[pos] = QUANTITYVAL
                        } else {
                            stringList.add(QUANTITYVAL)
                        }

                        quantityHashMap.put("Item" + position, stringList)

                        val list = quantityHashMap["Item$position"]?.toMutableList() ?: mutableListOf()

                        quantityHashMap["Item$position"] = list as java.util.ArrayList<String>

                        tvTotalScannQty.text = "${GlobalMethods.sumBatchQuantity(pos, list)}"


                    }

                }


            }

        }

        else if (flag.equals("SerialType")){
            batchItemsAdapter = BatchItemsAdapter(context, hashMap.get("Item" + position)!!, quantityHashMap.get("Item" + position)!!, "SerialQR", position, rvBatchItems)
        }

    }


    //TODO scan item lines api here....
    private fun getQuantityFromApi(batchCode: String, itemCode: String, position: Int, stringList: ArrayList<String>,
                                   tvOpenQty: TextView, tvTotalScannQty: TextView, tvTotalScanGw: TextView, rvBatchItems: RecyclerView, itemList_gl: ArrayList<ScanedOrderBatchedItems.Value>) {
        if (networkConnection.getConnectivityStatusBoolean(context)) {
            materialProgressDialog.show()
            var apiConfig = ApiConstantForURL()

            NetworkClients.updateBaseUrlFromConfig(apiConfig)

            QuantityNetworkClient.updateBaseUrlFromConfig(apiConfig)

            val networkClient = QuantityNetworkClient.create(context)
            networkClient.getQuantityValue(sessionManagement.getCompanyDB(context)!!,batchCode, itemCode, sessionManagement.getWarehouseCode(context)!!)//AppConstants.COMPANY_DB,
                .apply {
                    enqueue(object : Callback<GetQuantityModel> {
                        override fun onResponse(call: Call<GetQuantityModel>, response: Response<GetQuantityModel>) {
                            try {
                                materialProgressDialog.dismiss()
                                if (response.isSuccessful) {
                                    Log.e("response---------", response.body().toString())


                                    var responseModel = response.body()!!
                                    if (!responseModel.value.isNullOrEmpty() && !responseModel.value[0].Quantity.isNullOrEmpty() && !responseModel.value[0].Quantity.equals("0.0")) {

                                        var Quantity = responseModel.value[0].Quantity.toDoubleOrNull()
                                        var RemainingOpenQuantity = list[itemPo].RemainingOpenQuantity.toDoubleOrNull()

                                        if (Quantity!! > RemainingOpenQuantity!!){

                                            Log.e("stringList", "Success=>" + responseModel.value)

                                            scanCount = list[itemPo].isScanned
                                            ++scanCount
                                            list[itemPo].isScanned = scanCount


                                            hashMap.put("Item" + position, itemList_gl)

                                            stringList.add(responseModel.value[0].Quantity)

                                            quantityHashMap.put("Item" + position, stringList)


                                            tvTotalScannQty.text = "${GlobalMethods.sumBatchQuantity(position, stringList)}"

                                            if (stringList.isNotEmpty() && !stringList.contains("0")) {
                                                val layoutManager: RecyclerView.LayoutManager = LinearLayoutManager(context)
                                                rvBatchItems.layoutManager = layoutManager

//                                                setAdapter(position, rvBatchItems,RemainingOpenQuantity,stringList,tvTotalScannQty, "BatchType")

                                                batchItemsAdapter = BatchItemsAdapter(context, hashMap.get("Item" + position)!!,  quantityHashMap.get("Item" + position)!!, "IssueOrder", position, rvBatchItems) { newQuantity, pos , tvBatchQuantity->
                                                    Log.e("Quantity===> ", "onResponse: $newQuantity")
                                                    var QUANTITYVAL = newQuantity

                                                    val value = QUANTITYVAL.toIntOrNull() ?: 0

                                                    if (QUANTITYVAL.isEmpty()){
                                                        QUANTITYVAL = "0"
                                                    }

                                                    if (!QUANTITYVAL.isNullOrEmpty()) {
                                                        if (RemainingOpenQuantity != null && value > RemainingOpenQuantity) {
                                                            GlobalMethods.showError(context, "Value cannot exceed then Open Quantity")
                                                            tvBatchQuantity.setText("")
                                                        }
                                                        else {
                                                            if (stringList.size > pos) {
                                                                stringList[pos] = QUANTITYVAL
                                                            } else {
                                                                stringList.add(QUANTITYVAL)
                                                            }

                                                            quantityHashMap.put("Item" + position, stringList)

                                                            val list = quantityHashMap["Item$position"]?.toMutableList() ?: mutableListOf()

                                                            quantityHashMap["Item$position"] = list as java.util.ArrayList<String>

                                                            tvTotalScannQty.text = "${GlobalMethods.sumBatchQuantity(pos, list)}"


                                                        }

                                                    }


                                                }

                                                hashMapBatchList = hashMap
                                                hashmapBatchQuantityList = quantityHashMap
                                                Log.e("hashmap--->", hashMapBatchList.toString())
                                                Log.e("batchQuantityList-->", hashmapBatchQuantityList.toString())

                                                batchItemsAdapter?.setOnDeleteItemClickListener(this@DemoAdapter)
                                                rvBatchItems.adapter = batchItemsAdapter

                                                batchItemsAdapter?.notifyDataSetChanged()
                                            }

                                            else {
                                                batchItemsAdapter?.notifyDataSetChanged()
                                                GlobalMethods.showError(context, "Batch / Roll No. has zero Quantity of this PO.")
                                            }

                                        }

                                        else if (Quantity!! <= RemainingOpenQuantity!!){

                                            Log.e("stringList", "Success=>" + responseModel.value)

                                            scanCount = list[itemPo].isScanned
                                            ++scanCount;
                                            list[itemPo].isScanned = scanCount


                                            hashMap.put("Item" + position, itemList_gl)

                                            stringList.add(responseModel.value[0].Quantity)

                                            quantityHashMap.put("Item" + position, stringList)


                                            tvTotalScannQty.text = "${GlobalMethods.sumBatchQuantity(position, stringList)}"

                                            if (stringList.isNotEmpty() && !stringList.contains("0")) {
                                                val layoutManager: RecyclerView.LayoutManager = LinearLayoutManager(context)
                                                rvBatchItems.layoutManager = layoutManager

//                                                setAdapter(position, rvBatchItems,RemainingOpenQuantity,stringList,tvTotalScannQty, "BatchType")

                                                batchItemsAdapter = BatchItemsAdapter(context, hashMap.get("Item" + position)!!,  quantityHashMap.get("Item" + position)!!, "IssueOrder", position, rvBatchItems) { newQuantity, pos  , tvBatchQuantity->
                                                    Log.e("Quantity===> ", "onResponse: $newQuantity")
                                                    var QUANTITYVAL = newQuantity

                                                    val value = QUANTITYVAL.toIntOrNull() ?: 0

                                                    if (QUANTITYVAL.isEmpty()){
                                                        QUANTITYVAL = "0"
                                                    }

                                                    if (!QUANTITYVAL.isNullOrEmpty()) {
                                                        if (RemainingOpenQuantity != null && value > RemainingOpenQuantity) {
                                                            GlobalMethods.showError(context, "Value cannot exceed then Open Quantity")
                                                            tvBatchQuantity.setText("")
                                                        }
                                                        else {
                                                            if (stringList.size > pos) {
                                                                stringList[pos] = QUANTITYVAL
                                                            } else {
                                                                stringList.add(QUANTITYVAL)
                                                            }

                                                            quantityHashMap.put("Item" + position, stringList)

                                                            val list = quantityHashMap["Item$position"]?.toMutableList() ?: mutableListOf()

                                                            quantityHashMap["Item$position"] = list as java.util.ArrayList<String>

                                                            tvTotalScannQty.text = "${GlobalMethods.sumBatchQuantity(pos, list)}"


                                                        }

                                                    }


                                                }

                                                hashMapBatchList = hashMap
                                                hashmapBatchQuantityList = quantityHashMap
//                                                serialHashMapQuantityList = serialQuantityHashMap
                                                Log.e("hashmap--->", hashMapBatchList.toString())
                                                Log.e("batchQuantityList-->", hashmapBatchQuantityList.toString())
//                                                Log.e("serialQuantity-->", serialQuantityHashMap.toString())


                                                batchItemsAdapter?.setOnDeleteItemClickListener(this@DemoAdapter)
                                                rvBatchItems.adapter = batchItemsAdapter

                                                batchItemsAdapter?.notifyDataSetChanged()
                                            }

                                            else {
                                                batchItemsAdapter?.notifyDataSetChanged()
                                                GlobalMethods.showError(context, "Batch / Roll No. has zero Quantity of this PO.")
                                            }

                                        }


                                    }

                                    else {
                                        GlobalMethods.showError(context, "No Quantity")
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

                        override fun onFailure(call: Call<GetQuantityModel>, t: Throwable) {
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



    private fun getQuantityFromApi123(batchCode: String, itemCode: String, position: Int, stringList: ArrayList<String>,
                                      tvOpenQty: TextView, tvTotalScannQty: TextView, tvTotalScanGw: TextView, rvBatchItems: RecyclerView, itemList_gl: ArrayList<ScanedOrderBatchedItems.Value>) {
        if (networkConnection.getConnectivityStatusBoolean(context)) {
            materialProgressDialog.show()
            var apiConfig = ApiConstantForURL()

            NetworkClients.updateBaseUrlFromConfig(apiConfig)

            QuantityNetworkClient.updateBaseUrlFromConfig(apiConfig)

            val networkClient = QuantityNetworkClient.create(context)
            networkClient.getQuantityValue(sessionManagement.getCompanyDB(context)!!,batchCode, itemCode, sessionManagement.getWarehouseCode(context)!!)//AppConstants.COMPANY_DB,
                .apply {
                    enqueue(object : Callback<GetQuantityModel> {
                        override fun onResponse(call: Call<GetQuantityModel>, response: Response<GetQuantityModel>) {
                            try {
                                materialProgressDialog.dismiss()
                                if (response.isSuccessful) {
                                    Log.e("response---------", response.body().toString())

                                    var responseModel = response.body()!!
                                    if (responseModel.value.size > 0 && !responseModel.value.isNullOrEmpty()) {

                                        Log.e("stringList", "Success=>" + responseModel.value)
                                        stringList.add(responseModel.value[0].Quantity)
                                        quantityHashMap.put("Item" + position, stringList)

                                        //TODO sum of quantity of batches..
                                        tvTotalScannQty.text = GlobalMethods.sumBatchQuantity(position, quantityHashMap.get("Item" + position)!!).toString()


                                        if (quantityHashMap.get("Item" + position)!!.size > 0) {
                                            if (!quantityHashMap.get("Item" + position)!!.contains("0") ) {
                                                hashMap.put("Item" + position, itemList_gl)
                                                val layoutManager: RecyclerView.LayoutManager = LinearLayoutManager(context)
                                                rvBatchItems.layoutManager = layoutManager

                                                batchItemsAdapter = BatchItemsAdapter(context, hashMap.get("Item" + position)!!, quantityHashMap.get("Item" + position)!!, "IssueOrder", position, rvBatchItems){ quantity, pos , tvBatchQuantity ->

                                                    Log.e("Quantity===> ", "onResponse: " +quantity )
                                                    stringList.add(quantity)
                                                    quantityHashMap.put("Item" + position, stringList)

                                                    tvTotalScannQty.text = ":   "+GlobalMethods.sumBatchQuantity(position, quantityHashMap.get("Item" + position)!!).toString()


                                                }

                                                //todo call setOnItemListener Interface Function...
                                                batchItemsAdapter?.setOnDeleteItemClickListener(this@DemoAdapter)
                                                rvBatchItems.adapter = batchItemsAdapter
                                                batchItemsAdapter?.notifyDataSetChanged()

                                                //todo comment not required---
                                                /*var totalGrossWeight = GlobalMethods.changeDecimal(GlobalMethods.sumBatchGrossWeight(position, hashMap.get("Item" + position)!!).toString())
                                                tvTotalScanGw.text = ":   "+ totalGrossWeight*/


                                            }else{
                                                batchItemsAdapter?.notifyDataSetChanged()
                                                GlobalMethods.showError(context, "Batch / Roll No. has zero Quantity of this PO.")
                                            }
                                        }else{
                                            batchItemsAdapter?.notifyDataSetChanged()
                                            GlobalMethods.showError(context, "No Quantity Found of this Production Order.")
                                        }

                                    } else {
                                        GlobalMethods.showError(context, "No Quantity")
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

                        override fun onFailure(call: Call<GetQuantityModel>, t: Throwable) {
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
    private fun scanSerialLineItem(text: String, rvBatchItems: RecyclerView, position: Int, itemCode: String?, tvOpenQty: TextView, tvTotalScannQty: TextView, tvTotalScanGw: TextView) {
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

                                    if (responseModel.value.size > 0 && !responseModel.value.isNullOrEmpty()) {

                                        Log.e("ItemCode==>", "" + responseModel.value[0].ItemCode)
                                        itemPo = setScanDataOnItem(list, responseModel.value[0].ItemCode)
                                        Log.e("ItemPo==>", "" + itemPo)
                                    }
                                    Log.e("itemPo=>", itemPo.toString())


                                    var totalScanQty = tvTotalScannQty.text.toString()
                                    var total = totalScanQty.toIntOrNull() ?: 0

                                    if (itemPo == -1) {
                                        GlobalMethods.showError(context, "Item Code not matched")
                                    } else if (scanCount >= list[itemPo].RemainingOpenQuantity.toDouble()) {
                                        GlobalMethods.showError(context, "Scanning completed for this Item")
                                    }
                                    else{
                                        if (responseModel.value.size > 0 && !responseModel.value.isNullOrEmpty()) {

                                            scanCount = list[itemPo].isScanned
                                            ++scanCount;
                                            list[itemPo].isScanned = scanCount


                                            var modelResponse = responseModel.value
                                            scanedBatchedItemsList_gl.addAll(modelResponse)

                                            var itemList_gl: ArrayList<ScanedOrderBatchedItems.Value> = ArrayList()
                                            itemList_gl.addAll(hashMap.get("Item" + position)!!)
                                            //itemList_gl= hashMap.get("Item"+position)!!

                                            var stringList: ArrayList<String> = ArrayList()
                                            stringList.addAll(quantityHashMap.get("Item" + position)!!)

                                            itemList_gl.add(responseModel.value[0])


                                            if (!itemList_gl.isNullOrEmpty()) {

                                                Log.e("list_size-----", itemList_gl.size.toString())

                                                //todo quantity..

                                                stringList.add("1")
                                                quantityHashMap.put("Item" + position, stringList)

                                                //TODO sum of quantity of batches..
                                                tvTotalScannQty.text = ":   "+GlobalMethods.sumBatchQuantity(position, quantityHashMap.get("Item" + position)!!).toString()

                                                hashMap.put("Item" + position, itemList_gl)

                                                val layoutManager: RecyclerView.LayoutManager = LinearLayoutManager(context)
                                                rvBatchItems.layoutManager = layoutManager

//                                                setAdapter(position, rvBatchItems,0.0,stringList,tvTotalScannQty, "SerialType")
                                                batchItemsAdapter = BatchItemsAdapter(context, hashMap.get("Item" + position)!!, quantityHashMap.get("Item" + position)!!, "SerialQR", position, rvBatchItems)


                                                hashMapBatchList = hashMap
                                                hashmapBatchQuantityList = quantityHashMap
                                                Log.e("hashmap--->", hashMapBatchList.toString())
                                                Log.e("batchQuantityList-->", hashmapBatchQuantityList.toString())

                                                //todo call setOnItemListener Interface Function...
                                                batchItemsAdapter?.setOnDeleteItemClickListener(this@DemoAdapter)
                                                rvBatchItems.adapter = batchItemsAdapter
                                                batchItemsAdapter?.notifyDataSetChanged()
                                            }
                                        }

                                        else {
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


    private fun callNoneBindFunction(
        itemCode: String,
        rvBatchItems: RecyclerView,
        position: Int,
        tvTotalScannQty: TextView,
        itemDesc: String,
        scanItem: String
    ) {


        if (itemCode.isNotEmpty()) {

            if (itemCode.isNotEmpty()) {

                Log.e("ItemCode==>", "" + itemCode)
                itemPo = setScanDataOnItem(list, itemCode)
                Log.e("ItemPo==>", "" + itemPo)
            }

            Log.e("itemPo=>", itemPo.toString())
            var totalScanQty = tvTotalScannQty.text.toString()
            var total = totalScanQty.toIntOrNull() ?: 0


            if (itemPo == -1) {
                GlobalMethods.showError(context, "Item Code not matched")
            } else if (scanCount >= list[itemPo].RemainingOpenQuantity.toDouble()) {
                GlobalMethods.showError(context, "Scanning completed for this Item")
            }

            else{
                scanCount = list[itemPo].isScanned
                ++scanCount
                list[itemPo].isScanned = scanCount

                var data = ScanedOrderBatchedItems.Value("", itemCode, itemDesc, "", "", scanItem,"", "", "", "", "", "", "", 0, "", 0.0, 0.0, 0.0, 0.0, 0.0, "", 0.0, "", 0.0, 0.0, "")

                var modelResponse = data
                scanedBatchedItemsList_gl.addAll(listOf(modelResponse))

                var itemList_gl: ArrayList<ScanedOrderBatchedItems.Value> = ArrayList()
                itemList_gl.addAll(hashMap.get("Item" + position)!!)
                //itemList_gl= hashMap.get("Item"+position)!!

                var stringList: ArrayList<String> = ArrayList()
                stringList.addAll(quantityHashMap.get("Item" + position)!!)

                itemList_gl.add(data)

                var RemainingOpenQuantity = list[itemPo].RemainingOpenQuantity.toDoubleOrNull()


                if (!itemList_gl.isNullOrEmpty()) {

                    Log.e("list_size-----", itemList_gl.size.toString())

                    //todo quantity..

                    stringList.add("0")

                    quantityHashMap.put("Item" + position, stringList)

                    //TODO sum of quantity of batches..
                    tvTotalScannQty.text = ":   "+GlobalMethods.sumBatchQuantity(position, quantityHashMap.get("Item" + position)!!).toString()

                    hashMap.put("Item" + position, itemList_gl)

                    val layoutManager: RecyclerView.LayoutManager = LinearLayoutManager(context)
                    rvBatchItems.layoutManager = layoutManager

                    batchItemsAdapter = BatchItemsAdapter(context, hashMap.get("Item" + position)!!, quantityHashMap.get("Item" + position)!!, "NoneQR", position, rvBatchItems){ newQuantity, pos  , tvBatchQuantity->
                        Log.e("Quantity===> ", "onResponse: $newQuantity")
                        var QUANTITYVAL = newQuantity

                        val value = QUANTITYVAL.toIntOrNull() ?: 0

                        if (QUANTITYVAL.isEmpty()){
                            QUANTITYVAL = "0"
                        }

                        if (!QUANTITYVAL.isNullOrEmpty()) {
                            if (RemainingOpenQuantity != null && value > RemainingOpenQuantity) {
                                GlobalMethods.showError(context, "Value cannot exceed then Open Quantity")
                                tvBatchQuantity.setText("")
                            }
                            else {
                                if (stringList.size > pos) {
                                    stringList[pos] = QUANTITYVAL
                                } else {
                                    stringList.add(QUANTITYVAL)
                                }

                                quantityHashMap.put("Item" + position, stringList)

                                val list = quantityHashMap["Item$position"]?.toMutableList() ?: mutableListOf()

                                // Update the quantityHashMap with the modified list
                                quantityHashMap["Item$position"] = list as ArrayList

                                tvTotalScannQty.text = ":   ${GlobalMethods.sumBatchQuantity(pos, list)}"

                            }

                        }

                    }


                    hashMapBatchList = hashMap
                    hashmapBatchQuantityList = quantityHashMap
                    Log.e("hashmap--->", hashMapBatchList.toString())
                    Log.e("batchQuantityList-->", hashmapBatchQuantityList.toString())

                    //todo call setOnItemListener Interface Function...
                    batchItemsAdapter?.setOnDeleteItemClickListener(this@DemoAdapter)
                    rvBatchItems.adapter = batchItemsAdapter
                    batchItemsAdapter?.notifyDataSetChanged()

                }

            }



        }

    }


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


    var type = ""
    var itemDesc = ""


    open fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            val result = data?.getStringExtra("batch_code")
            var size = 0
            val list = hashMap.get("Item" + pos) as List<*>
            Log.e("size===>", list.size.toString())
            Log.e("ItemCode===>", itemCode)

            //todo spilt string and get string at 0 index...

            //todo set validation for duplicate item
            if (tvTotalScanGW.text.equals("Batch")){
                if (checkDuplicate(hashMap.get("Item" + pos)!!, result.toString().split(",")[1])) {
                    //todo scan call api here...
                    scanBatchLinesItem(result.toString().split(",")[1], recyclerView!!, pos, itemCode, tvOpenQty!!, tvTotalScanQty!!, tvTotalScanGW!!)

                }
            }else if (tvTotalScanGW.text.equals("Serial")){
                if (checkDuplicateForSerial(hashMap.get("Item" + pos)!!, result.toString().split(",")[1])) {
                    //todo scan call api here...
                    scanSerialLineItem(result.toString().split(",")[1], recyclerView!!, pos, itemCode, tvOpenQty!!, tvTotalScanQty!!, tvTotalScanGW!!)
                }
            }  else if (tvTotalScanGW.text.equals("None") || tvTotalScanGW.text.equals("NONE")){
                var scanItem = result.toString().split(",")[0]
                val parts = result.toString().split("~")

                val lastPart = parts.last()
                var itemCode = parts[2]
                itemDesc = parts[3]

                type = lastPart

                if (checkDuplicateForNone(hashMap.get("Item" + pos)!!, scanItem)) {
                    //todo scan call api here...
                    callNoneBindFunction(itemCode, recyclerView, pos, tvTotalScanQty, itemDesc, scanItem)
                }
            }
            else{
                if (tvTotalScanGW.text.isEmpty()){
                    GlobalMethods.showMessage(context, "Scan Type is Empty" )
                }else {
                    GlobalMethods.showMessage(context, "Scan Type is " + tvTotalScanGW.text.toString())
                }
            }


        }
    }


    //TODO set sql server for query...
    private fun setSqlServer() {
        try {
            val url = "jdbc:jtds:sqlserver://" + AppConstants.IP + ":" + AppConstants.PORT + "/" + sessionManagement.getCompanyDB(context)
            ActivityCompat.requestPermissions(context as Activity, arrayOf<String>(Manifest.permission.INTERNET), PackageManager.PERMISSION_GRANTED)
            val policy = ThreadPolicy.Builder().permitAll().build()
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
        }catch (e: IOException){
            e.printStackTrace()
        }

    }

    //TODO connection ..
    private fun connection(connection: Connection) {

        if (connection != null) {
            var statement: Statement? = null
            try {
                statement = connection!!.createStatement()
                // ResultSet resultSet = statement.executeQuery("Select * from CompanyDBs");
                val resultSet: ResultSet =
                    statement.executeQuery("Select Name,CompanyName,Version from CompanyDBs")
                while (resultSet.next()) {
                }
            } catch (e: SQLException) {
                e.printStackTrace()
            }
        } else {
            Log.e("Result=>", "Connection is null")
        }
    }






}
