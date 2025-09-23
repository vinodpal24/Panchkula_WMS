package com.wms.panchkula.ui.inventoryTransfer

import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import android.widget.Toast
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
import com.wms.panchkula.Retrofit_Api.ApiConstantForURL
import com.wms.panchkula.databinding.ActivityInventoryLineTransferBinding
import com.wms.panchkula.ui.goodsOrder.adapter.GoodsChanchalAdapter
import com.wms.panchkula.ui.goodsOrder.adapter.GoodsItemLineAdapter
import com.wms.panchkula.ui.goodsOrder.model.GoodsIssueSeriesModel
import com.wms.panchkula.ui.goodsOrder.model.LocalListForGoods
import com.wms.panchkula.ui.home.HomeActivity
import com.wms.panchkula.ui.issueForProductionOrder.Model.ScanedOrderBatchedItems
import com.wms.panchkula.ui.issueForProductionOrder.UI.qrScannerUi.QRScannerActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.pixplicity.easyprefs.library.Prefs
import com.webapp.internetconnection.CheckNetwoorkConnection
import com.wms.panchkula.ui.inventoryOrder.Model.InventoryPostResponse
import com.wms.panchkula.ui.inventoryTransfer.model.CardItem
import com.wms.panchkula.ui.inventoryTransfer.model.Warehouse_BPLID
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.IOException

class InventoryLineTransferActivity : AppCompatActivity(), GoodsItemLineAdapter.OnDeleteItemClickListener, GoodsChanchalAdapter.OnItemActionListener {

    lateinit var binding: ActivityInventoryLineTransferBinding
    lateinit var materialProgressDialog:    MaterialProgressDialog
    lateinit var checkNetwoorkConnection:   CheckNetwoorkConnection
    private lateinit var sessionManagement: SessionManagement
    private lateinit var networkConnection: NetworkConnection

    var itemLineArrayList: ArrayList<ScanedOrderBatchedItems.Value> =
        ArrayList<ScanedOrderBatchedItems.Value>()
    var batchQuantityList: ArrayList<String> = ArrayList<String>()
    var serialQuantityList: ArrayList<String> = ArrayList<String>()


    var quantityList_gl: ArrayList<String> = ArrayList<String>()
    private var scanedBatchedItemsList_gl: ArrayList<ScanedOrderBatchedItems.Value> = ArrayList()
    private lateinit var recyclerView: RecyclerView
    var batchItemsAdapter: GoodsItemLineAdapter? = null
    private var pos: Int = 0
    private var itemCode = ""
    val REQUEST_CODE     = 100
    var fromWhareHouse =""
    var fromBinLocation =""
    var toBinLocation =""
    var toWhareHouse =""
    var series       =""
    var BPLID        =""
    var BinManaged   =""

    //todo new adapter
//    var goodsOrderLineAdapter: GoodsShubhAdapter? = null
    var goodsOrderLineAdapter: GoodsChanchalAdapter? = null

    private lateinit var adapter: CardItemAdapter
    private val cardItems = mutableListOf<CardItem>()


    companion object {
        private const val TAG = "DemoActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityInventoryLineTransferBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.hide()
//todo new code for test good items
//        getDataToPostOnJson()
//        getPostJson()

        materialProgressDialog = MaterialProgressDialog(this@InventoryLineTransferActivity)
        checkNetwoorkConnection = CheckNetwoorkConnection(application)
        sessionManagement = SessionManagement(this@InventoryLineTransferActivity)
        networkConnection = NetworkConnection()

        title = " Inventory Transfer "

        binding.layFromWarehouse.visibility = View.VISIBLE
        binding.layToWarehouse.visibility   = View.VISIBLE
        binding.headComponent.visibility    = View.VISIBLE
        binding.ivAdd.visibility            = View.VISIBLE



        //todo setupNewAdapter for itemarrayaLIst
        goodsOrderLineAdapter = GoodsChanchalAdapter(this, AppConstants.scannedItemForGood, binding.tvTotalScannQty,this)
        binding.rvBatchItems.apply {
            adapter       = goodsOrderLineAdapter
            layoutManager = LinearLayoutManager(this@InventoryLineTransferActivity)
            //  setHasFixedSize(true)
        }
        goodsOrderLineAdapter!!.notifyDataSetChanged()



        //todo calling series api here---
        getGoodsIssueSeriesApi()

        //todo HIDE
        Handler(Looper.getMainLooper()).postDelayed({
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
            if (imm != null && binding.edBatchCodeScan != null) {
                imm.hideSoftInputFromWindow(binding.edBatchCodeScan.windowToken, 0)
            }
        }, 200)

        binding.tvDate.setText(GlobalMethods.getCurrentDate_dd_MM_yyyy())


        //todo if leaser type choose..
        if (sessionManagement.getScannerType(this@InventoryLineTransferActivity) == "LEASER") {

            binding.ivScanBatchCode.visibility = View.GONE

            binding.edBatchCodeScan.requestFocus()

            //todo HIDE
            Handler(Looper.getMainLooper()).postDelayed({
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                if (imm != null && binding.edBatchCodeScan != null) {
                    imm.hideSoftInputFromWindow(binding.edBatchCodeScan.windowToken, 0)
                }
            }, 200)


            binding.edFromWhareHouse.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(charSequence: CharSequence?, start: Int, count: Int, after: Int) {
                    // You can handle the text before it changes here
                }

                override fun onTextChanged(charSequence: CharSequence?, start: Int, before: Int, count: Int) {
                    // This method is called when the text changes
                    val text = charSequence.toString()
                    if(text.trim().isNotEmpty()){
                        var arr = text.split(",")
                        if(arr.size>0)
                        {
                            fromWhareHouse = arr[0].toString().trim()
                             if(text.contains("-BIN-LOCATION"))
                             {
                             fromBinLocation = arr[2].toString().trim()
                             }
                            if(fromWhareHouse.isNotEmpty()){
                                getBPLID(fromWhareHouse,"INVT")
                            }

                        }
                       // binding.edFromWhareHouse.setText("")
                    }
                }

                override fun afterTextChanged(editable: Editable?) {
                    // You can handle after text is changed here
                }
            })
            binding.edToWhareHouse.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(charSequence: CharSequence?, start: Int, count: Int, after: Int) {
                    // You can handle the text before it changes here
                }

                override fun onTextChanged(charSequence: CharSequence?, start: Int, before: Int, count: Int) {
                    // This method is called when the text changes
                    val text = charSequence.toString()
                   if(text.trim().isNotEmpty()){
                       var arr = text.split(",")
                       if(arr.size>0)
                       {
                           toWhareHouse = arr[0].toString().trim()
                           if(text.contains("-BIN-LOCATION"))
                           {
                               toBinLocation = arr[2].toString().trim()
                           }
                       }
                     //  binding.edToWhareHouse.setText("")
                   }
                }

                override fun afterTextChanged(editable: Editable?) {
                    // You can handle after text is changed here
                }
            })



            binding.edBatchCodeScan.addTextChangedListener(object : TextWatcher
             {
                override fun afterTextChanged(s: Editable?) {
                    //todo HIDE
                    Handler(Looper.getMainLooper()).postDelayed({
                        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                        if (imm != null && binding.edBatchCodeScan != null) {
                            imm.hideSoftInputFromWindow(binding.edBatchCodeScan.windowToken, 0)
                        }
                    }, 200)

                    val text = s.toString().trim()

                    if (text.isNotEmpty()) {
                        try {
                            val parts = text.toString().split(",")

                            val lastPart = parts.last()
                            var itemCode = parts[0]

                            type = lastPart
                            BatchScannedData =text.toString();
                            //todo set validation for duplicate item
                            if (type == "Batch") {
                                val batch = text.split(",")[1]
                                if (checkDuplicate(AppConstants.scannedItemForGood, batch)) {
                                    //todo scan call api here...
                                    scanBatchLinesItem(batch, binding.rvBatchItems, pos, itemCode, binding.tvTotalScannQty, type)

                                }
                            } else if (type.equals("Serial")) {
                                val batch = text.split(",")[1]
                                if (checkDuplicateForSerial(AppConstants.scannedItemForGood, batch)) {
                                    //todo scan call api here...
                                    scanSerialLineItem(batch, binding.rvBatchItems, pos, itemCode, binding.tvTotalScannQty, type)
                                }
                            }
                            else if (type.equals("NONE") || type.equals("None")){
                                var scanItem = text.toString()
                                val parts = text.toString().split(",")

                                val lastPart = parts.last()
                                var itemCode = parts[0]
                                itemDesc = parts[2]

                                type = lastPart


                                 if (checkDuplicateForNone(AppConstants.scannedItemForGood, scanItem)) {
                                     //todo scan call api here...
                                     callNoneBindFunction(itemCode, binding.rvBatchItems, pos, binding.tvTotalScannQty, itemDesc, scanItem)

                                 }


                            }

                            // Clear the EditText and request focus
                            binding.edBatchCodeScan.setText("")
                            binding.edBatchCodeScan.requestFocus()

                            Handler(Looper.getMainLooper()).postDelayed({
                                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                                imm?.hideSoftInputFromWindow(binding.edBatchCodeScan.windowToken, 0)
                            }, 200)
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
        else if (sessionManagement.getScannerType(this@InventoryLineTransferActivity) == "QR_SCANNER" || sessionManagement.getScannerType(this@InventoryLineTransferActivity) == null) { //|| sessionManagement.getLeaserCheck()!! == 0 && sessionManagement.getQRScannerCheck()!! == 1 || sessionManagement.getLeaserCheck()!! == 0 && sessionManagement.getQRScannerCheck()!! == 0
            binding.ivScanBatchCode.visibility = View.VISIBLE

            //TODO click on barcode scanner for popup..
            binding.ivScanBatchCode.setOnClickListener {
                var text = binding.edBatchCodeScan.text.toString().trim()
                recyclerView = binding.rvBatchItems
//                itemCode = this.ItemNo.toString()


                if (sessionManagement.getScannerType(this@InventoryLineTransferActivity) == null) {
                    showPopupNotChooseScanner()
                } else if (sessionManagement.getScannerType(this@InventoryLineTransferActivity) == "QR_SCANNER") {
                    val intent = Intent(this@InventoryLineTransferActivity, QRScannerActivity::class.java)
                    (this@InventoryLineTransferActivity as InventoryLineTransferActivity).startActivityForResult(intent, REQUEST_CODE)
                }

            }


        }


        binding.chipSave.setOnClickListener {
            Log.e("ScannedData==>",""+BatchScannedData.toString())
          //  var batchInDate = BatchScannedData.split(",")[5].replace("-","")
            //getQuantityForSuggestion(textMain, itemCodeMain, batchInDate )


            // callGoodsPostingApi()

            getPostJson()
        }

        binding.chipCancel.setOnClickListener {
            onBackPressed()
            finish()
            AppConstants.scannedItemForGood.clear()
        }

        // tarun work
        // Initialize data
        cardItems.add(
            CardItem(
             title = "Released"
        )
        )

        // Set up the adapter with the initial data
        adapter = CardItemAdapter(cardItems, this@InventoryLineTransferActivity)

        binding.rvCards.adapter = adapter

        binding.ivAdd.setOnClickListener {
            addNewCard()
        }


    }

    private fun addNewCard() {
        // Create a new card item with placeholder data
        val newCard = CardItem(
            title = "Released"
        )

        // Add the new card to the list
        cardItems.add(newCard)

        // Notify the adapter that the data has changed so it can update the RecyclerView
        adapter.notifyItemInserted(cardItems.size - 1)
    }



    override fun onBackPressed() {
        super.onBackPressed()
        finish()
        AppConstants.scannedItemForGood.clear()
    }


    lateinit var batchList: List<ScanedOrderBatchedItems.Value>

    private fun getJsonArray(list: List<LocalListForGoods>, batcharraySize: Int): JsonArray {

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
                if(!fromBinLocation.isEmpty())
                {
                    fromObject.addProperty("BinAbsEntry",fromBinLocation)
                    fromObject.addProperty("Quantity",list.get(i).Quantity)
                    fromObject.addProperty("BinActionType",    "batFromWarehouse",)
                    fromObject.addProperty("SerialAndBatchNumbersBaseLine",baseLine)
                    stockBin.add(fromObject)
                }


                var ToObject = JsonObject()

                ToObject.addProperty("BinAbsEntry",toBinLocation)
                ToObject.addProperty("Quantity",list.get(i).Quantity)
                ToObject.addProperty("BinActionType",    "batToWarehouse",)
                ToObject.addProperty("SerialAndBatchNumbersBaseLine",baseLine)

                stockBin.add(ToObject)
            }}

        return stockBin
    }

    //todo shubh new code to bificate list according to Abinas sir
    private fun getPostJson() {

        if (networkConnection.getConnectivityStatusBoolean(applicationContext)) {
            materialProgressDialog.show()

            val groupedItems = AppConstants.scannedItemForGood.groupBy { it.ItemCode }

            val documentLinesArray = JsonArray()


            groupedItems.forEach { (itemCode, items) ->

                var itemJsonObject = JsonObject()

                val batchNumbersArray = JsonArray()
                val serialNumbersArray = JsonArray()
                val noneNumbersObj = JsonObject()
                var warehouseCode = ""

                items.forEach { item ->

                    item.Batch?.let { batch ->
                        if (batch.isNotEmpty()) {
                            itemJsonObject = JsonObject().apply {
                                addProperty("ItemCode", itemCode)
                                addProperty("WarehouseCode", toWhareHouse)
                                addProperty("FromWarehouseCode", fromWhareHouse)
                                addProperty("Quantity", items.sumOf { if (it.ScanType == "None") 0 else GlobalMethods.changeDecimal(it.Quantity)!!.toInt()})
                            }
                            val batchNumberParts = batch.split(",")
                            if (item.ItemCode == itemCode && !item.ScanType.equals("None", ignoreCase = true)) {//batchNumberParts.size > 0 && batchNumberParts[0] == itemCode && !item.ScanType.equals("None", ignoreCase = true)
                                val batchObject = JsonObject().apply {
                                    addProperty("BatchNumber", batch)
                                    addProperty("Quantity", item.Quantity)
//                                    addProperty("WareHouseCode", item.WareHouseCode)
                                }
                                batchNumbersArray.add(batchObject)
                            }
                            //todo remove shubh so that get warehouse from inner
                            warehouseCode = item.WareHouseCode

                        }



                    }

                    item.SerialNumber?.let { serial ->
                        if (serial.isNotEmpty()) {
                            itemJsonObject = JsonObject().apply {
                                addProperty("ItemCode", itemCode)
                                addProperty("WarehouseCode", toWhareHouse)
                                addProperty("FromWarehouseCode", fromWhareHouse)
                                addProperty("Quantity", items.sumOf { if (it.ScanType == "None") 0 else GlobalMethods.changeDecimal(it.Quantity)!!.toInt()})
                            }

                            val serialNumberParts = serial.split(",")
                            if ( item.ItemCode == itemCode && !item.ScanType.equals("None", ignoreCase = true)) {//serialNumberParts.size > 0 &&
                                val serialObject = JsonObject().apply {
                                    addProperty("SystemSerialNumber", item.SystemSerialNumber)
                                   // addProperty("InternalSerialNumber", item.InternalSerialNumber)
                                    addProperty("Quantity", "1")
//                                    addProperty("WareHouseCode", item.WareHouseCode)
                                }
                                serialNumbersArray.add(serialObject)
                            }
                            warehouseCode = item.WareHouseCode
                        }
                    }
                }

                if(fromBinLocation.isNotEmpty()){
                    itemJsonObject.add("StockTransferLinesBinAllocations", getJsonArray(items,items.size))
                }

                if (batchNumbersArray.size() > 0) {
                    itemJsonObject.add("BatchNumbers", batchNumbersArray)
                }

                if (serialNumbersArray.size() > 0) {
                    itemJsonObject.add("SerialNumbers", serialNumbersArray)
                }

                if (warehouseCode.isNotEmpty()) {
                    itemJsonObject.addProperty("WarehouseCode", warehouseCode)
                }

                if (itemJsonObject.entrySet().size > 1) { // Ensure that the object has properties other than "ItemCode"
                    documentLinesArray.add(itemJsonObject)
                }
//                documentLinesArray.add(itemJsonObject)

                for (current in items) {
                    if (current.ScanType.equals("None", ignoreCase = true)) {
                        val serialObject = JsonObject().apply {
                            addProperty("ItemCode", current.ItemCode)
                            addProperty("Quantity", current.Quantity.toInt()) //items.sumOf { current.Quantity.toInt() } in case if send sum of all None Items
                            addProperty("WarehouseCode", current.WareHouseCode)
                        }

                        documentLinesArray.add(serialObject)
                    }

                }


            }

            val finalJsonObject = JsonObject().apply {

                addProperty("DocDate", GlobalMethods.getCurrentDateFormatted())
                addProperty("DueDate", GlobalMethods.getCurrentDateFormatted())
                addProperty("TaxDate", GlobalMethods.getCurrentDateFormatted())
                addProperty("BPLID", BPLID)
                addProperty("Series",series)
                addProperty("FromWarehouse",fromWhareHouse)
                addProperty("ToWarehouse",toWhareHouse)
                add("StockTransferLines", documentLinesArray)
            }

            val gson = Gson()
            val json = gson.toJson(finalJsonObject)
            println(json)

            Log.e(TAG, "PayLoad: $json")
            var apiConfig = ApiConstantForURL()

            NetworkClients.updateBaseUrlFromConfig(apiConfig)

            QuantityNetworkClient.updateBaseUrlFromConfig(apiConfig)

            val networkClient = NetworkClients.create(this@InventoryLineTransferActivity)
            networkClient.dostockTransfer(finalJsonObject).apply {
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
                                Log.d("Doc_Num", "onResponse: "+response.body()!!.DocNum.toString())
                                GlobalMethods.showSuccess(this@InventoryLineTransferActivity, "Post Successfully. "+response.body()!!.DocNum.toString())
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
                                            this@InventoryLineTransferActivity,
                                            mError.error.message.value
                                        )
                                    }
                                    if (mError.error.message.value != null) {
                                        GlobalMethods.showError(
                                            this@InventoryLineTransferActivity,
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
                Toast.makeText(this@InventoryLineTransferActivity, "No Network Connection", Toast.LENGTH_SHORT).show()
            }
        }


    }



    //todo new interface for new adapter
    override fun onQuantityChanged(position: Int, newQuantity: String, tvBatchQuantity : TextInputEditText) {
        var maxQuantity = GlobalMethods.changeDecimal(AppConstants.scannedItemForGood[position].FixedQuantity)!!.toDoubleOrNull() ?: 0.0
        var QUANTITYVAL = newQuantity

        val value = QUANTITYVAL.toIntOrNull() ?: 0

        if (QUANTITYVAL.isEmpty()){
            QUANTITYVAL = "0"
        }

        if (!QUANTITYVAL.isNullOrEmpty()) {

           /* if (maxQuantity != null && value > maxQuantity)
               {
            GlobalMethods.showError(this, "Value cannot exceed then Open Quantity")
            tvBatchQuantity.setText("")
               }
            else { }*/

                AppConstants.scannedItemForGood[position].Quantity = QUANTITYVAL
                val totalQuantity = AppConstants.scannedItemForGood.sumOf {
                    GlobalMethods.changeDecimal(it.Quantity)!!.toInt()
                }

                binding.tvTotalScannQty.text = ": $totalQuantity"

                Log.e(TAG, "onQuantityChanged: ${AppConstants.scannedItemForGood[position].Quantity}" )



        }

 /*

        AppConstants.scannedItemForGood[position].Quantity = QUANTITYVAL
        val totalQuantity = AppConstants.scannedItemForGood.sumOf {
                GlobalMethods.changeDecimal(it.Quantity)!!.toInt()
        }

        binding.tvTotalScannQty.text = ": $totalQuantity"

        Log.e(TAG, "onQuantityChanged: ${AppConstants.scannedItemForGood[position].Quantity}" )
*/

    }


    //todo new interface of nw adapter
    override fun onItemRemoved(position: Int) {
        var batch = ""

        if (!AppConstants.scannedItemForGood[position].Batch.isNullOrEmpty()) {
            batch = AppConstants.scannedItemForGood[position].Batch.toString()
        } else
            batch = AppConstants.scannedItemForGood[position].SerialNumber.toString()


        MaterialAlertDialogBuilder(this)
            .setTitle("Confirm...")
            .setMessage("Do you want to delete " + batch + " Item .")
            .setIcon(R.drawable.ic_trash)
            .setPositiveButton("Confirm",
                DialogInterface.OnClickListener { dialogInterface: DialogInterface?, i1: Int ->
                    goodsOrderLineAdapter!!.removeItem(position)
                    goodsOrderLineAdapter!!.notifyDataSetChanged()
                    val totalQuantity = AppConstants.scannedItemForGood.sumOf {
                            GlobalMethods.changeDecimal(it.Quantity)!!.toInt()
                        }

                    binding.tvTotalScannQty.text = ": $totalQuantity"
                    Log.e(TAG, "onItemRemoved: ${AppConstants.scannedItemForGood.size}" )
                    Log.e(TAG, "onItemRemoved: ${AppConstants.scannedItemForGood}" )


                })
            .setNegativeButton("Cancel",
                DialogInterface.OnClickListener { dialogInterface, i ->
                    dialogInterface.dismiss()
                })

            .show()


    }


    var wareHouseCode =""
    //todo changess by shubh
    override fun onWareHouseChanged(position: Int, newQuantity: String, warehouse: String, currentItem: LocalListForGoods) {
        AppConstants.scannedItemForGood[position].Quantity = newQuantity

        AppConstants.scannedItemForGood[position].FixedQuantity = newQuantity
        wareHouseCode =warehouse
        AppConstants.scannedItemForGood[position].WareHouseCode = warehouse
        val totalQuantity = AppConstants.scannedItemForGood.sumOf {
            GlobalMethods.changeDecimal(it.Quantity)!!.toDouble()
        }

        binding.tvTotalScannQty.text = ": $totalQuantity"

        Log.e(TAG, "onQuantityChanged: ${AppConstants.scannedItemForGood[position].Quantity}")
        Log.e(TAG, "onQuantityChanged: ${AppConstants.scannedItemForGood[position].FixedQuantity}")
        Log.e(TAG, "onQuantityChanged: ${AppConstants.scannedItemForGood[position].WareHouseCode}")

    }

    val handler = Handler(Looper.getMainLooper())

    var type = ""
    var itemDesc = ""

    //todo onActivity function override for qr code scanning in adapter..
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK) {

            Log.e("Result==>", data?.getStringExtra("batch_code").toString())
            if (requestCode == REQUEST_CODE) {
                val result = data?.getStringExtra("batch_code")

                val list = itemLineArrayList as List<*>
                Log.e("size===>", list.size.toString())
                Log.e("ItemCode===>", itemCode)

                //todo spilt string and get string at 0 index...

                if (result!!.isNotEmpty()) {
                    // Split the string by "~"
                    val parts = result.toString().split(",")

                    val lastPart = parts.last()
                    var itemCode = parts[0]

                    type = lastPart
                    BatchScannedData =result.toString();
                    //todo set validation for duplicate item
                    if (type == "Batch") {
                        if (checkDuplicate(AppConstants.scannedItemForGood, result.toString().split(",")[1])) {//checkDuplicate(itemLineArrayList, result.toString().split(",")[0])
                            //todo scan call api here...


                            scanBatchLinesItem(result.toString().split(",")[1], recyclerView, pos, itemCode, binding.tvTotalScannQty, type)



                        }
                    } else if (type.equals("Serial")) {
                        if (checkDuplicateForSerial(AppConstants.scannedItemForGood, result.toString().split(",")[1])) {
                            //todo scan call api here...
                            scanSerialLineItem(result.toString().split(",")[1], recyclerView, pos, itemCode, binding.tvTotalScannQty, type)
                        }
                    }
                    else if (type.equals("NONE") || type.equals("None")){
                        var scanItem = result.toString().split(",")[0]
                        val parts = result.toString().split(",")

                        val lastPart = parts.last()
                        var itemCode = parts[0]
                        itemDesc = parts[2]

                        type = lastPart

                        if (checkDuplicateForNone(AppConstants.scannedItemForGood, scanItem)) {
                            callNoneBindFunction(itemCode, recyclerView, pos, binding.tvTotalScannQty, itemDesc, scanItem)

                        }

                    }else{
                        GlobalMethods.showMessage(this@InventoryLineTransferActivity, "Scan Type is " + type )
                    }


                }

            }

        }
    }


    private fun showPopupNotChooseScanner() {
        val builder =
            AlertDialog.Builder(this@InventoryLineTransferActivity, R.style.CustomAlertDialog).create()
        val view =
            LayoutInflater.from(this@InventoryLineTransferActivity).inflate(R.layout.custom_popup_alert, null)
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
            var intent = Intent(this@InventoryLineTransferActivity, HomeActivity::class.java)
            startActivity(intent)
            builder.dismiss()

        }

        builder.setCancelable(true)
        builder.show()
    }


    //TODO duplicatcy checking from list...
    fun checkDuplicate(scanedBatchedItemsList_gl: MutableList<LocalListForGoods>, batchCode: String): Boolean {
        var startus: Boolean = true;
        for (items in scanedBatchedItemsList_gl) {
            if (items.Batch == null) {
                startus = true
            } else if (items.Batch.equals(batchCode)) {
                startus = false
                Toast.makeText(this, "Batch no. Already Exists!", Toast.LENGTH_SHORT).show()
            }
        }
        return startus
    }


    fun checkDuplicateForSerial(scanedBatchedItemsList_gl: MutableList<LocalListForGoods>, batchCode: String): Boolean {
        var startus: Boolean = true;
        for (items in scanedBatchedItemsList_gl) {
            if (items.SerialNumber == null) {
                startus = true
            } else if (items.SerialNumber.equals(batchCode)) {
                startus = false
                Toast.makeText(this, "Serial no. Already Exists!", Toast.LENGTH_SHORT).show()
            }
        }
        return startus
    }


    fun checkDuplicateForNone(scanedBatchedItemsList_gl: MutableList<LocalListForGoods>, batchCode: String): Boolean {
        var startus: Boolean = true;
        for (items in scanedBatchedItemsList_gl) {
            if (items.NoneVal == null) {
                startus = true
            } else if (items.NoneVal.equals(batchCode)) {
                startus = false
                Toast.makeText(this, "None no. Already Exists!", Toast.LENGTH_SHORT).show()
            }
        }
        return startus
    }


    override fun onDeleteItemClick(list: ArrayList<ScanedOrderBatchedItems.Value>, quantityHashMap1: ArrayList<String>, pos: Int, batchTypeList: ArrayList<String>, serialTypeList: ArrayList<String>) {
        var batch = ""

        if (!list[pos].Batch.isNullOrEmpty()) {
            batch = list[pos].Batch.toString()
        } else
            batch = list[pos].SerialNumber.toString()
//todo use when call bind fun.

        MaterialAlertDialogBuilder(this)
            .setTitle("Confirm...")
            .setMessage("Do you want to delete " + batch + " Item .")
            .setIcon(R.drawable.ic_trash)
            .setPositiveButton("Confirm",
                DialogInterface.OnClickListener { dialogInterface: DialogInterface?, i1: Int ->

                    //todo remove particular line from lists-
                    list.removeAt(pos)

                    quantityHashMap1.removeAt(pos)

                    batchTypeList.removeAt(pos)

                    serialTypeList.removeAt(pos)


                    batchItemsAdapter?.notifyDataSetChanged()


                    var data = GlobalMethods.sumBatchQuantity(pos, quantityHashMap1!!).toString()
                    Log.e("data_value===>", data.toString())
                    binding.tvTotalScannQty.setText(":   " + data)

                    batchQuantityList.addAll(batchTypeList)
                    serialQuantityList.addAll(serialTypeList)
                    quantityList_gl.addAll(quantityHashMap1)
                    itemLineArrayList.addAll(list)


                    Log.e("itemLineArrayList===>", itemLineArrayList.size.toString())
                    Log.e("list===>", list.size.toString())
                    Log.e("before_batch===>", quantityHashMap1.size.toString())
                    Log.e("batchTypeList===>", batchTypeList.size.toString())
                    Log.e("serialTypeList===>", serialTypeList.size.toString())
                })
            .setNegativeButton("Cancel",
                DialogInterface.OnClickListener { dialogInterface, i ->
                    dialogInterface.dismiss()
                })
            .show()
    }

    var BatchScannedData =""
    var textMain =""
    var itemCodeMain =""
    var tvTotalScannQty =""
    var rvBatchItems = ""
    var itemList_gl = ""
    var typeMain          = ""

    //TODO scan item lines api here....
    private fun scanBatchLinesItem(text: String, rvBatchItems: RecyclerView, position: Int, itemCode: String?, tvTotalScannQty: TextView, type: String) {
        if (networkConnection.getConnectivityStatusBoolean(this)) {
            materialProgressDialog.show()
            var apiConfig = ApiConstantForURL()

            NetworkClients.updateBaseUrlFromConfig(apiConfig)

            QuantityNetworkClient.updateBaseUrlFromConfig(apiConfig)

            val networkClient = NetworkClients.create(this)
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
                                    if (responseModel.value.size > 0 && !responseModel.value.isNullOrEmpty()) {
                                        var modelResponse = responseModel.value
                                        scanedBatchedItemsList_gl.addAll(modelResponse)

                                        var itemList_gl: ArrayList<ScanedOrderBatchedItems.Value> = ArrayList()
                                        itemList_gl.clear()
                                        itemList_gl.add(responseModel.value[0])


                                        if (!itemList_gl.isNullOrEmpty()) {

                                            Log.e("list_size-----", itemList_gl.size.toString())

                                            //todo quantity..

                                            textMain            = text
                                            itemCodeMain            = itemList_gl[0].ItemCode
                                            typeMain          = type

                                            getQuantityFromApi(text, itemList_gl[0].ItemCode, tvTotalScannQty, rvBatchItems, itemList_gl, type)

                                        }
                                    } else {
                                        GlobalMethods.showError(this@InventoryLineTransferActivity, "Invalid Batch Code")
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
                                                this@InventoryLineTransferActivity,
                                                mError.error.message.value
                                            )
                                        }
                                        if (mError.error.message.value != null) {
                                            GlobalMethods.showError(
                                                this@InventoryLineTransferActivity,
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

                        override fun onFailure(call: Call<ScanedOrderBatchedItems>, t: Throwable) {
                            Log.e("scanItemApiFailed-----", t.toString())
                            materialProgressDialog.dismiss()
                        }

                    })
                }
        } else {
            materialProgressDialog.dismiss()
            Toast.makeText(this@InventoryLineTransferActivity, "No Network Connection", Toast.LENGTH_SHORT)
                .show()
        }
    }

    private fun getBPLID(warehouseCode: String,DocType:String) {
        if (networkConnection.getConnectivityStatusBoolean(this)) {
            materialProgressDialog.show()
            var apiConfig = ApiConstantForURL()

            NetworkClients.updateBaseUrlFromConfig(apiConfig)

            QuantityNetworkClient.updateBaseUrlFromConfig(apiConfig)

            val networkClient = QuantityNetworkClient.create(this)
            val bplId = if (Prefs.getString(AppConstants.BPLID,"").isNotEmpty()) Prefs.getString(AppConstants.BPLID,"") else ""

            networkClient.doGetBPLID(warehouseCode,DocType,bplId)
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
                                        GlobalMethods.showError(this@InventoryLineTransferActivity, "Invalid Batch Code")
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
                                                this@InventoryLineTransferActivity,
                                                mError.error.message.value
                                            )
                                        }
                                        if (mError.error.message.value != null) {
                                            GlobalMethods.showError(
                                                this@InventoryLineTransferActivity,
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
            Toast.makeText(this@InventoryLineTransferActivity, "No Network Connection", Toast.LENGTH_SHORT)
                .show()
        }
    }



    private fun getQuantityForSuggestion(
        batchCode: String,
        itemCode: String,

        batchInDate : String
    ) {
        if (networkConnection.getConnectivityStatusBoolean(this@InventoryLineTransferActivity)) {
            materialProgressDialog.show()
            var apiConfig = ApiConstantForURL()

            NetworkClients.updateBaseUrlFromConfig(apiConfig)

            QuantityNetworkClient.updateBaseUrlFromConfig(apiConfig)
            var remQt     = batchInDate
            Log.e("Okh Test==>",""+remQt)
            val networkClient = QuantityNetworkClient.create(this@InventoryLineTransferActivity)
            networkClient.getQuantityForSuggestion(sessionManagement.getCompanyDB(this@InventoryLineTransferActivity)!!, remQt, itemCode,wareHouseCode)//AppConstants.COMPANY_DB,   list.get(position).FromWarehouseCode
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
                                        if(responseModel.value[0].Quantity.toString().trim().toDouble()>0.0)
                                        {
                                            AppConstants.showAlertDialog(this@InventoryLineTransferActivity,responseModel.value[0].Batch, responseModel.value[0].Quantity)

                                          /*  hashMap.get("Item" + position)!!.clear()
                                            batchItemsAdapter?.notifyDataSetChanged()
                                            //  quantityHashMap.remove("Item" + position)
                                            Log.e("Okh Remove",quantityHashMap.get("Item" + position)?.size.toString())
                                                  if (quantityHashMap.isNotEmpty() && quantityHashMap.containsKey("Item" + position))
                                                  {
                                                     quantityHashMap.remove("Item" + position)
                                                     // hashMap.remove("Item" + position, itemList_gl)
                                                      hashMap.get("Item" + position)?.removeAt(0)
                                                      batchItemsAdapter?.removeItem(position)
                                                      batchItemsAdapter?.notifyDataSetChanged()

                                                      Log.e("Okh Remove-11 ",quantityHashMap.size.toString())
                                                  }*/



                                        }
                                        else
                                        {
                                            binding.chipSave.isEnabled = false
                                            binding.chipSave.isCheckable = false
                                            Log.e("Bhupi","Hitting")
                                            getPostJson()
                                        }



                                    } else {
                                        GlobalMethods.showError(this@InventoryLineTransferActivity, "No Quantity")
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
                                            GlobalMethods.showError(this@InventoryLineTransferActivity, mError.error.message.value)
                                        }
                                        if (mError.error.message.value != null) {
                                            GlobalMethods.showError(this@InventoryLineTransferActivity, mError.error.message.value)
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
            Toast.makeText(this@InventoryLineTransferActivity, "No Network Connection", Toast.LENGTH_SHORT).show()
        }
    }


    //TODO scan item lines api here....

    var tempList: ArrayList<String> = ArrayList()


    private fun getQuantityFromApi(batchCode: String, itemCode: String, tvTotalScannQty: TextView, rvBatchItems: RecyclerView, itemList_gl: ArrayList<ScanedOrderBatchedItems.Value>, type: String) {

        //todo changes by shubh for warehouse listing
        if (networkConnection.getConnectivityStatusBoolean(this@InventoryLineTransferActivity)) {
            materialProgressDialog.show()
            var apiConfig = ApiConstantForURL()

            NetworkClients.updateBaseUrlFromConfig(apiConfig)

            QuantityNetworkClient.updateBaseUrlFromConfig(apiConfig)

            val networkClient = QuantityNetworkClient.create(this@InventoryLineTransferActivity)
            networkClient.getQuantityGoodsWithWareHouseCode(sessionManagement.getCompanyDB(applicationContext)!!,itemCode,type, batchCode)//getQuantityValue(batchCode, itemCode, "RM-309")//sessionManagement.getWarehouseCode(this@GoodsOrderActivity)!!
                .enqueue(object : Callback<GetQuantityModel> {
                    override fun onResponse(
                        call: Call<GetQuantityModel>,
                        response: Response<GetQuantityModel>
                    ) {
                        try {
                            materialProgressDialog.dismiss()
                            if (response.isSuccessful) {
                                val responseModel = response.body()!!
                                if (responseModel.value.isNotEmpty() && !responseModel.value[0].Quantity.isNullOrEmpty() && !responseModel.value[0].Quantity.equals("0.0")) {
                                    tempList.clear()

                                    for (i in itemList_gl.indices) {
                                        itemLineArrayList.add(itemList_gl[i])
                                    }


                                    Log.e("stringList", "Success=>" + responseModel.value)
                                    var stringList: ArrayList<String> = ArrayList()
                                    stringList.clear()
                                    stringList.add(responseModel.value[0].Quantity)


                                    AppConstants.scannedItemForGood.add(
                                        LocalListForGoods(
                                            DocEntry = itemList_gl[0].DocEntry.toInt(),
                                            ItemCode = itemCode,
                                            ItemDescription = itemList_gl[0].ItemDescription,
                                            Status = itemList_gl[0].Status,
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
                                            wareHouseListing = responseModel.value as MutableList<GetQuantityModel.Value>
                                        )
                                    )

                                    Log.e(TAG, "BATCH LOCAL LIST>>>: ${AppConstants.scannedItemForGood}")
                                    Log.e(TAG, " BATCH LOCAL LISTSIZE>>>: ${AppConstants.scannedItemForGood.size}")
                                    goodsOrderLineAdapter!!.notifyDataSetChanged()

                                    val totalQuantity = AppConstants.scannedItemForGood.sumOf {
                                            GlobalMethods.changeDecimal(it.Quantity)!!.toInt()
                                        }

//                                    binding.tvTotalScannQty.text = ": $totalQuantity"


                                    if (stringList.isNotEmpty() && !stringList.contains("0")) {

                                    } else {
                                        //  batchItemsAdapter?.notifyDataSetChanged()
                                        GlobalMethods.showError(this@InventoryLineTransferActivity, "Batch / Roll No. has zero Quantity of this PO.")
                                    }
                                } else {
                                    GlobalMethods.showError(this@InventoryLineTransferActivity, "No Quantity Found of this Goods Issue.")
                                    //   batchItemsAdapter?.notifyDataSetChanged()
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
            Toast.makeText(this@InventoryLineTransferActivity, "No Network Connection", Toast.LENGTH_SHORT)
                .show()
        }
    }


    //TODO scan item lines api here....
    private fun scanSerialLineItem(text: String, rvBatchItems: RecyclerView, position: Int, itemCode: String, tvTotalScannQty: TextView, type: String) {
        if (networkConnection.getConnectivityStatusBoolean(this@InventoryLineTransferActivity)) {
            materialProgressDialog.show()
            var apiConfig = ApiConstantForURL()

            NetworkClients.updateBaseUrlFromConfig(apiConfig)

            QuantityNetworkClient.updateBaseUrlFromConfig(apiConfig)

            val networkClient = NetworkClients.create(this@InventoryLineTransferActivity)
            networkClient.doGetSerialNumScanDetails("SerialNumber eq '" + text + "'" + " and ItemCode eq '" + itemCode + "'")
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
                                    if (responseModel.value.size > 0 && !responseModel.value.isNullOrEmpty()) {
                                        var modelResponse = responseModel.value
                                        scanedBatchedItemsList_gl.addAll(modelResponse)

                                        var itemList_gl: ArrayList<ScanedOrderBatchedItems.Value> = ArrayList()
                                        itemList_gl.clear()
                                        itemList_gl.add(responseModel.value[0])

                                        getQuantityFromApiForSerialType(itemCode, itemList_gl, text, type)



                                    } else {
                                        GlobalMethods.showError(
                                            this@InventoryLineTransferActivity,
                                            "Invalid Batch Code"
                                        )
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
                                                this@InventoryLineTransferActivity,
                                                mError.error.message.value
                                            )
                                        }
                                        if (mError.error.message.value != null) {
                                            GlobalMethods.showError(
                                                this@InventoryLineTransferActivity,
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

                        override fun onFailure(call: Call<ScanedOrderBatchedItems>, t: Throwable) {
                            Log.e("scanItemApiFailed-----", t.toString())
                            materialProgressDialog.dismiss()
                        }

                    })
                }
        } else {
            materialProgressDialog.dismiss()
            Toast.makeText(this@InventoryLineTransferActivity, "No Network Connection", Toast.LENGTH_SHORT)
                .show()
        }
    }


    private fun getQuantityFromApiForSerialType(itemCode: String, itemList_gl: ArrayList<ScanedOrderBatchedItems.Value>, text: String, type: String) {

        //todo changes by shubh for warehouse listing
        if (networkConnection.getConnectivityStatusBoolean(this@InventoryLineTransferActivity)) {
            materialProgressDialog.show()
            var apiConfig = ApiConstantForURL()

            NetworkClients.updateBaseUrlFromConfig(apiConfig)

            QuantityNetworkClient.updateBaseUrlFromConfig(apiConfig)

            val networkClient = QuantityNetworkClient.create(this@InventoryLineTransferActivity)
            networkClient.getQuantityGoodsWithWareHouseCode(sessionManagement.getCompanyDB(applicationContext)!! ,itemCode, type, text)//getQuantityValue(batchCode, itemCode, "RM-309")//sessionManagement.getWarehouseCode(this@GoodsOrderActivity)!!
                .enqueue(object : Callback<GetQuantityModel> {
                    override fun onResponse(
                        call: Call<GetQuantityModel>,
                        response: Response<GetQuantityModel>
                    ) {
                        try {
                            materialProgressDialog.dismiss()
                            if (response.isSuccessful) {
                                val responseModel = response.body()!!
                                if (responseModel.value.isNotEmpty() && !responseModel.value[0].Quantity.isNullOrEmpty() && !responseModel.value[0].Quantity.equals("0.0")) {


                                    for (i in itemList_gl.indices) {
                                        itemLineArrayList.add(itemList_gl[i])
                                    }


                                    var stringList: ArrayList<String> = ArrayList()
                                    //todo changes bu shubh
                                    //   stringList.clear()
                                    stringList.addAll(serialQuantityList)


                                    if (!itemList_gl.isNullOrEmpty()) {

                                        Log.e("list_size-----", itemList_gl.size.toString())

                                        //todo quantity..
                                        stringList.add("1")

                                        serialQuantityList.clear()


                                        //todo adding new serial item in locallstof appconstant
                                        AppConstants.scannedItemForGood.add(
                                            LocalListForGoods(
                                                DocEntry = itemList_gl[0].DocEntry.toInt(),
                                                ItemCode = itemCode.toString(),
                                                ItemDescription = itemList_gl[0].ItemDescription,
                                                Status = itemList_gl[0].Status,
                                                Batch = itemList_gl[0].Batch,
                                                SystemNumber = itemList_gl[0].SystemNumber.toInt(),
                                                SerialNumber = itemList_gl[0].SerialNumber,
                                                ScanType = "Serial",
                                                Quantity = "1",
                                                WareHouseCode = "",
                                                UnitPrice = "",
                                                BatchNumber = itemList_gl[0].Batch,
                                                SystemSerialNumber = itemList_gl[0].SystemNumber,
                                                InternalSerialNumber = itemList_gl[0].SerialNumber,
                                                wareHouseListing = responseModel.value as MutableList<GetQuantityModel.Value>

                                            )
                                        )

                                        Log.e(TAG, "SERAIL LOCAL LIST>>>: ${AppConstants.scannedItemForGood}")
                                        Log.e(TAG, " SERIAL LOCAL LISTSIZE>>>: ${AppConstants.scannedItemForGood.size}")

                                        goodsOrderLineAdapter!!.notifyDataSetChanged()
                                        // Calculate total quantity and set to TextView (assuming you have a TextView instance)
                                        val totalQuantity = AppConstants.scannedItemForGood.sumOf {
                                            GlobalMethods.changeDecimal(it.Quantity)!!.toInt()
                                        }

                                        binding.tvTotalScannQty.text = ": $totalQuantity"


                                    }


                                } else {
                                    GlobalMethods.showError(this@InventoryLineTransferActivity, "No Quantity Found of this Goods Issue.")
                                    //   batchItemsAdapter?.notifyDataSetChanged()
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
            Toast.makeText(this@InventoryLineTransferActivity, "No Network Connection", Toast.LENGTH_SHORT)
                .show()
        }
    }


    var noneQuantityList: ArrayList<String> = ArrayList<String>()


    private fun callNoneBindFunction(itemCode: String, rvBatchItems: RecyclerView, position: Int, tvTotalScannQty: TextView, itemDesc: String, scanItem: String) {

        if (itemCode.isNotEmpty()) {

            var itemList_gl: ArrayList<ScanedOrderBatchedItems.Value> = ArrayList()
            itemList_gl.clear()
            var data = ScanedOrderBatchedItems.Value("0", itemCode, itemDesc, "", "", scanItem,"", "", "", "", "", "", "", 0, "", 0.0, 0.0, 0.0, 0.0, 0.0, "", 0.0, "", 0.0, 0.0, "")

            itemList_gl.add(data)

            for (i in itemList_gl.indices) {
                itemLineArrayList.add(itemList_gl[i])
            }

            getQuantityFromApiForNoneType(itemCode, itemList_gl)





        }

    }


    private fun getQuantityFromApiForNoneType(itemCode: String, itemList_gl: ArrayList<ScanedOrderBatchedItems.Value>) {

        //todo changes by shubh for warehouse listing
        if (networkConnection.getConnectivityStatusBoolean(this@InventoryLineTransferActivity)) {
            materialProgressDialog.show()
            var apiConfig = ApiConstantForURL()

            NetworkClients.updateBaseUrlFromConfig(apiConfig)

            QuantityNetworkClient.updateBaseUrlFromConfig(apiConfig)

            val networkClient = QuantityNetworkClient.create(this@InventoryLineTransferActivity)
            networkClient.getQuantityGoodsWithWareHouseCode(sessionManagement.getCompanyDB(applicationContext)!!,itemCode,"", "")//getQuantityValue(batchCode, itemCode, "RM-309")//sessionManagement.getWarehouseCode(this@GoodsOrderActivity)!!
                .enqueue(object : Callback<GetQuantityModel> {
                    override fun onResponse(
                        call: Call<GetQuantityModel>,
                        response: Response<GetQuantityModel>
                    ) {
                        try {
                            materialProgressDialog.dismiss()
                            if (response.isSuccessful) {
                                val responseModel = response.body()!!

                                if (responseModel.value.isNotEmpty() && !responseModel.value[0].Quantity.isNullOrEmpty() && !responseModel.value[0].Quantity.equals("0.0")) {

                                    for (i in itemList_gl.indices) {
                                        itemLineArrayList.add(itemList_gl[i])
                                    }


                                    var stringList: ArrayList<String> = ArrayList()
                                    stringList.addAll(noneQuantityList)


                                    if (!itemList_gl.isNullOrEmpty()) {

                                        Log.e("list_size-----", itemList_gl.size.toString())

                                        stringList.add("0")

                                        noneQuantityList.clear()

                                        for (i in stringList.indices) {
                                            noneQuantityList.add(stringList[i])
                                            quantityList_gl.add(stringList[i])

                                        }


                                        //todo adding new serial item in locallstof appconstant
                                        AppConstants.scannedItemForGood.add(
                                            LocalListForGoods(
                                                DocEntry = itemList_gl[0].DocEntry.toInt(),
                                                ItemCode = itemList_gl[0].ItemCode,
                                                ItemDescription = itemList_gl[0].ItemDescription,
                                                Status = itemList_gl[0].Status,
                                                Batch = "",
                                                SystemNumber = itemList_gl[0].SystemNumber.toInt(),
                                                SerialNumber = "",
                                                ScanType = "None",
                                                Quantity = "0",
                                                WareHouseCode = "",//responseModel.value[0].WarehouseCode
                                                UnitPrice = "",
                                                BatchNumber = "",
                                                SystemSerialNumber = itemList_gl[0].SystemNumber,
                                                InternalSerialNumber = itemList_gl[0].SerialNumber,
                                                NoneVal = itemList_gl[0].NoneVal,
                                                wareHouseListing = responseModel.value as MutableList<GetQuantityModel.Value>

                                            )
                                        )

                                        Log.e(TAG, "None LOCAL LIST>>>: ${AppConstants.scannedItemForGood}")
                                        Log.e(TAG, " None LOCAL LISTSIZE>>>: ${AppConstants.scannedItemForGood.size}")

                                        goodsOrderLineAdapter!!.notifyDataSetChanged()
                                        // Calculate total quantity and set to TextView (assuming you have a TextView instance)
                                        val totalQuantity = AppConstants.scannedItemForGood.sumOf {
                                            GlobalMethods.changeDecimal(it.Quantity)!!.toInt()
                                        }

//                                        binding.tvTotalScannQty.text = ": $totalQuantity"

                                    }

                                } else {
                                    GlobalMethods.showError(this@InventoryLineTransferActivity, "No Quantity Found of this Goods Issue.")
                                    //   batchItemsAdapter?.notifyDataSetChanged()
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
            Toast.makeText(this@InventoryLineTransferActivity, "No Network Connection", Toast.LENGTH_SHORT)
                .show()
        }
    }


    var seriesData : GoodsIssueSeriesModel = GoodsIssueSeriesModel()

    private fun getGoodsIssueSeriesApi() {

        //todo changes by shubh for warehouse listing
        if (networkConnection.getConnectivityStatusBoolean(this@InventoryLineTransferActivity)) {
            materialProgressDialog.show()
            var apiConfig = ApiConstantForURL()

            NetworkClients.updateBaseUrlFromConfig(apiConfig)

            QuantityNetworkClient.updateBaseUrlFromConfig(apiConfig)

            val networkClient = QuantityNetworkClient.create(this@InventoryLineTransferActivity)
            networkClient.getGoodsIssueSeries(sessionManagement.getCompanyDB(applicationContext)!!)
                .enqueue(object : Callback<GoodsIssueSeriesModel> {
                    override fun onResponse(
                        call: Call<GoodsIssueSeriesModel>,
                        response: Response<GoodsIssueSeriesModel>
                    ) {
                        try {
                            materialProgressDialog.dismiss()
                            if (response.isSuccessful) {
                                val responseModel = response.body()!!
                                seriesData = responseModel
                            } else {
                                Log.e(TAG, "onResponse: "+response.message() )
                            }
                        } catch (e: Exception) {
                            materialProgressDialog.dismiss()
                            e.printStackTrace()
                        }
                    }

                    override fun onFailure(call: Call<GoodsIssueSeriesModel>, t: Throwable) {
                        Log.e("scanItemApiFailed-----", t.toString())
                        materialProgressDialog.dismiss()
                    }
                })
        } else {
            materialProgressDialog.dismiss()
            Toast.makeText(this@InventoryLineTransferActivity, "No Network Connection", Toast.LENGTH_SHORT).show()
        }
    }



    private fun handleErrorResponse(response: Response<GetQuantityModel>) {
        materialProgressDialog.dismiss()
        val gson = GsonBuilder().create()
        try {
            val errorBody = response.errorBody()!!.string()
            val errorModel = gson.fromJson(errorBody, OtpErrorModel::class.java)
            errorModel.error.message.value?.let {
                GlobalMethods.showError(this@InventoryLineTransferActivity, it)
                Log.e("json_error------", it)
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }


}