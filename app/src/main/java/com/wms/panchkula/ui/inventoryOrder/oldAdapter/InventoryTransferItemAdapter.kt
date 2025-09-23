package com.wms.panchkula.ui.inventoryOrder.oldAdapter

import DynamicFieldInventoryReqAdapter
import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.content.ContentValues
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
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
import com.wms.panchkula.R
import com.wms.panchkula.Retrofit_Api.NetworkClients
import com.wms.panchkula.Retrofit_Api.QuantityNetworkClient
import com.wms.panchkula.SessionManagement.SessionManagement
import com.wms.panchkula.databinding.ProductionOrderLinesAdapterLayoutBinding
import com.wms.panchkula.Retrofit_Api.ApiConstantForURL
import com.wms.panchkula.ui.home.HomeActivity
import com.wms.panchkula.ui.inventoryOrder.Model.InventoryRequestModel
import com.wms.panchkula.ui.invoiceOrder.UI.InventoryTransferLinesActivity
import com.wms.panchkula.ui.issueForProductionOrder.Model.ScanedOrderBatchedItems
import com.wms.panchkula.ui.issueForProductionOrder.UI.qrScannerUi.QRScannerActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.gson.GsonBuilder
import com.pixplicity.easyprefs.library.Prefs
import com.wms.panchkula.Global_Classes.GlobalMethods.toSimpleJson
import com.wms.panchkula.Model.*
import com.wms.panchkula.ui.inventoryOrder.Model.BinLocationModel
import com.wms.panchkula.ui.inventoryOrder.Model.DefaultToBinLocationModel
import com.wms.panchkula.ui.purchase.model.PurchaseRequestModel
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.IOException
import java.net.SocketTimeoutException
import java.sql.*


class InventoryTransferItemAdapter(
    private val context: Context, var list: ArrayList<InventoryRequestModel.StockTransferLines>, private val networkConnection: NetworkConnection,
    private val materialProgressDialog: MaterialProgressDialog, private val callback: AdapterCallback, private val save: Chip
) : RecyclerView.Adapter<InventoryTransferItemAdapter.ViewHolder>(), BatchItemsInventoryReqAdapter.OnDeleteItemClickListener, BatchItemsInventoryReqAdapter.OnScannedItemClickListener {

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

    private lateinit var binLocationByWarehouse: BinLocationModel
    private lateinit var recyclerView: RecyclerView
    private var pos: Int = 0
    private var itemCode = ""
    var batchItemsAdapter: BatchItemsInventoryReqAdapter? = null
    lateinit var tvTotalScanQty: TextView
    lateinit var tvTotalScanGW: TextView
    lateinit var tvOpenQty: TextView
    private val scannedBatchSet = mutableSetOf<String>()


    init {
//        setSqlServer()
        sessionManagement = SessionManagement(context)
    }

    private var totalScanGW: String = ""

    //todo interfaces...
    interface AdapterCallback {
        fun onApiResponseStock(
            response: java.util.HashMap<String, ArrayList<ScanedOrderBatchedItems.Value>>,
            list: ArrayList<InventoryRequestModel.StockTransferLines>,
            quantityResponse: HashMap<String, ArrayList<String>>,
            serialQuantityResponse: HashMap<String, ArrayList<String>>,
            noneQuantityResponse: HashMap<String, ArrayList<String>>
        )
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ProductionOrderLinesAdapterLayoutBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, @SuppressLint("RecyclerView") position: Int) {
        with(holder) {
            with(list[position]) {

                binding.tvItemCode.text = this.ItemCode
                binding.tvItemName.text = this.ItemDescription
                var qty = this.PlannedQuantity - this.IssuedQuantity
                binding.tvOpenQty.text = this.RemainingOpenQuantity
                this.FromWarehouseCode = sessionManagement.getWarehouseCode(context).toString()
                tvTotalScanQty = binding.tvTotalScannQty

//                tvTotalScanGW = binding.tvTotalScanGw

                totalScanGW = when {
                    this.Batch == "Y" && this.Serial == "N" && this.None == "N" -> "Batch"
                    this.Serial == "Y" && this.Batch == "N" && this.None == "N" -> "Serial"
                    this.None == "Y" && this.Batch == "N" && this.Serial == "N" -> "None"
                    else -> "None"
                }

                binding.tvTotalScanGw.text = "$totalScanGW"


                binding.mainCard.setOnClickListener {
                    //callBinLocationByWarehouse(context, this.FromWarehouseCode,position,this.ItemCode,this.Batch.toString())
                } //comment by vinod @04Jun, 2025

                //callBinLocationByWarehouse(context, this.FromWarehouseCode, parentPosition)
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
                        Log.e("count None QTY===> " + count, noneQuantityHashMap.toString())
                    }
                    count++
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
                            Log.e("text====>BP", "afterTextChanged: No Change" + s.toString().trim())

                            // Fetch the text from EditText after text has changed
                            val text = s.toString().trim()

                            Log.e("text====>BP", "afterTextChanged: ======BBB==>" + text)

                            if (text.isNotEmpty()) {
                                try {
                                    val parts = text.split(",")
                                    val lastPart = parts.getOrNull(parts.size - 1) ?: return
                                    itemTyep = lastPart
                                    Log.e("text====>", "afterTextChanged: Normal" + itemTyep)
                                    if (itemTyep.equals("Batch", true)) {

                                        val batch = parts.getOrNull(1).toString()
                                        val qrItemCode = parts.getOrNull(0).toString()
                                        val batchInDate = parts.getOrNull(5)?.replace("-", "").toString()
                                        /*if (scannedBatchSet.contains(batch)) {
                                            Log.w("SCAN_ITEM", "Duplicate scan blocked immediately: $batch")
                                            Toast.makeText(context, "Batch already scanned!", Toast.LENGTH_SHORT).show()
                                            return
                                        }*/

                                        val isUnique = checkDuplicate(hashMap["Item$position"] ?: arrayListOf(), batch)
                                        Log.e(
                                            "SCAN_ITEM",
                                            "Before If => Scan String (${parts.size})=> ($text) \nhashMap(ScannedList) Size: ${hashMap["Item$position"]?.size} ScanItem Code: $itemCode, QrItem Code: $qrItemCode," +
                                                    "ItemType:" +
                                                    " " +
                                                    "${itemTyep}, Batch: ${batch} " +
                                                    "isUnique: $isUnique"
                                        )

                                        /*var isBatch = scanedBatchedItemsList_gl.any { it.Batch.equals(batch, true) }
                                        Log.w("SCAN_ITEM", "isBatch: $isBatch")*/

                                        if (isUnique) {
                                            Log.e("SCAN_ITEM", "checkDuplicate if => ItemType: ${itemTyep}, Batch: ${batch}")
                                            //todo scan call api here...
                                            scanBatchLinesItem(batch, binding.rvBatchItems, adapterPosition, itemCode, binding.tvOpenQty, binding.tvTotalScannQty, binding.tvTotalScanGw, batchInDate)
                                        }
                                    } else if (itemTyep.equals("Serial", true)) {
                                        //todo getting QR code on 2 index
                                        val batch = parts.getOrNull(1).toString()
                                        Log.e("text====>", "afterTextChanged: " + batch)


                                        if (checkDuplicateForSerial(hashMap.get("Item" + position)!!, batch)) {
                                            Log.e("SCAN_ITEM", "checkDuplicateForSerial if => ItemType: ${itemTyep}, Batch: ${batch}")
                                            //todo scan call api here...
                                            scanSerialLineItem(batch, binding.rvBatchItems, adapterPosition, itemCode, binding.tvOpenQty, binding.tvTotalScannQty, binding.tvTotalScanGw)
                                        }
                                    } else if (itemTyep.equals("None") || itemTyep.equals("NONE")) {
                                        Log.e("text====>BBBBBBBB", "afterTextChanged: Normal")
                                        var scanItem = text
                                        val parts = text.split(",")

                                        val lastPart = parts.last()
                                        var itemCode = parts[0]
                                        itemDesc = parts[2]

                                        type = lastPart
                                        Log.e("text====>", "Normal=1: ")
                                        if (checkDuplicateForNone(hashMap.get("Item" + position)!!, text)) {
                                            Log.e("SCAN_ITEM", "checkDuplicateForNone if => ItemType: ${itemTyep}, Batch: No")
                                            //todo scan call api here...
                                            callNoneBindFunction(itemCode, binding.rvBatchItems, adapterPosition, tvTotalScanQty, itemDesc, scanItem)
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
                        } else if (sessionManagement.getScannerType(context) == "QR_SCANNER") {
                            val intent = Intent(context, QRScannerActivity::class.java)
//                            pos = adapterPosition
                            pos = setScanDataOnItem(list, list.get(adapterPosition).ItemCode)
                            (context as InventoryTransferLinesActivity).startActivityForResult(intent, REQUEST_CODE)
                        }

                    }


                }


                //TODO save order lines listener by interface...
                save.setOnClickListener {
                    if (binding.edBatchCodeScan.text.equals("0.0")) {
                        GlobalMethods.showError(context, "Please Enter a valid Quantity")
                    } else {
                        for (i in 0 until list.size) {
                            if (list[i].isScanned != 0) {
                                AppConstants.IS_SCAN = true
                            }
                        }
                        Log.e(ContentValues.TAG, "isItemScan ==> : BP " + AppConstants.IS_SCAN)
                        if (!AppConstants.IS_SCAN) {
                            //save.isEnabled = false
                            //save.isCheckable = false
                            GlobalMethods.showError(context, "Items Not Scan.")
                        } else {

                            //save.isEnabled = false
                            //save.isCheckable = false
                            Log.d("BP-List", hashMap.toString())
                            Log.d("BP-List-1", list.toString())
                            Log.d("BP-List-2", quantityHashMap.toString())
                            Log.d("BP-List-3", serialQuantityHashMap.toString())
                            Log.d("BP-List-4", noneQuantityHashMap.toString())

                            callback.onApiResponseStock(
                                hashMap,
                                list,
                                quantityHashMap,
                                serialQuantityHashMap,
                                noneQuantityHashMap
                            )
                        }
                    }
                }
            }
        }
    }

    private fun callBinLocationByWarehouse(
        context: Context,
        fromWarehouseCode: String,
        toWarehouseCode: String,
        parentPosition: Int,
        itemCode: String,
        batch: String,
        tvTotalScannQty: TextView,
        onQtySaved: (String) -> Unit
    ) {
        var fromBinLocationModel: BinLocationModel? = null
        var toBinLocationModel: DefaultToBinLocationModel.Value? = null

        val apiConfig = ApiConstantForURL()
        val url = apiConfig.BASE_URL
        val url2 = apiConfig.QUANTITY_BASE_URL
        Log.e("BASE_URL", "Base Url ===> $url")
        Log.e("BASE_URL", "Qty Base Url ====> $url2")

        QuantityNetworkClient.updateBaseUrlFromConfig(apiConfig)
        val networkClient = QuantityNetworkClient.create(context)

        networkClient.getBinLocationByWarehouse(fromWarehouseCode, itemCode, batch).apply {
            enqueue(object : Callback<BinLocationModel>, QtyListener {
                override fun onResponse(call: Call<BinLocationModel>, response: Response<BinLocationModel>) {
                    try {
                        if (response.isSuccessful) {
                            materialProgressDialog.dismiss()
                            fromBinLocationModel = response.body()!!
                            //tryOpenDialog()
                            val binManaged = fromBinLocationModel?.BinManaged.toString()
                            Log.e("BIN_MASTER", "BinManaged => $binManaged")
                            if (binManaged == "Y") {
                                openDynamicFieldsDialog(context, parentPosition, fromBinLocationModel!!, toWarehouseCode, batch, this) { updatedQty ->
                                    onQtySaved(updatedQty)
                                }
                            } else {
                                GlobalMethods.showMessage(context, "No need to select bin location for without bin managed item.")
                            }
                        } else {
                            materialProgressDialog.dismiss()

                            Prefs.clear()

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
                        e.printStackTrace()
                    }
                }

                override fun onFailure(call: Call<BinLocationModel>, t: Throwable) {
                    Log.e("login_api_failure-----", t.toString())
                    materialProgressDialog.dismiss()
                    when (t) {
                        is SocketTimeoutException -> {
                            GlobalMethods.showError(context, "Connection timed out. Please try again.")
                        }

                        is IOException -> {
                            GlobalMethods.showError(context, "Network error. Please check your internet connection.")
                        }

                        else -> {
                            GlobalMethods.showError(context, "Something went wrong: ${t.localizedMessage}")
                        }
                    }
//                                Prefs.clear()
                    Log.e("DBURL==>", "onFailure: " + Prefs.getString(AppConstants.DBUrl, ""))

                    //Toast.makeText(this@PurchaseTransferLinesActivity, t.message, Toast.LENGTH_SHORT)
                }

                override fun onQtyChanged(totalScannedQty: Double) {
                    tvTotalScannQty.text = totalScannedQty.toString()
                    Log.e("SCAN_QT", "onQtyChanged callback => Qty: $totalScannedQty")
                }

            })
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

    var itemTyep = ""

    //TODO viewholder...
    class ViewHolder(val binding: ProductionOrderLinesAdapterLayoutBinding) : RecyclerView.ViewHolder(binding.root)


    var hashMapBatchList: java.util.HashMap<String, ArrayList<ScanedOrderBatchedItems.Value>> = java.util.HashMap<String, ArrayList<ScanedOrderBatchedItems.Value>>()
    var hashmapBatchQuantityList: java.util.HashMap<String, ArrayList<String>> = java.util.HashMap<String, ArrayList<String>>()
    var serialHashMapQuantityList: java.util.HashMap<String, ArrayList<String>> = java.util.HashMap<String, ArrayList<String>>()
    var noneHashMapQuantityList: java.util.HashMap<String, ArrayList<String>> = java.util.HashMap<String, ArrayList<String>>()


    override fun onDeleteItemClick(list: ArrayList<ScanedOrderBatchedItems.Value>, quantityHashMap1: ArrayList<String>, pos: Int, parentPosition: Int, rvBatchItems: RecyclerView) {

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

                    hashMapBatchList = hashMap
                    hashmapBatchQuantityList = quantityHashMap
                    serialHashMapQuantityList = serialQuantityHashMap
                    Log.e("Batch_Item", "before_valueList===> $list")
                    Log.e("Batch_Item", "before_batch===> $quantityHashMap1")
                    if (pos < list.size) list.removeAt(pos)
                    if (pos < quantityHashMap1.size) quantityHashMap1.removeAt(pos)
                    /*list.removeAt(pos)
                    quantityHashMap1.removeAt(pos)*/
                    Log.e("Batch_Item", "after_valueList===> $list")
                    Log.e("Batch_Item", "after_batch===> $quantityHashMap1")

                    var data = GlobalMethods.sumBatchQuantity(pos, quantityHashMap1!!).toString()
                    Log.e("data_value===>", data.toString())
                    tvTotalScanQty.setText(data)
//                    tvTotalScannQty.text = GlobalMethods.sumBatchQuantity(pos, quantityHashMap1!!).toString()


                    hashMapBatchList = hashMap
                    hashmapBatchQuantityList = quantityHashMap
                    serialHashMapQuantityList = serialQuantityHashMap
                    noneHashMapQuantityList = noneQuantityHashMap

                    Log.e("After list size", list.size.toString())
                    Log.e("After qty size", quantityHashMap1.size.toString())

                    val currentChildAdapter = rvBatchItems.adapter as? BatchItemsInventoryReqAdapter
                    currentChildAdapter?.apply {
                        // Update its internal data source if it's separate from the hashMaps
                        // For example, if BatchItemsDeliveryAdapter has its own list, update it here.
                        // If it directly uses the hashMaps, then it just needs to be notified.
                        notifyItemRemoved(pos)
                        // notifyItemRangeChanged is often good practice if items shift up
                        notifyItemRangeChanged(pos, list.size - pos)
                    }

//                    notifyItemChanged(parentPosition)

                    Log.e("parentPosition==>", "parentPosition: " + parentPosition)
                    /*  val layoutManager: RecyclerView.LayoutManager = LinearLayoutManager(context)
                      rvBatchItems.layoutManager = layoutManager
                      if (!list[pos].Batch.isNullOrEmpty()) {
                          batchItemsAdapter = BatchItemsInventoryReqAdapter(context, hashMap.get("Item" + parentPosition)!!, quantityHashMap.get("Item" + parentPosition)!!, "SerialQR", parentPosition, rvBatchItems)
                      }
                      else if (!list[pos].SerialNumber.isNullOrEmpty()) {
                          batchItemsAdapter = BatchItemsInventoryReqAdapter(context, hashMap.get("Item" + parentPosition)!!, serialQuantityHashMap.get("Item" + parentPosition)!!, "SerialQR", parentPosition, rvBatchItems)
                      }

                      batchItemsAdapter?.setOnDeleteItemClickListener(this@InventoryTransferItemAdapter)
                      rvBatchItems.adapter = batchItemsAdapter*/
                    /*batchItemsAdapter?.notifyItemRemoved(pos)
                    batchItemsAdapter?.notifyDataSetChanged()
                    notifyDataSetChanged()*/

                })
            .setNegativeButton("Cancel",
                DialogInterface.OnClickListener { dialogInterface, i ->
                    dialogInterface.dismiss()
                })

            .show()
    }

    /* fun checkDuplicate(scanedBatchedItemsList_gl: ArrayList<ScanedOrderBatchedItems.Value>, batchCode: String): Boolean {
         var startus: Boolean = true;
         for (items in scanedBatchedItemsList_gl) {
             if (items.Batch.equals(batchCode)) {
                 startus = false
                 Toast.makeText(context, "Batch no. Already Exists!", Toast.LENGTH_SHORT).show()
             }
         }
         return startus
     }*/

    fun checkDuplicate(
        scannedBatchedItemsList: ArrayList<ScanedOrderBatchedItems.Value>,
        batchCode: String
    ): Boolean {
        for (item in scannedBatchedItemsList) {
            val isDuplicate = item.Batch.equals(batchCode, ignoreCase = true)
            Log.i("SCAN_ITEM", "checkDuplicate: Item: ${item.ItemCode}, Batch: ${item.Batch}, isDuplicate: $isDuplicate")
            if (item.Batch.equals(batchCode, ignoreCase = true)) {
                Toast.makeText(context, "Batch no. Already Exists!", Toast.LENGTH_SHORT).show()
                return false
            }
        }
        return true
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
    private fun scanBatchLinesItem(
        text: String,
        rvBatchItems: RecyclerView,
        position: Int,
        itemCode: String?,
        tvOpenQty: TextView,
        tvTotalScannQty: TextView,
        tvTotalScanGw: TextView,
        batchInDate: String
    ) {
        if (networkConnection.getConnectivityStatusBoolean(context)) {
            materialProgressDialog.show()
            var apiConfig = ApiConstantForURL()

            NetworkClients.updateBaseUrlFromConfig(apiConfig)

            QuantityNetworkClient.updateBaseUrlFromConfig(apiConfig)

            val networkClient = NetworkClients.create(context)

            networkClient.doGetBatchNumScanDetails("Batch eq '" + text.trim() + "'" + " and ItemCode eq '" + itemCode + "'")
                .apply {
                    enqueue(object : Callback<ScanedOrderBatchedItems> {
                        @SuppressLint("SuspiciousIndentation")
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

                                    if (responseModel.value.size == 0) {
                                        GlobalMethods.showError(context, "Batch quantity not found.")
                                    } else if (itemPo == -1) {
                                        GlobalMethods.showError(context, "Item Code not matched")
                                    } else if (total >= list[itemPo].RemainingOpenQuantity.toDouble()) {
                                        GlobalMethods.showError(context, "Scanning completed for this Item")
                                    } else {
                                        Log.d("SCAN_ITEM", "responseModel: ${responseModel.value}")
                                        Log.d("SCAN_ITEM", "responseModel.size: ${responseModel.value.size}")
                                        Log.d("SCAN_ITEM", "responseModel.isNullOrEmpty: ${responseModel.value.isNullOrEmpty()}")
                                        if (responseModel.value.size > 0 && !responseModel.value.isNullOrEmpty()) {

                                            var modelResponse = responseModel.value
                                            Log.i(
                                                "SCAN_ITEM", "doGetBatchNumScanDetails => Batch Details (${modelResponse.size}): ${toSimpleJson(modelResponse)}" +
                                                        "\nscanedBatchedItemsList_gl (${scanedBatchedItemsList_gl.size}) before add batch details: ${toSimpleJson(scanedBatchedItemsList_gl)}"
                                            )

                                            scanedBatchedItemsList_gl.addAll(modelResponse)

                                            var itemList_gl: ArrayList<ScanedOrderBatchedItems.Value> = ArrayList()
                                            Log.e(
                                                "SCAN_ITEM", "doGetBatchNumScanDetails => scanedBatchedItemsList_gl (${scanedBatchedItemsList_gl.size}) after add batch details: ${
                                                    toSimpleJson
                                                        (scanedBatchedItemsList_gl)
                                                }"
                                            )

                                            itemList_gl.addAll(hashMap.get("Item" + position)!!)
                                            //itemList_gl= hashMap.get("Item"+position)!!
                                            Log.e("SCAN_ITEM", "doGetBatchNumScanDetails => itemList_gl (${itemList_gl.size}) add hashMap list: ${toSimpleJson(itemList_gl)}")
                                            var stringList: ArrayList<String> = ArrayList()
                                            stringList.addAll(quantityHashMap.get("Item" + position)!!)
                                            Log.e("SCAN_ITEM", "doGetBatchNumScanDetails => stringList (${stringList.size}) add quantityHashMap list: ${toSimpleJson(stringList)}")
                                            val newBatchItem = responseModel.value[0]


                                            //itemList_gl.addAll(itemList_gl.distinctBy { newBatchItem.Batch == it.Batch })

                                            Log.e("SCAN_ITEM", "doGetBatchNumScanDetails => itemList_gl (${itemList_gl.size}) after add newBatchItem: ${toSimpleJson(itemList_gl)}")

                                            //if (itemList_gl.isNotEmpty()) {
                                            try {
                                                Log.e("SCAN_ITEM", "Calling with batchCode=$text, itemCode=${itemCode}")
                                                getQuantityFromApi(
                                                    text,
                                                    itemCode.toString(),
                                                    position,
                                                    stringList,
                                                    tvOpenQty,
                                                    tvTotalScannQty,
                                                    tvTotalScanGw,
                                                    rvBatchItems,
                                                    itemList_gl,
                                                    newBatchItem
                                                )
                                            } catch (e: Exception) {
                                                Log.e("SCAN_ITEM", "Exception in getQuantityFromApi: ${e.message}", e)
                                            }
                                            /*} else {
                                                Log.e("SCAN_ITEM", "itemList_gl is empty, skipping API call")
                                            }*/

                                            // Check if batch already exists in itemList_gl
                                            /*val isDuplicate = itemList_gl.any { it.Batch.equals(newBatchItem.Batch, ignoreCase = true) }

                                            if (!isDuplicate) {
                                                itemList_gl.add(newBatchItem)
                                                Log.d("SCAN_ITEM", "New batch added: ${newBatchItem.Batch}")
                                            } else {
                                                Log.d("SCAN_ITEM", "Duplicate batch found: ${newBatchItem.Batch} — Skipping add")
                                            }*/

                                            // hashMap.put("Item" + position, itemList_gl)


                                            //if (!itemList_gl.isNullOrEmpty()) {

                                            //Log.e("list_size-----", itemList_gl.size.toString())


                                            //getQuantityFromApi(text, itemList_gl[0].ItemCode, position, stringList, tvOpenQty, tvTotalScannQty, tvTotalScanGw, rvBatchItems, itemList_gl, newBatchItem)
                                            /* if(tvTotalScanGw.text.equals("Batch"))
                                             getQuantityForSuggestion(text, itemList_gl[0].ItemCode, position, stringList, tvOpenQty, tvTotalScannQty,tvTotalScanGw, rvBatchItems, itemList_gl,batchInDate )
                                        */
                                            //}
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
    private fun getQuantityFromApi(
        batchCode: String, itemCode: String, position: Int, stringList: ArrayList<String>,
        tvOpenQty: TextView, tvTotalScannQty: TextView, tvTotalScanGw: TextView, rvBatchItems: RecyclerView, itemList_gl: ArrayList<ScanedOrderBatchedItems.Value>, newBatchItem: ScanedOrderBatchedItems.Value
    ) {
        Log.e("SCAN_ITEM", "call getQuantityFromApi()")
        if (networkConnection.getConnectivityStatusBoolean(context)) {
            Log.e("SCAN_ITEM", "call if getQuantityFromApi()")
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
                                    Log.e("SCAN_ITEM", "getQuantityValue => response: ${toSimpleJson(response.body())}")


                                    var responseModel = response.body()!!
                                    if (!responseModel.value.isNullOrEmpty() && !responseModel.value[0].Quantity.isNullOrEmpty() && !responseModel.value[0].Quantity.equals("0.0")) {

                                        var Quantity = responseModel.value[0].Quantity.toDoubleOrNull()
                                        var RemainingOpenQuantity = list[itemPo].RemainingOpenQuantity.toDoubleOrNull()

                                        if (Quantity != null && RemainingOpenQuantity != null) {

                                            scanCount = list[itemPo].isScanned
                                            ++scanCount
                                            list[itemPo].isScanned = scanCount

                                            val isDuplicate = itemList_gl.any { it.Batch.equals(newBatchItem.Batch, ignoreCase = true) }

                                            if (!isDuplicate) {
                                                itemList_gl.add(newBatchItem)
                                                hashMap["Item$position"] = itemList_gl

                                                // Handle quantity assignment based on Batch/Serial selection
                                                val quantityToAdd = if (tvTotalScanGw.text.toString().equals("Batch", ignoreCase = true) && Quantity > RemainingOpenQuantity) {
                                                    RemainingOpenQuantity.toString()
                                                } else {
                                                    responseModel.value[0].Quantity
                                                }

                                                stringList.add(quantityToAdd)
                                                quantityHashMap["Item$position"] = stringList
                                                globalQtyList_gl["Item$position"] = ArrayList(stringList)

                                                Log.d("SCAN_ITEM", "New batch added: ${newBatchItem.Batch}")
                                                Log.d(
                                                    "SCAN_ITEM",
                                                    "hashMap (${hashMap["Item$position"]?.size}) updated: ${toSimpleJson(hashMap["Item$position"])}" +
                                                            "\nquantityHashMap (${quantityHashMap["Item$position"]?.size}) updated: ${toSimpleJson(quantityHashMap["Item$position"])}"
                                                )

                                                tvTotalScannQty.text = GlobalMethods.sumBatchQuantity(position, stringList).toString()

                                                if (stringList.isNotEmpty() && !stringList.contains("0")) {
                                                    rvBatchItems.layoutManager = LinearLayoutManager(context)

                                                    batchItemsAdapter = BatchItemsInventoryReqAdapter(
                                                        context,
                                                        tvTotalScannQty,
                                                        itemList_gl,
                                                        stringList,
                                                        "IssueOrder",
                                                        position,
                                                        rvBatchItems
                                                    ) { newQuantity, pos, tvBatchQuantity ->

                                                        val quantityStr = newQuantity.ifEmpty { "0" }
                                                        val quantityVal = quantityStr.toIntOrNull() ?: 0
                                                        val allowedQtyList = globalQtyList_gl["Item$pos"] ?: emptyList()
                                                        val allowedQty = allowedQtyList.getOrNull(pos)?.toDoubleOrNull()?.toInt()

                                                        Log.w(
                                                            "INVENT_REQ",
                                                            "QUANTITY: $quantityStr, Value: $quantityVal, AllowedList: $allowedQtyList, AllowedValue: $allowedQty"
                                                        )

                                                        if (RemainingOpenQuantity != null && quantityVal > RemainingOpenQuantity) {
                                                            GlobalMethods.showError(context, "Value cannot exceed Open Quantity")
                                                            tvBatchQuantity.setText("")
                                                            return@BatchItemsInventoryReqAdapter
                                                        }

                                                        if (allowedQty != null && quantityVal > allowedQty) {
                                                            GlobalMethods.showError(context, "Value cannot exceed Open Quantity")
                                                            tvBatchQuantity.setText("")
                                                            return@BatchItemsInventoryReqAdapter
                                                        }

                                                        val currentList = quantityHashMap["Item$position"] ?: arrayListOf()
                                                        if (pos < currentList.size) {
                                                            currentList[pos] = quantityStr
                                                        } else {
                                                            while (currentList.size < pos) currentList.add("0")
                                                            currentList.add(quantityStr)
                                                        }

                                                        quantityHashMap["Item$position"] = currentList
                                                        tvTotalScannQty.text = GlobalMethods.sumBatchQuantity(pos, currentList).toString()
                                                    }

                                                    hashMapBatchList = hashMap
                                                    hashmapBatchQuantityList = quantityHashMap
                                                    serialHashMapQuantityList = serialQuantityHashMap

                                                    batchItemsAdapter?.setOnScannedItemClickListener(this@InventoryTransferItemAdapter)
                                                    batchItemsAdapter?.setOnDeleteItemClickListener(this@InventoryTransferItemAdapter)
                                                    rvBatchItems.adapter = batchItemsAdapter
                                                    batchItemsAdapter?.notifyDataSetChanged()
                                                } else {
                                                    batchItemsAdapter?.notifyDataSetChanged()
                                                    GlobalMethods.showError(context, "Batch / Roll No. has zero Quantity of this PO.")
                                                }

                                            } else {
                                                Log.d("SCAN_ITEM", "Duplicate batch found: ${newBatchItem.Batch} — Skipping add")
                                            }
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

    // todo code add by tarun for alert dialog
    private fun showAlertDialog(batch: String, quantity: String) {

        val builder = AlertDialog.Builder(context)
            .setTitle("Suggested Batch: $batch \nAnd Quantity: $quantity")
//            .setMessage("Do you want to proceed?")
            .setPositiveButton("OK") { dialog, _ ->

                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->

                dialog.dismiss()
            }

        // Show the dialog
        val dialog = builder.create()
        dialog.show()
        dialog.setCanceledOnTouchOutside(false)
        // Adjust the width and height of the dialog
        val layoutParams = dialog.window?.attributes
        layoutParams?.width = WindowManager.LayoutParams.MATCH_PARENT // Set the width to match parent
        layoutParams?.height = WindowManager.LayoutParams.WRAP_CONTENT // Set the height to wrap content
        dialog.window?.attributes = layoutParams

    }

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
                list.get(position).FromWarehouseCode
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

                                                //todo quantity..

                                                stringList.add("1")
                                                serialQuantityHashMap.put("Item" + position, stringList)

                                                globalQtyList_gl.put("Item" + position, stringList)

                                                //TODO sum of quantity of batches..
                                                tvTotalScannQty.text = GlobalMethods.sumBatchQuantity(position, serialQuantityHashMap.get("Item" + position)!!).toString()

                                                hashMap.put("Item" + position, itemList_gl)

                                                scannedBatchSet.clear()
                                                val layoutManager: RecyclerView.LayoutManager = LinearLayoutManager(context)
                                                rvBatchItems.layoutManager = layoutManager

                                                batchItemsAdapter =
                                                    BatchItemsInventoryReqAdapter(
                                                        context, tvTotalScannQty, hashMap.get("Item" + position)!!, serialQuantityHashMap.get("Item" + position)!!, "SerialQR", position,
                                                        rvBatchItems
                                                    )


                                                hashMapBatchList = hashMap
                                                hashmapBatchQuantityList = quantityHashMap
                                                serialHashMapQuantityList = serialQuantityHashMap
                                                Log.e("hashmap--->", hashMapBatchList.toString())
                                                Log.e("batchQuantityList-->", hashmapBatchQuantityList.toString())
                                                Log.e("serialQuantity-->", serialQuantityHashMap.toString())


                                                //todo call setOnItemListener Interface Function...
                                                batchItemsAdapter?.setOnScannedItemClickListener(this@InventoryTransferItemAdapter)
                                                batchItemsAdapter?.setOnDeleteItemClickListener(this@InventoryTransferItemAdapter)
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


    private fun callNoneBindFunction(itemCode: String, rvBatchItems: RecyclerView, position: Int, tvTotalScannQty: TextView, itemDesc: String, scanItem: String) {

        Log.e("text====>", "Normal=2: Bhupi")
        if (itemCode.isNotEmpty()) {

            if (itemCode.isNotEmpty()) {

                Log.e("ItemCode==>", "" + itemCode)
                itemPo = setScanDataOnItem(list, itemCode)
                Log.e("ItemPo==>", "" + itemPo)
            }

            Log.e("itemPo=>", itemPo.toString())
            var totalScanQty = tvTotalScannQty.text.toString()
            var total = totalScanQty.toIntOrNull() ?: 0

            var RemainingOpenQuantity = list[itemPo].RemainingOpenQuantity.toDoubleOrNull()


            if (itemPo == -1) {
                GlobalMethods.showError(context, "Item Code not matched")
            } else if (scanCount >= list[itemPo].RemainingOpenQuantity.toDouble()) {
                GlobalMethods.showError(context, "Scanning completed for this Item")
            } else {
                Log.e("text====>", "Normal=2: " + type)
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
                Log.e("-stringList--", stringList.size.toString())


                if (!itemList_gl.isNullOrEmpty()) {

                    Log.e("list_size-----", itemList_gl.size.toString())
                    Log.e("text====>", "Normal=3: " + itemList_gl.size.toString())

                    //todo quantity..

                    stringList.add("0")

                    Log.e("list_size-----Poo", "" + position)

                    noneQuantityHashMap.put("Item" + position, stringList)


                    globalQtyList_gl["Item$position"] = ArrayList(stringList)
                    Log.e("text====>", "Normal=4: " + stringList.size.toString())

                    //TODO sum of quantity of batches..
                    tvTotalScannQty.text = GlobalMethods.sumBatchQuantity(position, noneQuantityHashMap.get("Item" + position)!!).toString()
                    Log.e("text====>", "Normal=5: " + hashMap.toString())
                    Log.e("text====>", "Normal=55: " + noneQuantityHashMap.toString())
                    hashMap.put("Item" + position, itemList_gl)
                    Log.e("text====>", "Normal=5: " + noneQuantityHashMap.get("Item" + position))
                    val layoutManager: RecyclerView.LayoutManager = LinearLayoutManager(context)
                    rvBatchItems.layoutManager = layoutManager

                    //  batchItemsAdapter = BatchItemsInventoryReqAdapter(context, hashMap.get("Item" + position)!!, noneQuantityHashMap.get("Item" + position)!!, "NoneQR", position, rvBatchItems){ newQuantity, pos  , tvBatchQuantity->
                    batchItemsAdapter = BatchItemsInventoryReqAdapter(
                        context,
                        tvTotalScannQty,
                        hashMap.get("Item" + position)!!,
                        noneQuantityHashMap.get("Item" + position)!!,
                        "NoneQR",
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


                        Log.e("text====>", "Normal=6: " + hashMap.toString())
                        if (QUANTITYVAL.isEmpty()) {
                            QUANTITYVAL = "0"
                        }

                        if (!QUANTITYVAL.isNullOrEmpty()) {

                            /* if (RemainingOpenQuantity != null && value > RemainingOpenQuantity ) {
                                 GlobalMethods.showError(context, "Value cannot exceed then Open Quantity")
                                 tvBatchQuantity.setText("")
                             }*/

                            /* else if (mapValue != null && value > mapValue){
                                 GlobalMethods.showError(context, "Value cannot exceed then Open Quantity")
                                 tvBatchQuantity.setText("")
                             }*/
//                            else {
                            if (stringList.size > pos) {
                                stringList[pos] = QUANTITYVAL
                            } else {
                                stringList.add(QUANTITYVAL)
                            }

                            noneQuantityHashMap.put("Item" + position, stringList)
                            Log.e("text====>", "Normal=7: " + noneQuantityHashMap.toString())

                            val list = noneQuantityHashMap["Item$position"]?.toMutableList() ?: mutableListOf()

                            // Update the quantityHashMap with the modified list
                            noneQuantityHashMap["Item$position"] = list as ArrayList

                            tvTotalScannQty.text = ":   ${GlobalMethods.sumBatchQuantity(pos, list)}"

//                            }

                        }

                    }




                    hashMapBatchList = hashMap
                    hashmapBatchQuantityList = quantityHashMap
                    serialHashMapQuantityList = serialQuantityHashMap
                    noneHashMapQuantityList = noneQuantityHashMap

                    Log.e("hashmap--->B", hashMapBatchList.toString())
                    Log.e("batchQuantityList-->1B", hashmapBatchQuantityList.toString())
                    Log.e("serialQuantity-->B", serialQuantityHashMap.toString())
                    Log.e("noneQuantity-->B", noneQuantityHashMap.toString())


                    //todo call setOnItemListener Interface Function...
                    batchItemsAdapter?.setOnScannedItemClickListener(this@InventoryTransferItemAdapter)
                    batchItemsAdapter?.setOnDeleteItemClickListener(this@InventoryTransferItemAdapter)
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
            Log.e("ItemCode===>", itemCode)

            var batchInDate = result.toString().split(",")[5].replace("-", "")
            Log.e("batchInDate===>", batchInDate)
            //todo spilt string and get string at 0 index...

            //todo set validation for duplicate item
            if (tvTotalScanGW.text.equals("Batch")) {
                if (checkDuplicate(hashMap.get("Item" + pos)!!, result.toString().split(",")[1])) {
                    //todo scan call api here...
                    scanBatchLinesItem(result.toString().split(",")[1], recyclerView!!, pos, itemCode, tvOpenQty!!, tvTotalScanQty!!, tvTotalScanGW!!, batchInDate)

                }
            } else if (tvTotalScanGW.text.equals("Serial")) {
                if (checkDuplicateForSerial(hashMap.get("Item" + pos)!!, result.toString().split(",")[1])) {
                    //todo scan call api here...
                    scanSerialLineItem(result.toString().split(",")[1], recyclerView!!, pos, itemCode, tvOpenQty!!, tvTotalScanQty!!, tvTotalScanGW!!)
                }
            } else if (tvTotalScanGW.text.equals("None") || tvTotalScanGW.text.equals("NONE")) {
                var scanItem = result.toString().split(",")[0]
                val parts = result.toString().split(",")

                val lastPart = parts.last()
                var itemCode = parts[0]
                itemDesc = parts[2]

                type = lastPart

                if (checkDuplicateForNone(hashMap.get("Item" + pos)!!, scanItem)) {
                    //todo scan call api here...
                    callNoneBindFunction(itemCode, recyclerView, pos, tvTotalScanQty, itemDesc, scanItem)
                }
            } else {
                if (tvTotalScanGW.text.isEmpty()) {
                    GlobalMethods.showMessage(context, "Scan Type is Empty")
                } else {
                    GlobalMethods.showMessage(context, "Scan Type is " + tvTotalScanGW.text.toString())
                }
            }


        }
    }


    private val binLocation = mutableListOf<ModelBinLocation>()

    private lateinit var adapterFields: DynamicFieldInventoryReqAdapter

    //private lateinit var adapterFields: DynamicFieldAdapter
    var defaultBinName = "";
    var defaultBinCode = "";
    var temp = ArrayList<String>()

    private fun openDynamicFieldsDialog(context: Context, po: Int, binLocationByFromWarehouse: BinLocationModel, toWarehouseCode: String, batch: String, qtyCallback: QtyListener, onQtySaved: (String) -> Unit) {

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
        tvItemQty.setText(" Open Quantity : " + list[po].RemainingOpenQuantity)
        val fromBinLocations = binLocationByFromWarehouse.BinCode.toString() //list[po].BinCode


        defaultBinName = binLocationByFromWarehouse.DefaultBinCD.toString() //list[po].DefaultBinCD
        val defaultFromBinCode = binLocationByFromWarehouse.DefaultABSEntry.toString() //list[po].DefaultABSEntry


        val fromBinAbs = binLocationByFromWarehouse.BinABSEntry.toString().split(",").map { it.trim() }
        val fromBinLocationList = fromBinLocations.split(",").map { it.trim() }

        Log.e("binLocationList==>", fromBinLocationList.toString())
        if (!binLocation.isEmpty())
            binLocation.clear()
        binLocation.add(ModelBinLocation())
        //getBinAbs(list[po].WarehouseCode)

        if (binLocationByFromWarehouse.BinManaged.equals("N", true)) {
            btnAddBin.isClickable = false
            btnAddBin.visibility = View.GONE
            addBin_Txt.setText("  Quantity ")
            setFieldAdapters(context, rvDynamicField, fromBinLocationList, fromBinAbs, defaultFromBinCode, toWarehouseCode, "YY", list[po].BinManaged)

        } else {
            if (binLocationByFromWarehouse.BinABSEntry != null && fromBinLocations != null) {
                val fromBinAbs = binLocationByFromWarehouse.BinABSEntry.toString().split(",").map { it.trim() }
                val fromBinLocationList = fromBinLocations.split(",").map { it.trim() }
                Log.e("binLocationList==>", fromBinLocationList.toString())
                setFieldAdapters(context, rvDynamicField, fromBinLocationList, fromBinAbs, defaultFromBinCode, toWarehouseCode, "YY", binLocationByFromWarehouse.BinManaged.toString())
            } else {
                setFieldAdapters(context, rvDynamicField, fromBinLocationList, fromBinAbs, defaultFromBinCode, toWarehouseCode, "YY", binLocationByFromWarehouse.BinManaged.toString())
            }
        }

        btnAddBin.setOnClickListener {
            binLocation.add(ModelBinLocation())
            adapterFields.notifyItemInserted(binLocation.size - 1)
        }
        btnSave.setOnClickListener {
            Log.e("BinData=>", binLocation.toString())


            val myArrayList = ArrayList<PurchaseRequestModel.binAllocationJSONs>()
            var totalQty = 0.0;
            for (j in binLocation.indices) {
                if (!binLocation.get(j).itemQuantity.trim().isEmpty()) {
                    totalQty += binLocation.get(j).itemQuantity.trim().toDouble()
                }

                var binAllocationJSONs = PurchaseRequestModel.binAllocationJSONs(
                    binLocation.get(j).binLocation.trim(),
                    binLocation.get(j).binLocationCode.trim(),
                    batch.trim(),
                    binLocation.get(j).itemQuantity.trim(),
                    binLocation.get(j).WareHouseCode.trim(),
                    binLocation.get(j).toBinLocationCode.trim(),
                    binLocation.get(j).ManufacturerSerialNumber.trim(),
                    binLocation.get(j).InternalSerialNumber.trim(),
                    binLocation.get(j).ExpiryDate.trim(),
                    binLocation.get(j).ManufacturingDate.trim()
                )

                myArrayList.add(binAllocationJSONs)
                Log.e("BinData=>", binAllocationJSONs.toString())
            }
            /* if (totalQty > list[po].Quantity.toDouble()) { // comment by Vinod @24Jul,2025 as per Ashish Sejwar
                 Toast.makeText(context, "Quantity exceeded. ", Toast.LENGTH_SHORT).show()
             } else {*/
            // Toast.makeText(context, "Data saved: ${inputData.size}", Toast.LENGTH_SHORT).show()

            /*list[po].binAllocationJSONs = arrayListOf()
            list[po].binAllocationJSONs.addAll(myArrayList)
            list[po].RemainingOpenQuantity = totalQty.toString()*/

            if (list[po].binAllocationJSONs == null) {
                list[po].binAllocationJSONs = arrayListOf()
            }

            // Get the existing list (safe initialization already done)
            val existing = list[po].binAllocationJSONs ?: arrayListOf()
            val hasDuplicate = myArrayList.any { new ->
                existing.any { existingItem ->
                    existingItem.BinAbsEntry == new.BinAbsEntry &&
                            existingItem.BatchNum == new.BatchNum
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
            list[po].binAllocationJSONs = existing

            // Accumulate quantity
            val oldQty = list[po].Quantity?.toDoubleOrNull() ?: 0.0
            list[po].Quantity = (oldQty + totalQty).toString()
            qtyCallback.onQtyChanged(totalQty)
            onQtySaved(totalQty.toString())
            dialog.dismiss()
            binLocation.clear()
            //}
            dialog.dismiss()
        }
        btnCancel.setOnClickListener {
            dialog.dismiss()
        }
        dialog.show()
    }


    private fun setFieldAdapters(
        context: Context, rvDynamicField: RecyclerView, fromBinLocationList: List<String>, fromBinAbs: List<String>, defaultFromBinCode: String,
        toWarehouseCode: String, scanType: String, binManaged: String

    ) {
        rvDynamicField.apply {
            // Initialize RecyclerView
            adapterFields = DynamicFieldInventoryReqAdapter(
                context, fromBinLocationList, fromBinAbs, defaultFromBinCode, toWarehouseCode, binLocation,

                onRemoveItem = { parentPosition ->
                    binLocation.removeAt(parentPosition)
                    adapterFields?.notifyDataSetChanged()
                }, scanType, binManaged
            )

            /*adapterFields = DynamicFieldAdapter(
                context, fromBinLocationList, fromBinAbs,
                binLocation,

                onRemoveItem = { parentPosition ->
                    binLocation.removeAt(parentPosition)
                    adapterFields?.notifyDataSetChanged()
                }, scanType, binManaged
            )*/

            layoutManager = LinearLayoutManager(context)
            adapter = adapterFields

        }
    }

    override fun onScannedItemClicked(etBatchQuantity: TextInputEditText, tvTotalScannQty: TextView, batchItem: ScanedOrderBatchedItems.Value, parentPosition: Int, batchItemPosition: Int) {

        val itemCode = batchItem.ItemCode
        val batch = batchItem.Batch
        //val binManaged = list[parentPosition].BinManaged
        Log.e("BIN_MASTER", "Scanned Item => ${toSimpleJson(batchItem)}")
        //if (binManaged == "Y") {
        val fromWarehouseCode = sessionManagement.getWarehouseCode(context, AppConstants.FROM_WAREHOUSE)
        val toWarehouseCode = sessionManagement.getWarehouseCode(context, AppConstants.TO_WAREHOUSE)
        val fromInvReqWarehouse = sessionManagement.getInvReqWarehouseCode(context)
        if (fromInvReqWarehouse.equals(fromWarehouseCode, true)) {
            Log.e("BIN_MASTER", "if => fromWarehouseCode: $fromWarehouseCode, fromInvReqWarehouse: $fromInvReqWarehouse,   toWarehouseCode: $toWarehouseCode")
            callBinLocationByWarehouse(context, fromWarehouseCode.toString(), toWarehouseCode.toString(), parentPosition, itemCode, batch, tvTotalScannQty) { updatedQty ->
                etBatchQuantity.setText(updatedQty)
            }
        } else {
            Log.e("BIN_MASTER", "else => fromWarehouseCode: $fromWarehouseCode, fromInvReqWarehouse: $fromInvReqWarehouse,   toWarehouseCode: $toWarehouseCode")
            callBinLocationByWarehouse(context, fromInvReqWarehouse.toString(), toWarehouseCode.toString(), parentPosition, itemCode, batch, tvTotalScannQty) { updatedQty ->
                etBatchQuantity.setText(updatedQty)
            }
        }
        /*} else {
            GlobalMethods.showMessage(context, "No need to select bin location for without bin managed item.")
        }*/

    }

    interface QtyListener {
        fun onQtyChanged(totalScannedQty: Double)
    }


}
