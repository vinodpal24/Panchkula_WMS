package com.wms.panchkula.ui.issueForProductionOrder.Adapter


import DynamicFieldAdapter
import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.icu.text.DecimalFormat
import android.os.Build
import android.os.Handler
import android.os.Looper

import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.wms.panchkula.Global_Classes.AppConstants
import com.wms.panchkula.Global_Classes.GlobalMethods
import com.wms.panchkula.Global_Classes.MaterialProgressDialog
import com.wms.panchkula.Global_Notification.NetworkConnection
import com.wms.panchkula.Model.GetQuantityModel
import com.wms.panchkula.Model.GetSuggestionQuantity
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
import com.wms.panchkula.ui.issueForProductionOrder.UI.productionOrderLines.ProductionOrderLinesActivity
import com.wms.panchkula.ui.issueForProductionOrder.UI.qrScannerUi.QRScannerActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.gson.GsonBuilder
import com.wms.panchkula.Global_Classes.GlobalMethods.showBinLocationErrorDialog
import com.wms.panchkula.Global_Classes.GlobalMethods.toSimpleJson
import com.wms.panchkula.Model.GetAbsModel
import com.wms.panchkula.Model.ModelBinLocation
import com.wms.panchkula.ui.purchase.model.PurchaseRequestModel

import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.IOException
import java.sql.*


class ProductionOrderLinesAdapter(
    private val context: Context, var list: ArrayList<ProductionListModel.ProductionOrderLine>, private val networkConnection: NetworkConnection,
    private val materialProgressDialog: MaterialProgressDialog, private val callback: AdapterCallback, private val save: Chip, var width: Double?, var length: Double?, var gsm: Double?
) : RecyclerView.Adapter<ProductionOrderLinesAdapter.ViewHolder>(), BatchItemsIssueAdapter.OnDeleteItemClickListener, BatchItemsIssueAdapter.OnScannedItemClickListener {
    var Batchstr = ""

    //todo declaration..
    private var connection: Connection? = null
    val REQUEST_CODE = 100
    private lateinit var sessionManagement: SessionManagement
    var hashMap: HashMap<String, ArrayList<ScanedOrderBatchedItems.Value>> = HashMap<String, ArrayList<ScanedOrderBatchedItems.Value>>()
    var quantityHashMap: HashMap<String, ArrayList<String>> = HashMap<String, ArrayList<String>>()
    var globalQtyList_gl: HashMap<String, ArrayList<String>> = HashMap<String, ArrayList<String>>()
    var serialQuantityHashMap: HashMap<String, ArrayList<String>> = HashMap<String, ArrayList<String>>()
    var noneQuantityHashMap: HashMap<String, ArrayList<String>> = HashMap<String, ArrayList<String>>()

    private var scanedBatchedItemsList_gl: ArrayList<ScanedOrderBatchedItems.Value> = ArrayList()
    private lateinit var recyclerView: RecyclerView
    private var pos: Int = 0
    private var itemCode = ""
    var batchItemsAdapter: BatchItemsIssueAdapter? = null
    lateinit var tvTotalScanQty: TextView
    lateinit var tvTotalScanGW: TextView
    lateinit var tvOpenQty: TextView
    var openQty = 0.0
    var itemTyep = ""

    init {
//        setSqlServer()
        sessionManagement = SessionManagement(context)
    }

    //todo interfaces...
    interface AdapterCallback {
        fun onApiResponse(
            response: HashMap<String, ArrayList<ScanedOrderBatchedItems.Value>>,
            quantityResponse: HashMap<String, ArrayList<String>>, serialQuantityResponse: HashMap<String, ArrayList<String>>,
            noneQuantityResponse: HashMap<String, ArrayList<String>>
        )
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ProductionOrderLinesAdapterLayoutBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    @RequiresApi(Build.VERSION_CODES.N)
    override fun onBindViewHolder(holder: ViewHolder, @SuppressLint("RecyclerView") position: Int) {
        with(holder) {
            with(list[position]) {
                binding.tvItemCode.text = this.ItemNo
                binding.tvItemName.text = this.ItemName
                var qty = this.PlannedQuantity.toDouble() - this.IssuedQuantity.toDouble()
                val decimalFormat = DecimalFormat("#.##")
                qty = decimalFormat.format(qty).toDouble()

                binding.tvOpenQty.text = qty.toString()
                binding.tvWidth.text = ":   " + width.toString()
                binding.tvLength.text = ":   " + length.toString()

                tvTotalScanQty = binding.tvTotalScannQty
//                tvTotalScanGW = binding.tvTotalScanGw
                Log.i("PO_ITEMS", "list.size => ${list.size}")

                if (this.Batch == "Y" && this.Serial == "N" && this.None == "N") {
//                    binding.tvTotalScanGw.text = ":   Batch"
                    binding.tvTotalScanGw.text = "Batch"
//                    tvTotalScanGW.text = "Batch"
                } else if (this.Serial == "Y" && this.Batch == "N" && this.None == "N") {
                    binding.tvTotalScanGw.text = "Serial"
//                    tvTotalScanGW.text = "Serial"
                } else if (this.None == "Y" && this.Batch == "N" && this.Serial == "N") {
                    binding.tvTotalScanGw.text = "None"
//                    tvTotalScanGW.text = "None"
                }
//                binding.tvGsm.text = ":   "+ gsm.toString()

                //todo add adapter size in hashmap at once.
                var count = 0
                for (i in 0 until list.size) {
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


                    var serialStringList: ArrayList<String> = ArrayList()
                    if (serialQuantityHashMap.size != list.size) {
                        serialQuantityHashMap.put("Item" + count, serialStringList)
                    }

                    var globalQtyStringList: ArrayList<String> = ArrayList()
                    if (globalQtyList_gl.size != list.size) {
                        globalQtyList_gl.put("Item" + count, globalQtyStringList)
                    }

                    var noneStringList: ArrayList<String> = ArrayList()
                    if (noneQuantityHashMap.size != list.size) {
                        noneQuantityHashMap.put("Item" + count, noneStringList)
                    }
                    count++
                }
                Log.e("count ===> ", count.toString())


                Log.d("scanner_type===>", sessionManagement.getScannerType(context).toString())


                //todo HIDE
                Handler(Looper.getMainLooper()).postDelayed({
                    val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                    if (imm != null && binding.edBatchCodeScan != null) {
                        imm.hideSoftInputFromWindow(binding.edBatchCodeScan.windowToken, 0)
                    }
                }, 200)


                //todo if leaser type choose..
                if (sessionManagement.getScannerType(context) == "LEASER") { //sessionManagement.getLeaserCheck()!! == 1 && sessionManagement.getQRScannerCheck()!! == 0
                    binding.ivScanBatchCode.visibility = View.GONE
                    tvTotalScanGW = binding.tvTotalScanGw

                    var itmNo = this.ItemNo


                    binding.edBatchCodeScan.addTextChangedListener(object : TextWatcher {
                        override fun afterTextChanged(s: Editable?) {
                            try {
                                AppConstants.WhsCode = list[position].Warehouse
                                AppConstants.ItemCode = list[position].ItemNo
                                var text = binding.edBatchCodeScan.text.toString().trim()
                                var size = 0
                                val aa = text.split(",")
                                val lastPart = text.split(",")[aa.size - 1]
                                itemTyep = lastPart
                                //TODO validation for stop multiple scanning at one order line...
                                if (hashMap.get("Item" + position) is List<*>) {
                                    val list = hashMap.get("Item" + position) as List<*>

                                    Log.e("size===>", list.size.toString())
                                    var str = text
                                    if (str.contains(",") || str.contains("~")) {
                                        var batchInDate = str.split(",")[5].replace("-", "")
                                        if (itemTyep.equals("Batch", true)) {

                                            str = str.split(",")[1]
                                            Batchstr = str
                                            Log.i("DISTNUMBER", "doGetBatchNumScanDetails => DistNumber after scan : $Batchstr")
                                            AppConstants.BatchNo = Batchstr

                                            Log.i("DISTNUMBER", "doGetBatchNumScanDetails => DistNumber from prefs: ${AppConstants.BatchNo}")

                                            Log.e("text====>", "afterTextChanged: " + str)
                                            if (checkDuplicate(hashMap.get("Item" + position)!!, str)) {
                                                //todo scan call api here...
                                                scanBatchLinesItem(
                                                    str,
                                                    binding.rvBatchItems,
                                                    adapterPosition,
                                                    itmNo,
                                                    binding.tvOpenQty,
                                                    binding.tvTotalScannQty,
                                                    binding.tvTotalScanGw,
                                                    qty,
                                                    batchInDate
                                                )

                                            }
                                        } else if (itemTyep.equals("Serial", true)) {
                                            str = str.split(",")[1]

                                            if (checkDuplicateForSerial(hashMap.get("Item" + position)!!, str)) {
                                                //todo scan call api here...

                                                scanSerialLineItem(str, binding.rvBatchItems, adapterPosition, itmNo, binding.tvOpenQty, binding.tvTotalScannQty, binding.tvTotalScanGw, qty)

                                            }
                                        } else if (itemTyep.equals("None", true) || itemTyep.equals("NONE")) {
                                            var scanItem = text
                                            val parts = text.split(",")

                                            val lastPart = parts.last()
                                            var itemCode = parts[0]
                                            itemDesc = parts[2]
                                            Batchstr = ""
                                            AppConstants.BatchNo = Batchstr
                                            if (checkDuplicateForNone(hashMap.get("Item" + position)!!, text)) {
                                                //todo scan call api here...
                                                callNoneBindFunction(itemCode, binding.rvBatchItems, adapterPosition, tvTotalScanQty, itemDesc, scanItem, qty)
                                            }
                                        }

                                    } else {
                                        var batchInDate = str.split(",")[5].replace("-", "")

                                        if (itemTyep.equals("Batch", true)) {
                                            str = str.split(",")[1]
                                            Batchstr = str
                                            AppConstants.BatchNo = Batchstr
                                            if (checkDuplicate(hashMap.get("Item" + position)!!, str)) {
                                                //todo scan call api here...
                                                scanBatchLinesItem(
                                                    str,
                                                    binding.rvBatchItems,
                                                    adapterPosition,
                                                    itmNo,
                                                    binding.tvOpenQty,
                                                    binding.tvTotalScannQty,
                                                    binding.tvTotalScanGw,
                                                    qty,
                                                    batchInDate
                                                )

                                            }
                                        } else if (itemTyep.equals("Serial", true)) {
                                            str = str.split(",")[1]
                                            Batchstr = str
                                            AppConstants.BatchNo = Batchstr
                                            if (checkDuplicateForSerial(hashMap.get("Item" + position)!!, str)) {
                                                //todo scan call api here...
                                                scanSerialLineItem(str, binding.rvBatchItems, adapterPosition, itmNo, binding.tvOpenQty, binding.tvTotalScannQty, binding.tvTotalScanGw, qty)
                                            }
                                        } else if (itemTyep.equals("None", true) || itemTyep.equals("NONE")) {
                                            var scanItem = text
                                            val parts = text.split(",")
                                            Batchstr = ""
                                            AppConstants.BatchNo = Batchstr
                                            val lastPart = parts.last()
                                            var itemCode = parts[0]
                                            itemDesc = parts[2]

                                            if (checkDuplicateForNone(hashMap.get("Item" + position)!!, text)) {
                                                //todo scan call api here...
                                                callNoneBindFunction(itemCode, binding.rvBatchItems, pos, tvTotalScanQty, itemDesc, scanItem, qty)
                                            }
                                        }


                                    }


                                }
                                binding.edBatchCodeScan.setText("")
                                binding.edBatchCodeScan.requestFocus()

                                Handler(Looper.getMainLooper()).postDelayed({
                                    val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                                    if (imm != null && binding.edBatchCodeScan != null) {
                                        imm.hideSoftInputFromWindow(binding.edBatchCodeScan.windowToken, 0)
                                    }
                                }, 200)


                            } catch (e: Exception) {
                                e.printStackTrace()

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
                        itemCode = this.ItemNo.toString()
                        tvOpenQty = binding.tvOpenQty
                        tvTotalScanQty = binding.tvTotalScannQty

                        tvTotalScanGW = binding.tvTotalScanGw
                        openQty = qty

                        if (sessionManagement.getScannerType(context) == null) {
                            showPopupNotChooseScanner()
                        } else if (sessionManagement.getScannerType(context) == "QR_SCANNER") {
                            val intent = Intent(context, QRScannerActivity::class.java)
                            pos = adapterPosition
                            (context as ProductionOrderLinesActivity).startActivityForResult(intent, REQUEST_CODE)
                        }

                    }


                }


                //TODO save order lines listener by interface...
                save.setOnClickListener {
                    callback.onApiResponse(hashMap, quantityHashMap, serialQuantityHashMap, noneQuantityHashMap)
                }

                /*binding.mainCard.setOnClickListener {
                    AppConstants.WhsCode = list[position].Warehouse
                    AppConstants.ItemCode = list[position].ItemNo
                    getBinAbs(AppConstants.ItemCode, AppConstants.WhsCode, AppConstants.BatchNo, position)
                }*/ //comment by vinod @04Jun, 2025

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


    var hashMapBatchList: java.util.HashMap<String, ArrayList<ScanedOrderBatchedItems.Value>> = java.util.HashMap<String, ArrayList<ScanedOrderBatchedItems.Value>>()
    var hashmapBatchQuantityList: java.util.HashMap<String, ArrayList<String>> = java.util.HashMap<String, ArrayList<String>>()
    var serialHashMapQuantityList: java.util.HashMap<String, ArrayList<String>> = java.util.HashMap<String, ArrayList<String>>()
    var noneHashMapQuantityList: java.util.HashMap<String, ArrayList<String>> = java.util.HashMap<String, ArrayList<String>>()


    /*override fun onDeleteItemClick(list: ArrayList<ScanedOrderBatchedItems.Value>, quantityHashMap1: ArrayList<String>, pos: Int, parentPosition: Int, rvBatchItems: RecyclerView) {

        var batch = ""

        if (!list[pos].Batch.isNullOrEmpty()) {
            batch = list[pos].Batch
        } else if (!list[pos].SerialNumber.isNullOrEmpty()) {
            batch = list[pos].SerialNumber
        }


        MaterialAlertDialogBuilder(context)
            .setTitle("Confirm...")
            .setMessage("Do you want to delete " + batch + " Item .")
            .setIcon(R.drawable.ic_trash)
            .setPositiveButton("Confirm",
                DialogInterface.OnClickListener { dialogInterface: DialogInterface?, i1: Int ->
                    hashmapBatchQuantityList = quantityHashMap
                    Log.e("before_valueList===>", list.size.toString())
                    Log.e("before_batch===>", quantityHashMap1.size.toString())
                    Log.e("quantityHashMap===>", hashmapBatchQuantityList.toString())

                    list.removeAt(pos)

                    quantityHashMap1.removeAt(pos)


                    //todo added update list value in hashmap list after remove items--
//                    quantityHashMap.put("Item$pos", quantityHashMap1 as java.util.ArrayList<String>)


                    var data = GlobalMethods.sumBatchQuantity(pos, quantityHashMap1!!).toString()
                    Log.e("data_value===>", data.toString())
                    tvTotalScanQty.setText(":   " + data)
//                    tvTotalScannQty.text = ":   " + GlobalMethods.sumBatchQuantity(pos, quantityHashMap1!!).toString()


                    hashMapBatchList = hashMap
                    hashmapBatchQuantityList = quantityHashMap
                    serialHashMapQuantityList = serialQuantityHashMap
                    noneHashMapQuantityList = noneQuantityHashMap
                    Log.e("hashmap--->", hashMapBatchList.toString())
                    Log.e("aftQuantityHashMap-->", hashmapBatchQuantityList.toString())
                    Log.e("aftserialQuantity-->", serialHashMapQuantityList.toString())
                    Log.e("aftNoneQuantity-->", noneHashMapQuantityList.toString())

                    Log.e("after_valueList===>", list.size.toString())
                    Log.e("after_batch===>", quantityHashMap1.size.toString())

                    batchItemsAdapter?.notifyDataSetChanged()
                    notifyItemChanged(parentPosition)

                })
            .setNegativeButton("Cancel",
                DialogInterface.OnClickListener { dialogInterface, i ->
                    dialogInterface.dismiss()
                })
            .show()

    }*/

    override fun onDeleteItemClick(
        list: ArrayList<ScanedOrderBatchedItems.Value>,
        quantityHashMap1: ArrayList<String>,
        pos: Int,
        parentPosition: Int,
        rvBatchItems: RecyclerView
    ) {
        val batch = list[pos].Batch.takeIf { !it.isNullOrEmpty() } ?: list[pos].SerialNumber ?: ""

        MaterialAlertDialogBuilder(context)
            .setTitle("Confirm...")
            .setMessage("Do you want to delete \"$batch\" item?")
            .setIcon(R.drawable.ic_trash)
            .setPositiveButton("Confirm") { _, _ ->
                Log.e("Batch_Item", "before_valueList===> $list")
                Log.e("Batch_Item", "before_batch===> $quantityHashMap1")
                if (pos < list.size) list.removeAt(pos)
                if (pos < quantityHashMap1.size) quantityHashMap1.removeAt(pos)
                Log.e("Batch_Item", "after_valueList===> $list")
                Log.e("Batch_Item", "after_batch===> $quantityHashMap1")
                // Update total quantity after removing item
                val totalQty = GlobalMethods.sumBatchQuantity(0, quantityHashMap1).toString()
                tvTotalScanQty.text = ":   $totalQty"

                // Log current states
                Log.e("After list size", list.size.toString())
                Log.e("After qty size", quantityHashMap1.size.toString())

                val currentChildAdapter = rvBatchItems.adapter as? BatchItemsIssueAdapter
                currentChildAdapter?.apply {
                    // Update its internal data source if it's separate from the hashMaps
                    // For example, if BatchItemsDeliveryAdapter has its own list, update it here.
                    // If it directly uses the hashMaps, then it just needs to be notified.
                    notifyItemRemoved(pos)
                    // notifyItemRangeChanged is often good practice if items shift up
                    notifyItemRangeChanged(pos, list.size - pos)
                }

                /*// Notify only the child adapter for that parentPosition
                rvBatchItems.adapter?.notifyItemRemoved(pos)
                rvBatchItems.adapter?.notifyItemRangeChanged(pos, list.size)

                // Optional: Notify parent adapter if needed
                notifyItemChanged(parentPosition)*/
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
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


    //TODO scan item lines api here....
    private fun scanBatchLinesItem(
        text: String,
        rvBatchItems: RecyclerView,
        position: Int,
        itemCode: String?,
        tvOpenQty: TextView,
        tvTotalScannQty: TextView,
        tvTotalScanGw: TextView,
        openQty: Double,
        batchInDate: String
    ) {
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
                                        AppConstants.BatchNo = text
                                        list[position].BatchNumber = text
                                        Log.i("DISTNUMBER", "doGetBatchNumScanDetails => DistNumber : ${responseModel.value[0].Batch}")
                                        Log.e("ItemPo==>", "" + itemPo)
                                    }
                                    Log.e("itemPo=>", itemPo.toString())

                                    var totalScanQty = tvTotalScannQty.text.toString()
                                    var total = totalScanQty.toIntOrNull() ?: 0

                                    if (itemPo == -1) {
                                        GlobalMethods.showError(context, "Item Code not matched")
                                    } else if (total >= openQty.toDouble()) {
                                        GlobalMethods.showError(context, "Scanning completed for this Item")
                                    } else {

                                        if (responseModel.value.size > 0 && !responseModel.value.isNullOrEmpty()) {
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

                                                getQuantityFromApi(text, itemList_gl[0].ItemCode, position, stringList, tvTotalScannQty, rvBatchItems, itemList_gl, openQty)

                                                if (tvTotalScanGw.text.equals("Batch"))
                                                    getQuantityForSuggestion(
                                                        text,
                                                        itemList_gl[0].ItemCode,
                                                        position,
                                                        stringList,
                                                        tvOpenQty,
                                                        tvTotalScannQty,
                                                        tvTotalScanGw,
                                                        rvBatchItems,
                                                        itemList_gl,
                                                        batchInDate
                                                    )


                                            }
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


    //TODO scan item lines api here....


    private fun getQuantityForSuggestion(
        batchCode: String,
        itemCode: String,
        position: Int,
        stringList: ArrayList<String>,
        tvOpenQty: TextView,
        tvTotalScannQty: TextView,
        tvTotalScanGw: TextView,
        rvBatchItems: RecyclerView,
        itemList_gl: ArrayList<ScanedOrderBatchedItems.Value>,
        batchInDate: String
    ) {
        if (networkConnection.getConnectivityStatusBoolean(context)) {
            materialProgressDialog.show()
            var apiConfig = ApiConstantForURL()

            NetworkClients.updateBaseUrlFromConfig(apiConfig)

            QuantityNetworkClient.updateBaseUrlFromConfig(apiConfig)
            var remQt = tvOpenQty.text.toString().trim().removePrefix(":  ")
            remQt = batchInDate
            Log.e("Okh Test==>", "" + remQt)
            val networkClient = QuantityNetworkClient.create(context)
            networkClient.getQuantityForSuggestion(
                sessionManagement.getCompanyDB(context)!!,
                remQt,
                itemCode,
                list.get(position).Warehouse
            )//AppConstants.COMPANY_DB,   list.get(position).FromWarehouseCode
                .apply {
                    enqueue(object : Callback<GetSuggestionQuantity> {
                        @RequiresApi(Build.VERSION_CODES.N)
                        override fun onResponse(call: Call<GetSuggestionQuantity>, response: Response<GetSuggestionQuantity>) {
                            try {
                                materialProgressDialog.dismiss()
                                if (response.isSuccessful) {
                                    Log.e("response---------", response.body().toString())

                                    var responseModel = response.body()!!
                                    if (responseModel.value.size > 0 && !responseModel.value.isNullOrEmpty()) {
                                        if (responseModel.value[0].Quantity.toString().trim().toDouble() > 0.0) {
                                            AppConstants.showAlertDialog(context, responseModel.value[0].Batch, responseModel.value[0].Quantity)
                                            hashMap.get("Item" + position)!!.clear()
                                            batchItemsAdapter?.notifyDataSetChanged()
                                            //  quantityHashMap.remove("Item" + position)
                                            Log.e("Okh Remove", quantityHashMap.get("Item" + position)?.size.toString())
                                            /*      if (quantityHashMap.isNotEmpty() && quantityHashMap.containsKey("Item" + position))
                                                  {
                                                     quantityHashMap.remove("Item" + position)
                                                     // hashMap.remove("Item" + position, itemList_gl)
                                                      hashMap.get("Item" + position)?.removeAt(0)
                                                      batchItemsAdapter?.removeItem(position)
                                                      batchItemsAdapter?.notifyDataSetChanged()

                                                      Log.e("Okh Remove-11 ",quantityHashMap.size.toString())
                                                  }
      */


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

                        override fun onFailure(call: Call<GetSuggestionQuantity>, t: Throwable) {
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


    private fun getQuantityFromApi(
        batchCode: String,
        itemCode: String,
        position: Int,
        stringList: ArrayList<String>,
        tvTotalScannQty: TextView,
        rvBatchItems: RecyclerView,
        itemList_gl: ArrayList<ScanedOrderBatchedItems.Value>,
        openQty: Double
    ) {
        if (networkConnection.getConnectivityStatusBoolean(context)) {
            materialProgressDialog.show()
            var apiConfig = ApiConstantForURL()

            NetworkClients.updateBaseUrlFromConfig(apiConfig)

            QuantityNetworkClient.updateBaseUrlFromConfig(apiConfig)

            val networkClient = QuantityNetworkClient.create(context)
            networkClient.getQuantityValue(sessionManagement.getCompanyDB(context)!!, batchCode, itemCode, list.get(position).Warehouse)
                .enqueue(object : Callback<GetQuantityModel> {
                    override fun onResponse(call: Call<GetQuantityModel>, response: Response<GetQuantityModel>) {
                        try {
                            materialProgressDialog.dismiss()
                            if (response.isSuccessful) {
                                val responseModel = response.body()!!
                                if (responseModel.value.isNotEmpty() && !responseModel.value[0].Quantity.isNullOrEmpty() && !responseModel.value[0].Quantity.equals("0.0")) {

                                    var Quantity = responseModel.value[0].Quantity.toDoubleOrNull()

                                    if (Quantity!! > openQty) {
                                        scanCount = list[itemPo].isScanned
                                        ++scanCount;
                                        list[itemPo].isScanned = scanCount

                                        //todo added order line if quantity is not 0 or blank-
                                        hashMap.put("Item" + position, itemList_gl)




                                        stringList.add(openQty.toString())


                                        quantityHashMap.put("Item" + position, stringList)

                                        globalQtyList_gl["Item$position"] = ArrayList(stringList)

                                        tvTotalScannQty.text = "${GlobalMethods.sumBatchQuantity(position, stringList)}"

                                        if (stringList.isNotEmpty() && !stringList.contains("0")) {
                                            rvBatchItems.layoutManager = LinearLayoutManager(context)

                                            batchItemsAdapter = BatchItemsIssueAdapter(
                                                context,
                                                tvTotalScannQty,
                                                hashMap["Item$position"]!!,
                                                quantityHashMap.get("Item" + position)!!,
                                                "IssueOrder",
                                                position,
                                                rvBatchItems
                                            ) { newQuantity, pos, tvBatchQuantity ->
                                                Log.e("Quantity===> ", "onResponse: $newQuantity")
                                                var QUANTITYVAL = newQuantity

                                                val value = QUANTITYVAL.toIntOrNull() ?: 0

                                                val listCopy = globalQtyList_gl["Item$pos"]?.map { it } ?: emptyList()

                                                val mapValue = if (pos in listCopy.indices) {
                                                    listCopy[pos].toDoubleOrNull()?.toInt()
                                                } else {
                                                    null
                                                }

                                                if (QUANTITYVAL.isEmpty()) {
                                                    QUANTITYVAL = "0"
                                                }

                                                if (!QUANTITYVAL.isNullOrEmpty()) {

                                                    /*     if (openQty != null && value > openQty) {
                                                           GlobalMethods.showError(context, "Value cannot exceed then Open Quantity")
                                                           tvBatchQuantity.setText("")
                                                       }
                                                       else if (mapValue != null && value > mapValue){
                                                           GlobalMethods.showError(context, "Value cannot exceed then Open Quantity")
                                                           tvBatchQuantity.setText("")
                                                       }
                                                      else {
                                                       }*/
                                                    if (stringList.size > pos) {
                                                        stringList[pos] = QUANTITYVAL
                                                    } else {
                                                        stringList.add(QUANTITYVAL)
                                                    }

                                                    quantityHashMap.put("Item" + position, stringList)

                                                    val list = quantityHashMap["Item$position"]?.toMutableList() ?: mutableListOf()

                                                    // Update the quantityHashMap with the modified list
                                                    quantityHashMap["Item$position"] = list as java.util.ArrayList<String>

                                                    tvTotalScannQty.text = "${GlobalMethods.sumBatchQuantity(pos, list)}"


                                                }

//                                            quantityHashMap["Item$position"] = updatedStringList as java.util.ArrayList<String>

                                            }

                                            batchItemsAdapter?.setOnScannedItemClickListener(this@ProductionOrderLinesAdapter)
                                            batchItemsAdapter?.setOnDeleteItemClickListener(this@ProductionOrderLinesAdapter)
                                            rvBatchItems.adapter = batchItemsAdapter

                                            batchItemsAdapter?.notifyDataSetChanged()
                                        } else {
                                            batchItemsAdapter?.notifyDataSetChanged()
                                            GlobalMethods.showError(context, "Batch / Roll No. has zero Quantity of this PO.")
                                        }

                                    } else if (Quantity!! <= openQty) {
                                        scanCount = list[itemPo].isScanned
                                        ++scanCount;
                                        list[itemPo].isScanned = scanCount

                                        //todo added order line if quantity is not 0 or blank-
                                        hashMap.put("Item" + position, itemList_gl)


                                        Log.e("stringList", "Success=>" + responseModel.value)

                                        stringList.add(Quantity.toString())     //responseModel.value[0].Quantity


                                        quantityHashMap.put("Item" + position, stringList)

                                        globalQtyList_gl["Item$position"] = ArrayList(stringList)


                                        tvTotalScannQty.text = "${GlobalMethods.sumBatchQuantity(position, stringList)}"

                                        if (stringList.isNotEmpty() && !stringList.contains("0")) {
                                            rvBatchItems.layoutManager = LinearLayoutManager(context)

                                            batchItemsAdapter = BatchItemsIssueAdapter(
                                                context,
                                                tvTotalScannQty,
                                                hashMap["Item$position"]!!,
                                                quantityHashMap.get("Item" + position)!!,
                                                "IssueOrder",
                                                position,
                                                rvBatchItems
                                            ) { newQuantity, pos, tvBatchQuantity ->
                                                Log.e("Quantity===> ", "onResponse: $newQuantity")
                                                var QUANTITYVAL = newQuantity

                                                val value = QUANTITYVAL.toIntOrNull() ?: 0

                                                val listCopy = globalQtyList_gl["Item$position"]?.map { it } ?: emptyList()

                                                val mapValue = if (pos in listCopy.indices) {
                                                    listCopy[pos].toDoubleOrNull()?.toInt()
                                                } else {
                                                    null
                                                }

                                                if (QUANTITYVAL.isEmpty()) {
                                                    QUANTITYVAL = "0"
                                                }

                                                if (!QUANTITYVAL.isNullOrEmpty()) {
                                                    if (openQty != null && value > openQty) {
                                                        GlobalMethods.showError(context, "Value cannot exceed then Open Quantity")
                                                        tvBatchQuantity.setText("")
                                                    } else if (mapValue != null && value > mapValue) {
                                                        GlobalMethods.showError(context, "Value cannot exceed then Open Quantity")
                                                        tvBatchQuantity.setText("")
                                                    } else {
                                                        if (stringList.size > pos) {
                                                            stringList[pos] = QUANTITYVAL
                                                        } else {
                                                            stringList.add(QUANTITYVAL)
                                                        }

                                                        quantityHashMap.put("Item" + position, stringList)

                                                        val list = quantityHashMap["Item$position"]?.toMutableList() ?: mutableListOf()

                                                        // Update the quantityHashMap with the modified list
                                                        quantityHashMap["Item$position"] = list as java.util.ArrayList<String>

                                                        tvTotalScannQty.text = "${GlobalMethods.sumBatchQuantity(pos, list)}"


                                                    }


                                                }

//                                            quantityHashMap["Item$position"] = updatedStringList as java.util.ArrayList<String>

                                            }

                                            batchItemsAdapter?.setOnScannedItemClickListener(this@ProductionOrderLinesAdapter)
                                            batchItemsAdapter?.setOnDeleteItemClickListener(this@ProductionOrderLinesAdapter)
                                            rvBatchItems.adapter = batchItemsAdapter

                                            batchItemsAdapter?.notifyDataSetChanged()
                                        } else {
                                            batchItemsAdapter?.notifyDataSetChanged()
                                            GlobalMethods.showError(context, "Batch / Roll No. has zero Quantity of this PO.")
                                        }
                                    }

                                } else {
                                    GlobalMethods.showError(context, "No Quantity Found of this Production Order.")
                                    batchItemsAdapter?.notifyDataSetChanged()
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


    private fun getQuantityFromApi123(
        batchCode: String,
        itemCode: String,
        position: Int,
        stringList: ArrayList<String>,
        tvOpenQty: TextView,
        tvTotalScannQty: TextView,
        tvTotalScanGw: TextView,
        rvBatchItems: RecyclerView,
        itemList_gl: ArrayList<ScanedOrderBatchedItems.Value>
    ) {
        if (networkConnection.getConnectivityStatusBoolean(context)) {
            materialProgressDialog.show()
            var apiConfig = ApiConstantForURL()

            NetworkClients.updateBaseUrlFromConfig(apiConfig)

            QuantityNetworkClient.updateBaseUrlFromConfig(apiConfig)

            val networkClient = QuantityNetworkClient.create(context)
            networkClient.getQuantityValue(sessionManagement.getCompanyDB(context)!!, batchCode, itemCode, sessionManagement.getWarehouseCode(context)!!)//AppConstants.COMPANY_DB,
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

                                        if (!responseModel.value[0].Quantity.isNullOrEmpty()) {
                                            stringList.add(responseModel.value[0].Quantity)
                                        }
                                        quantityHashMap.put("Item" + position, stringList)
                                        //TODO sum of quantity of batches..
                                        tvTotalScannQty.text = GlobalMethods.sumBatchQuantity(position, quantityHashMap.get("Item" + position)!!).toString()


                                        if (quantityHashMap.get("Item" + position)!!.size > 0) {
                                            if (!quantityHashMap.get("Item" + position)!!.contains("0")) {
                                                hashMap.put("Item" + position, itemList_gl)
                                                val layoutManager: RecyclerView.LayoutManager = LinearLayoutManager(context)
                                                rvBatchItems.layoutManager = layoutManager

                                                batchItemsAdapter = BatchItemsIssueAdapter(
                                                    context,
                                                    tvTotalScannQty,
                                                    hashMap.get("Item" + position)!!,
                                                    quantityHashMap.get("Item" + position)!!,
                                                    "IssueOrder",
                                                    position,
                                                    rvBatchItems
                                                ) { quantity, pos, tvBatchQuantity ->

                                                    Log.e("Quantity===> ", "onResponse: " + quantity)


                                                    stringList.add(quantity)
                                                    quantityHashMap.put("Item" + position, stringList)

                                                    tvTotalScannQty.text = GlobalMethods.sumBatchQuantity(position, quantityHashMap.get("Item" + position)!!).toString()

                                                }

//                                                batchItemsAdapter = BatchItemsIssueAdapter(context, hashMap.get("Item" + position)!!, quantityHashMap.get("Item" + position)!!, "IssueOrder")
                                                //todo call setOnItemListener Interface Function...
                                                batchItemsAdapter?.setOnDeleteItemClickListener(this@ProductionOrderLinesAdapter)
                                                rvBatchItems.adapter = batchItemsAdapter

                                                //todo comment not required---
                                                /*var totalGrossWeight = GlobalMethods.changeDecimal(GlobalMethods.sumBatchGrossWeight(position, hashMap.get("Item" + position)!!).toString())
                                                tvTotalScanGw.text = ":   "+ totalGrossWeight*/


                                            } else {
                                                batchItemsAdapter?.notifyDataSetChanged()
                                                GlobalMethods.showError(context, "Batch / Roll No. has zero Quantity of this PO.")
                                            }
                                        } else {
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
    private fun scanSerialLineItem(
        text: String,
        rvBatchItems: RecyclerView,
        position: Int,
        itemCode: String?,
        tvOpenQty: TextView,
        tvTotalScannQty: TextView,
        tvTotalScanGw: TextView,
        openQty: Double
    ) {
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
                                    } else {
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
                                            stringList.addAll(serialQuantityHashMap.get("Item" + position)!!)

                                            itemList_gl.add(responseModel.value[0])


                                            if (!itemList_gl.isNullOrEmpty()) {

                                                Log.e("list_size-----", itemList_gl.size.toString())

                                                //todo quantity..

//                                            getSerialQuantityFromApi(text, itemList_gl[0].ItemCode, position, stringList, tvOpenQty, tvTotalScannQty,tvTotalScanGw, rvBatchItems, itemList_gl )

                                                stringList.add("1")
                                                serialQuantityHashMap.put("Item" + position, stringList)

                                                globalQtyList_gl.put("Item" + position, stringList)

                                                //TODO sum of quantity of batches..
                                                tvTotalScannQty.text = GlobalMethods.sumBatchQuantity(position, serialQuantityHashMap.get("Item" + position)!!).toString()


                                                hashMap.put("Item" + position, itemList_gl)
                                                val layoutManager: RecyclerView.LayoutManager = LinearLayoutManager(context)
                                                rvBatchItems.layoutManager = layoutManager

                                                batchItemsAdapter =
                                                    BatchItemsIssueAdapter(context, tvTotalScannQty, hashMap.get("Item" + position)!!, serialQuantityHashMap.get("Item" + position)!!, "SerialQR", position, rvBatchItems)

//                                            batchItemsAdapter = BatchItemsIssueAdapter(context, hashMap.get("Item" + position)!!, serialQuantityHashMap.get("Item" + position)!!, "SerialQR")

//                                            batchItemsAdapter = BatchItemsIssueAdapter(context, hashMap.get("Item" + position)!!, ArrayList(), "SerialQR")
                                                //todo call setOnItemListener Interface Function...
                                                batchItemsAdapter?.setOnScannedItemClickListener(this@ProductionOrderLinesAdapter)
                                                batchItemsAdapter?.setOnDeleteItemClickListener(this@ProductionOrderLinesAdapter)
                                                rvBatchItems.adapter = batchItemsAdapter
                                                batchItemsAdapter?.notifyDataSetChanged()

                                            }
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


    //todo set none type data--
    private fun callNoneBindFunction(
        itemCode: String,
        rvBatchItems: RecyclerView,
        position: Int,
        tvTotalScannQty: TextView,
        itemDesc: String,
        scanItem: String,
        openQty: Double
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
            } else if (scanCount >= openQty.toDouble()) {
                GlobalMethods.showError(context, "Scanning completed for this Item")
            } else {
                scanCount = list[itemPo].isScanned
                ++scanCount;
                list[itemPo].isScanned = scanCount

                var data = ScanedOrderBatchedItems.Value("", itemCode, itemDesc, "", "", scanItem, "", "", "", "", "", "", "", 0, "", 0.0, 0.0, 0.0, 0.0, 0.0, "", 0.0, "", 0.0, 0.0, "")

                var modelResponse = data
                scanedBatchedItemsList_gl.addAll(listOf(modelResponse))

                var itemList_gl: ArrayList<ScanedOrderBatchedItems.Value> = ArrayList()
                itemList_gl.addAll(hashMap.get("Item" + position)!!)
                //itemList_gl= hashMap.get("Item"+position)!!

                var stringList: ArrayList<String> = ArrayList()
                stringList.addAll(noneQuantityHashMap.get("Item" + position)!!)

                itemList_gl.add(data)


                if (!itemList_gl.isNullOrEmpty()) {

                    Log.e("list_size-----", itemList_gl.size.toString())

                    //todo quantity..

                    stringList.add("0")

                    noneQuantityHashMap.put("Item" + position, stringList)

                    globalQtyList_gl.put("Item" + position, stringList)

                    //TODO sum of quantity of batches..
                    tvTotalScannQty.text = GlobalMethods.sumBatchQuantity(position, noneQuantityHashMap.get("Item" + position)!!).toString()

                    hashMap.put("Item" + position, itemList_gl)
                    val layoutManager: RecyclerView.LayoutManager = LinearLayoutManager(context)
                    rvBatchItems.layoutManager = layoutManager


                    batchItemsAdapter =
                        BatchItemsIssueAdapter(context, tvTotalScannQty, hashMap["Item$position"]!!, noneQuantityHashMap.get("Item" + position)!!, "NoneQR", position, rvBatchItems) { newQuantity, pos, tvBatchQuantity ->
                            Log.e("Quantity===> ", "onResponse: $newQuantity")
                            var QUANTITYVAL = newQuantity
                            val value = QUANTITYVAL.toIntOrNull() ?: 0

                            /*  val listCopy = globalQtyList_gl["Item$pos"]?.map { it } ?: emptyList()

                              val mapValue = if (pos in listCopy.indices) {
                                  listCopy[pos].toDoubleOrNull()?.toInt()
                              } else {
                                  null
                              }*/

                            if (QUANTITYVAL.isEmpty()) {
                                QUANTITYVAL = "0"
                            }

                            if (!QUANTITYVAL.isNullOrEmpty()) {
                                if (openQty != null && value > openQty) {
                                    GlobalMethods.showError(context, "Value cannot exceed then Open Quantity")
                                    tvBatchQuantity.setText("")
                                }
                                /* else if (mapValue != null && value > mapValue){
                                     GlobalMethods.showError(context, "Value cannot exceed then Open Quantity")
                                     tvBatchQuantity.setText("")
                                 }*/
                                else {

                                    if (stringList.size > pos) {
                                        stringList[pos] = QUANTITYVAL
                                    } else {
                                        stringList.add(QUANTITYVAL)
                                    }

                                    noneQuantityHashMap.put("Item" + position, stringList)

                                    val list = noneQuantityHashMap["Item$position"]?.toMutableList() ?: mutableListOf()

                                    // Update the quantityHashMap with the modified list
                                    noneQuantityHashMap["Item$position"] = list as java.util.ArrayList<String>

                                    tvTotalScannQty.text = "${GlobalMethods.sumBatchQuantity(pos, list)}"

                                }

                            }


                        }


//                batchItemsAdapter = BatchItemsIssueAdapter(context, hashMap.get("Item" + position)!!, noneQuantityHashMap.get("Item" + position)!!, "NoneQR")

                    //todo call setOnItemListener Interface Function...
                    batchItemsAdapter?.setOnScannedItemClickListener(this@ProductionOrderLinesAdapter)
                    batchItemsAdapter?.setOnDeleteItemClickListener(this@ProductionOrderLinesAdapter)
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

    var itemDesc = ""

    open fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            val result = data?.getStringExtra("batch_code")
            var size = 0
            val list = hashMap.get("Item" + pos) as List<*>
            Log.e("size===>", list.size.toString())
            Log.e("ItemCode===>", itemCode)


            //todo spilt string and get string at 0 index...

            var batchInDate = result.toString().split(",")[5].replace("-", "")

            //todo set validation for duplicate item
            if (tvTotalScanGW.text.equals("Batch")) {
                if (checkDuplicate(hashMap.get("Item" + pos)!!, result.toString().split(",")[1])) {
                    //todo scan call api here...
                    scanBatchLinesItem(result.toString().split(",")[1], recyclerView, pos, itemCode, tvOpenQty, tvTotalScanQty, tvTotalScanGW, openQty, batchInDate)

                }
            } else if (tvTotalScanGW.text.equals("Serial")) {
                if (checkDuplicateForSerial(hashMap.get("Item" + pos)!!, result.toString().split(",")[1])) {
                    //todo scan call api here...
                    scanSerialLineItem(result.toString().split(",")[1], recyclerView, pos, itemCode, tvOpenQty, tvTotalScanQty, tvTotalScanGW, openQty)
                }
            } else if (tvTotalScanGW.text.equals("None") || tvTotalScanGW.text.equals("NONE")) {
                var scanItem = result.toString().split(",")[0]
                val parts = result.toString().split(",")

                val lastPart = parts.last()
                var itemCode = parts[0]
                itemDesc = parts[2]

                if (checkDuplicateForNone(hashMap.get("Item" + pos)!!, scanItem)) {
                    //todo scan call api here...
                    callNoneBindFunction(itemCode, recyclerView, pos, tvTotalScanQty, itemDesc, scanItem, openQty)
                }
            } else {
                if (tvTotalScanGW.text.isEmpty()) {
                    GlobalMethods.showMessage(context, "Scan Type is Empty")
                } else {
                    GlobalMethods.showMessage(context, "Scan Type is " + tvTotalScanGW.text.toString())
                }
            }


            //todo validation for stop multiple scanning at one order line...
            /*if (hashMap.get("Item" + pos) is List<*>) {
                Log.e("size===>q", list.size.toString())
                val list = hashMap.get("Item" + pos) as List<*>
                if (list.size == size) {
                    Log.e("size===>", list.size.toString())
                    Log.e("ItemCode===>", itemCode)
                    //todo spilt string and get string at 0 index...
                    scanOrderLinesItem(result.toString().split(",")[0], recyclerView, pos, itemCode, tvOpenQty, tvTotalScanQty, tvTotalScanGW)
                } else {
                    GlobalMethods.showError(context, "Can not Scan multiple Batch")
                    Log.e("SizeGreaterThanOne===>", "Error")
                }
            }*/


        }
    }


    var listNew = ArrayList<GetAbsModel.Value>()
    private val binLocation = mutableListOf<ModelBinLocation>()

    private lateinit var adapterFields: DynamicFieldAdapter
    var defaultBinName = "";
    var defaultBinCode = "";
    var temp = ArrayList<String>()

    private fun openDynamicFieldsDialog(context: Context, parentPosition: Int, batchItemPosition: Int, batchNum: String, qtyCallback: QtyListener, onQtySaved: (String) -> Unit) {

        val dialog = Dialog(this.context)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(true)
        dialog.setContentView(R.layout.dialog_dynamic_fields)
        dialog.setCancelable(false)
        // Ensure the background is transparent
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        // Set dialog width to MATCH_PARENT
        dialog.window?.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT)

        val btnAddBin: ImageView = dialog.findViewById(R.id.btnAddBin)
        val btnSave: MaterialButton = dialog.findViewById(R.id.btnSave)
        val btnCancel: MaterialButton = dialog.findViewById(R.id.btnCancel)
        val rvDynamicField: RecyclerView = dialog.findViewById(R.id.rvDynamicField)
        val tvItemQty: TextView = dialog.findViewById(R.id.tvItemQty)
        val addBin_Txt: TextView = dialog.findViewById(R.id.addBin_Txt)

        tvItemQty.setText(" Open Quantity : " + listNew[0].Quantity)
        val binLocations = listNew[0].BinCode

        /*if (!list.getOrNull(pos)?.binAllocationJSONs.isNullOrEmpty()) {
            Log.e("ISSUE_PRODUCTION", "openDynamicFieldsDialog=> pos==> $parentPosition \nBinList==> ${toSimpleJson(list[parentPosition].binAllocationJSONs[batchItemPosition])}")
        }*/


        val binAbs = listNew[0].AbsEntry.split(",").map { it.trim() }
        val binLocationList = binLocations.split(",").map { it.trim() }
        //setDefaultSpinner(acBinLocation,binLocationList,binAbs)
        if (!binLocation.isEmpty())
            binLocation.clear()
        binLocation.add(ModelBinLocation())
        //getBinAbs(list[po].WarehouseCode)

// Check if previously saved bin data exists
        /*val existingAllocations = list.getOrNull(pos)?.binAllocationJSONs

        if (!existingAllocations.isNullOrEmpty()) {
            // Load previously saved bin data
            binLocation.addAll(existingAllocations.map {
                ModelBinLocation(
                    binLocation = it.BinLocation,
                    binLocationCode = it.BinAbsEntry,
                    batchNumber = it.BatchNum,
                    itemQuantity = it.Quantity,
                    WareHouseCode = it.WarehouseCode,
                    toBinLocationCode = it.ToBinAbsEntry,
                    ManufacturerSerialNumber = it.ManufacturerSerialNumber,
                    InternalSerialNumber = it.InternalSerialNumber,
                    ExpiryDate = it.ExpiryDate,
                    ManufacturingDate = it.ManufacturingDate
                )
            })
        } else {
            // If no previous data, start with empty one
            binLocation.add(ModelBinLocation())
        }*/


        if (listNew[0].AbsEntry != null && binLocations != null) {
            val binAbs = listNew[0].AbsEntry.split(",").map { it.trim() }
            val binLocationList = binLocations.split(",").map { it.trim() }
            setFieldAdapters(
                context,
                rvDynamicField,
                binLocationList,
                "YY_No_TO",
                "Y",
                binAbs
            )
        } else {
            setFieldAdapters(
                context,
                rvDynamicField,
                binLocationList,
                "YY_No_TO",
                "Y",
                binAbs
            )
        }

        btnAddBin.setOnClickListener {
            binLocation.add(ModelBinLocation())
            adapterFields.notifyItemInserted(binLocation.size - 1)
        }
        btnSave.setOnClickListener {
            Log.e("ISSUE_PRODUCTION", "openDynamicFieldsDialog (click Save Btn)=> BinData  ==> ${toSimpleJson(binLocation)}")


            val myArrayList = ArrayList<PurchaseRequestModel.binAllocationJSONs>()
            var totalQty = 0.0;
            for (j in binLocation.indices) {
                if (!binLocation.get(j).itemQuantity.trim().isEmpty()) {
                    totalQty += binLocation.get(j).itemQuantity.trim().toDouble()
                }

                var binAllocationJSONs = PurchaseRequestModel.binAllocationJSONs(
                    binLocation.get(j).binLocation.trim(),
                    binLocation.get(j).binLocationCode.trim(),
                    batchNum.trim(),
                    binLocation.get(j).itemQuantity.trim(),
                    binLocation.get(j).WareHouseCode.trim(),
                    binLocation.get(j).toBinLocationCode.trim(),
                    binLocation.get(j).ManufacturerSerialNumber.trim(),
                    binLocation.get(j).InternalSerialNumber.trim(),
                    binLocation.get(j).ExpiryDate.trim(),
                    binLocation.get(j).ManufacturingDate.trim()
                )

                myArrayList.add(binAllocationJSONs)
                //Log.e("BinData=>",binAllocationJSONs.toString())
            }
            if (totalQty > listNew[0].Quantity.toDouble()) {
                Toast.makeText(context, "Quantity exceeded. ", Toast.LENGTH_SHORT).show()
            } else {
                // Toast.makeText(context, "Data saved: ${inputData.size}", Toast.LENGTH_SHORT).show()

                /* list[pos].binAllocationJSONs = arrayListOf()
                 list[pos].binAllocationJSONs.addAll(myArrayList)
                 list[pos].Quantity = totalQty.toString()*/

                if (list[parentPosition].binAllocationJSONs == null) {
                    list[parentPosition].binAllocationJSONs = arrayListOf()
                }

                // Get the existing list (safe initialization already done)
                val existing = list[parentPosition].binAllocationJSONs ?: arrayListOf()
                val hasDuplicate = myArrayList.any { new ->
                    existing.any { existingItem ->
                        Log.i(
                            "ISSUE_PRODUCTION", "openDynamicFieldsDialog=> \nBinAbsEntry (New) => ${new.BinAbsEntry}" +
                                    "BinAbsEntry (Existing) => ${existingItem.BinAbsEntry}\nBatchNum (New) => ${new.BatchNum}\nBatchNum (Existing) => ${existingItem.BatchNum}\""
                        )
                        existingItem.BinAbsEntry == new.BinAbsEntry && existingItem.BatchNum == new.BatchNum
                    }
                }

                if (hasDuplicate) {
                    GlobalMethods.showError(context, "You can't add same Bin Location with same batch more than one.")
                    //Toast.makeText(context, "Duplicate Bin and Quantity found. Not added.", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                // Just add all new allocations, even if duplicates
                existing.addAll(myArrayList)

                // Update the list
                list[parentPosition].binAllocationJSONs = existing
                Log.i(
                    "ISSUE_PRODUCTION", "openDynamicFieldsDialog=> \nBinAllocations (New) => ${toSimpleJson(myArrayList)}\n" +
                            "BinAllocations (Existing) => ${toSimpleJson(existing)}"
                )
                list[parentPosition].binAllocationJSONs = existing

                // Accumulate quantity
                val oldQty = list[parentPosition].Quantity?.toDoubleOrNull() ?: 0.0
                list[parentPosition].Quantity = (oldQty + totalQty).toString()
                //Log.e("ISSUE_PRODUCTION", "openDynamicFieldsDialog => Qty: $totalQty")
                qtyCallback.onQtyChanged(totalQty)
                onQtySaved(totalQty.toString())
                dialog.dismiss()
                binLocation.clear()
                Log.e("ISSUE_PRODUCTION", "openDynamicFieldsDialog=> BinData Final ==> ${toSimpleJson(list[parentPosition])}")
            }
            dialog.dismiss()
        }
        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }


    private fun setFieldAdapters(context: Context, rvDynamicField: RecyclerView, binLocationList: List<String>, scanType: String, binManaged: String, binAbs: List<String>) {
        rvDynamicField.apply {
            // Initialize RecyclerView
            adapterFields = DynamicFieldAdapter(
                context, binLocationList, binAbs,
                binLocation,

                onRemoveItem = { parentPosition ->
                    binLocation.removeAt(parentPosition)
                    adapterFields?.notifyDataSetChanged()
                }, scanType, binManaged
            )

            layoutManager = LinearLayoutManager(context)
            adapter = adapterFields

        }
    }


    //networkConnection = NetworkConnection()

    private fun getBinAbs(ItemCode: String, whscode: String, distNumber: String, parentPosition: Int, batchItemPosition: Int, tvTotalScannQty: TextView, onQtySaved: (String) -> Unit) {

        /* networkConnection = NetworkConnection()
         materialProgressDialog = MaterialProgressDialog(context)*/
        //todo changes by shubh for warehouse listing
        if (networkConnection.getConnectivityStatusBoolean(context)) {
            materialProgressDialog.show()
            var apiConfig = ApiConstantForURL()

            NetworkClients.updateBaseUrlFromConfig(apiConfig)

            QuantityNetworkClient.updateBaseUrlFromConfig(apiConfig)

            val networkClient = QuantityNetworkClient.create(context)
            networkClient.getBin(ItemCode, whscode, distNumber)
                .enqueue(object : Callback<GetAbsModel>, QtyListener {
                    override fun onResponse(
                        call: Call<GetAbsModel>,
                        response: Response<GetAbsModel>
                    ) {
                        try {
                            materialProgressDialog.dismiss()
                            if (response.isSuccessful) {
                                listNew.clear()
                                listNew.addAll(response.body()!!.value)
                                if (listNew.size > 0) {
                                    openDynamicFieldsDialog(context, parentPosition, batchItemPosition, distNumber, this) { updatedQty ->
                                        onQtySaved(updatedQty)
                                    }
                                } else {
                                    //GlobalMethods.showError(context,"Bin Location must be enabled for Item Code: $ItemCode in Warehouse: $whscode for DistNumber: $distNumber before proceeding.")
                                    showBinLocationErrorDialog(context, ItemCode, whscode, distNumber, true)
                                }
                            } else {
                                // handleErrorResponse(response)
                            }
                        } catch (e: Exception) {
                            materialProgressDialog.dismiss()
                            e.printStackTrace()
                        }
                    }

                    override fun onFailure(call: Call<GetAbsModel>, t: Throwable) {
                        Log.e("scanItemApiFailed-----", t.toString())
                        materialProgressDialog.dismiss()
                    }

                    override fun onQtyChanged(totalScannedQty: Double) {
                        tvTotalScannQty.text = totalScannedQty.toString()
                        Log.e("SCAN_QT", "onQtyChanged callback => Qty: $totalScannedQty")
                    }
                })
        } else {
            materialProgressDialog.dismiss()
            Toast.makeText(context, "No Network Connection", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onScannedItemClicked(etBactchQty: TextInputEditText, tvTotalScannQty: TextView, batchItem: ScanedOrderBatchedItems.Value, parentPosition: Int, batchItemPosition: Int) {
        Log.i("ISSUE_PRODUCTION", "onScannedItemClicked => DocLine Position: $parentPosition ScanItem Position: $batchItemPosition scanItem: ${toSimpleJson(batchItem)}")
        AppConstants.WhsCode = list[parentPosition].Warehouse
        AppConstants.ItemCode = list[parentPosition].ItemNo
        AppConstants.BatchNo = batchItem.Batch
        getBinAbs(AppConstants.ItemCode, AppConstants.WhsCode, AppConstants.BatchNo, parentPosition, batchItemPosition, tvTotalScannQty) { updatedQty ->
            etBactchQty.setText(updatedQty)
        }

    }

    interface QtyListener {
        fun onQtyChanged(totalScannedQty: Double)
    }
}
