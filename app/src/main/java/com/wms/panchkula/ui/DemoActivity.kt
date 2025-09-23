package com.wms.panchkula.ui

import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.wms.panchkula.Global_Classes.GlobalMethods
import com.wms.panchkula.Global_Classes.MaterialProgressDialog
import com.wms.panchkula.Global_Notification.NetworkConnection
import com.wms.panchkula.Model.GetQuantityModel
import com.wms.panchkula.Model.OtpErrorModel
import com.wms.panchkula.R
import com.wms.panchkula.Retrofit_Api.NetworkClients
import com.wms.panchkula.Retrofit_Api.QuantityNetworkClient
import com.wms.panchkula.SessionManagement.SessionManagement
import com.wms.panchkula.databinding.ActivityGoodsOrderBinding
import com.wms.panchkula.Retrofit_Api.ApiConstantForURL
import com.wms.panchkula.ui.goodsOrder.adapter.GoodsItemLineAdapter
import com.wms.panchkula.ui.home.HomeActivity
import com.wms.panchkula.ui.issueForProductionOrder.Model.InventoryGenExitsModel
import com.wms.panchkula.ui.issueForProductionOrder.Model.ScanedOrderBatchedItems
import com.wms.panchkula.ui.issueForProductionOrder.UI.qrScannerUi.QRScannerActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.webapp.internetconnection.CheckNetwoorkConnection
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.IOException

class DemoActivity : AppCompatActivity(), GoodsItemLineAdapter.OnDeleteItemClickListener {

    lateinit var binding: ActivityGoodsOrderBinding
    lateinit var materialProgressDialog: MaterialProgressDialog
    lateinit var checkNetwoorkConnection: CheckNetwoorkConnection
    private lateinit var sessionManagement: SessionManagement
    private lateinit var networkConnection: NetworkConnection

    var itemLineArrayList: ArrayList<ScanedOrderBatchedItems.Value> = ArrayList<ScanedOrderBatchedItems.Value>()
    var batchQuantityList: ArrayList<String> = ArrayList<String>()
    var serialQuantityList: ArrayList<String> = ArrayList<String>()
    var noneQuantityList: ArrayList<String> = ArrayList<String>()
    var quantityList_gl: ArrayList<String> = ArrayList<String>()
    private var scanedBatchedItemsList_gl: ArrayList<ScanedOrderBatchedItems.Value> = ArrayList()
    private lateinit var recyclerView: RecyclerView
    var batchItemsAdapter: GoodsItemLineAdapter? = null
    private var pos: Int = 0
    private var itemCode = ""
    val REQUEST_CODE = 100



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGoodsOrderBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayShowHomeEnabled(true)

        materialProgressDialog = MaterialProgressDialog(this@DemoActivity)
        checkNetwoorkConnection = CheckNetwoorkConnection(application)
        sessionManagement = SessionManagement(this@DemoActivity)
        networkConnection = NetworkConnection()


        //todo HIDE
        Handler(Looper.getMainLooper()).postDelayed({
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
            if (imm != null && binding.edBatchCodeScan != null) {
                imm.hideSoftInputFromWindow(binding.edBatchCodeScan.windowToken, 0)
            }
        }, 200)

        //todo if leaser type choose..
        if (sessionManagement.getScannerType(this@DemoActivity) == "LEASER") {
            binding.ivScanBatchCode.visibility = View.GONE


            binding.edBatchCodeScan.setOnKeyListener { view1, keyCode, keyEvent ->
                try {
                    var text = binding.edBatchCodeScan.text.toString().trim()
                    var size = 0
                    //TODO validation for stop multiple scanning at one order line...

                    binding.edBatchCodeScan.setText("")
                    binding.edBatchCodeScan.requestFocus()

                    Handler(Looper.getMainLooper()).postDelayed({
                        val imm =
                            getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                        if (imm != null && binding.edBatchCodeScan != null) {
                            imm.hideSoftInputFromWindow(binding.edBatchCodeScan.windowToken, 0)
                        }
                    }, 200)

                } catch (e: Exception) {
                    e.printStackTrace()
                }

                return@setOnKeyListener true
            }

        }

        //todo is qr scanner type choose..
        else if (sessionManagement.getScannerType(this@DemoActivity) == "QR_SCANNER" || sessionManagement.getScannerType(
                this@DemoActivity
            ) == null
        ) { //|| sessionManagement.getLeaserCheck()!! == 0 && sessionManagement.getQRScannerCheck()!! == 1 || sessionManagement.getLeaserCheck()!! == 0 && sessionManagement.getQRScannerCheck()!! == 0
            binding.ivScanBatchCode.visibility = View.VISIBLE

            //TODO click on barcode scanner for popup..
            binding.ivScanBatchCode.setOnClickListener {
                var text = binding.edBatchCodeScan.text.toString().trim()
                recyclerView = binding.rvBatchItems
//                itemCode = this.ItemNo.toString()


                if (sessionManagement.getScannerType(this@DemoActivity) == null) {
                    showPopupNotChooseScanner()
                } else if (sessionManagement.getScannerType(this@DemoActivity) == "QR_SCANNER") {
                    val intent = Intent(this@DemoActivity, QRScannerActivity::class.java)
                    (this@DemoActivity as DemoActivity).startActivityForResult(intent, REQUEST_CODE)
                }

            }


        }


        binding.chipSave.setOnClickListener {
            binding.chipSave.isEnabled = false
            binding.chipSave.isCheckable = false
            callGoodsPostingApi()
        }


    }


    lateinit var batchList: List<ScanedOrderBatchedItems.Value>
    val handler = Handler(Looper.getMainLooper())

    private fun callGoodsPostingApi() {

        if (networkConnection.getConnectivityStatusBoolean(applicationContext)) {
            materialProgressDialog.show()

            var postedJson: JsonObject = JsonObject()
            postedJson.addProperty("Comments", "")
            postedJson.addProperty("DocDate", GlobalMethods.getCurrentDateFormatted()) //todo current date will send here---

            val DocumentLinesArray = JsonArray()


            var batchIndex = 0
            var serialIndex = 0

            for (i in itemLineArrayList.indices) {
                val item = itemLineArrayList[i]

                if (item.Batch != null) {
                    // Sum batch quantities for the current item
                    val quantity = GlobalMethods.sumBatchQuantity(batchIndex, batchQuantityList)

                    val jsonObject = JsonObject().apply {
                        addProperty("ItemCode", item.ItemCode)
                        addProperty("UnitPrice", "")
                        addProperty("Quantity", quantity)
                        addProperty("WarehouseCode", "01")
                    }

                    val batchNumbersArray = JsonArray()
                    while (batchIndex < batchQuantityList.size) {
                        val jsonLinesObject = JsonObject().apply {
                            addProperty("BatchNumber", item.Batch)
                            addProperty("SystemSerialNumber", item.SystemNumber)
                            addProperty("Quantity", batchQuantityList[batchIndex])
                        }
                        batchNumbersArray.add(jsonLinesObject)
                        batchIndex++

                        break // Move to the next batch item
                    }

                    jsonObject.add("BatchNumbers", batchNumbersArray)
                    DocumentLinesArray.add(jsonObject)

                    // Break the outer loop if batchQuantityList is exhausted
                    if (batchIndex >= batchQuantityList.size) {
                        break
                    }

                }

                else if (item.SerialNumber != null) {
                    // Sum serial quantities for the current item
                    val quantity = GlobalMethods.sumBatchQuantity(serialIndex, serialQuantityList)

                    val jsonObject = JsonObject().apply {
                        addProperty("ItemCode", item.ItemCode)
                        addProperty("UnitPrice", "")
                        addProperty("Quantity", quantity)
                        addProperty("WarehouseCode", "01")
                    }

                    val serialNumbersArray = JsonArray()
                    while (serialIndex < serialQuantityList.size) {
                        val jsonLinesObject = JsonObject().apply {
                            addProperty("SystemSerialNumber", item.SystemNumber)
                            addProperty("InternalSerialNumber", item.SerialNumber)
                            addProperty("Quantity", "1")
                        }
                        serialNumbersArray.add(jsonLinesObject)
                        serialIndex++

                        break // Move to the next serial item
                    }

                    jsonObject.add("SerialNumbers", serialNumbersArray)
                    DocumentLinesArray.add(jsonObject)

                    // Break the outer loop if serialQuantityList is exhausted
                    if (serialIndex >= serialQuantityList.size) {
                        break
                    }
                }

                else {
                    // Sum serial quantities for the current item
                    var quantity = 0.0
//                     quantity = GlobalMethods.sumBatchQuantity(serialIndex, noneQuantityList)

                    for (j in 0 until noneQuantityList.size) {
                        quantity = GlobalMethods.sumBatchQuantity(j, noneQuantityList)
                    }

                    val jsonObject = JsonObject().apply {
                        addProperty("ItemCode", item.ItemCode)
                        addProperty("UnitPrice", "")
                        addProperty("Quantity", quantity)
                        addProperty("WarehouseCode", "01")
                    }

                    DocumentLinesArray.add(jsonObject)

                }
            }


            /*        for (i in itemLineArrayList.indices) {
        
                        if (itemLineArrayList[i].Batch != null) {
        
                            //TODO sum of order line batch quantities and compare with line quantity..
                            var quantity = 0.000
                            quantity = GlobalMethods.sumBatchQuantity(i, batchQuantityList)
        
                            val jsonObject = JsonObject()
                            jsonObject.addProperty("ItemCode", itemLineArrayList[i].ItemCode)
                            jsonObject.addProperty("UnitPrice", "")
                            jsonObject.addProperty("Quantity", quantity)
                            jsonObject.addProperty("WarehouseCode", "01")// tempList[0].Warehouse sessionManagement.getWarehouseCode(this)
        
                            var BatchNumbersArray = JsonArray()
        
        
                            for (i in itemLineArrayList.indices) {
                                for (j in i until batchQuantityList.size) {
                                    var jsonLinesObject = JsonObject()
                                    jsonLinesObject.addProperty("BatchNumber", itemLineArrayList[i].Batch)
                                    jsonLinesObject.addProperty("SystemSerialNumber", itemLineArrayList[i].SystemNumber)
                                    jsonLinesObject.addProperty("Quantity", batchQuantityList[j])
        
                                    BatchNumbersArray.add(jsonLinesObject)
        
                                    break
                                }
                            }
        
        
                            jsonObject.add("BatchNumbers", BatchNumbersArray)
                            if (batchList.size > 0)
                                DocumentLinesArray.add(jsonObject)
        
                        } else if (itemLineArrayList[i].SerialNumber != null) {
                            //TODO sum of order line batch quantities and compare with line quantity..
                            var quantity = 0.000
                            quantity = GlobalMethods.sumBatchQuantity(i, serialQuantityList)
        
                            val jsonObject = JsonObject()
                            jsonObject.addProperty("ItemCode", itemLineArrayList[i].ItemCode)
                            jsonObject.addProperty("UnitPrice", "")
                            jsonObject.addProperty("Quantity", quantity)
                            jsonObject.addProperty("WarehouseCode", "01")// tempList[0].Warehouse sessionManagement.getWarehouseCode(this)
        
                            var SerialNumbersArray = JsonArray()
        
                            for (i in itemLineArrayList.indices) {
                                for (j in i until serialQuantityList.size) {
                                    var jsonLinesObject = JsonObject()
                                    jsonLinesObject.addProperty("SystemSerialNumber", itemLineArrayList[i].SystemNumber)
                                    jsonLinesObject.addProperty("InternalSerialNumber", itemLineArrayList[i].SerialNumber)
                                    jsonLinesObject.addProperty("Quantity", "1")
        
                                    SerialNumbersArray.add(jsonLinesObject)
        
                                    break
                                }
                            }
        
        
                            jsonObject.add("SerialNumbers", SerialNumbersArray)
                            if (batchList.size > 0)
                                DocumentLinesArray.add(jsonObject)
                        }
        
                    }*/



            postedJson.add("DocumentLines", DocumentLinesArray)

            Log.e("success--PayLoad==>", "==>" + postedJson.toString())

            val networkClient = NetworkClients.create(this@DemoActivity)
            networkClient.doGetInventoryGenExits(postedJson).apply {
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
                                    GlobalMethods.showSuccess(
                                        this@DemoActivity,
                                        "Goods Order Post Successfully. "+response.body()!!.DocNum.toString()
                                    )
                                }
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
                                            this@DemoActivity,
                                            mError.error.message.value
                                        )
                                    }
                                    if (mError.error.message.value != null) {
                                        GlobalMethods.showError(
                                            this@DemoActivity,
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
            materialProgressDialog.dismiss()
            handler.post {
                Toast.makeText(this@DemoActivity, "No Network Connection", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

    var type = ""
    var itemDesc = ""

    //todo onActivity function override for qr code scanning in adapter..
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK) {

            Log.e("Result==>", data?.getStringExtra("batch_code").toString())
            if (requestCode == REQUEST_CODE && resultCode == Activity.RESULT_OK) {
                val result = data?.getStringExtra("batch_code")
                var size = 0
//                val list = hashMap.get("Item" + pos) as List<*>
                val list = itemLineArrayList as List<*>
                Log.e("size===>", list.size.toString())
                Log.e("ItemCode===>", itemCode)

                //todo spilt string and get string at 0 index...

                // Split the string by "~"
                val parts = result.toString().split("~")

                val lastPart = parts.last()
                var itemCode = parts[2]

                type = lastPart

                //todo set validation for duplicate item
                if (type == "Batch") {
                    if (checkDuplicate(itemLineArrayList, result.toString().split(",")[0])) {//checkDuplicate(itemLineArrayList, result.toString().split(",")[0])
                        //todo scan call api here...
                        scanBatchLinesItem(result.toString().split(",")[0], recyclerView, pos, itemCode, binding.tvTotalScannQty)

                    }
                } else if (type.equals("Serial")) {
                    if (checkDuplicateForSerial(itemLineArrayList, result.toString().split(",")[0])) {
                        //todo scan call api here...
                        scanSerialLineItem(result.toString().split(",")[0], recyclerView, pos, itemCode, binding.tvTotalScannQty)
                    }
                }
                else if (type.equals("NONE")){
                    val parts = result.toString().split("~")

                    val lastPart = parts.last()
                    var itemCode = parts[2]
                    itemDesc = parts[3]

                    type = lastPart

                    callNoneBindFunction(itemCode, recyclerView, pos, binding.tvTotalScannQty, itemDesc)
                }
            }

        }
    }


    var noneArrayList: ArrayList<ScanedOrderBatchedItems.Value> = ArrayList<ScanedOrderBatchedItems.Value>()


    private fun callNoneBindFunction(itemCode: String, rvBatchItems: RecyclerView, position: Int, tvTotalScannQty: TextView, itemDesc: String) {

        if (itemCode.isNotEmpty()) {

            var itemList_gl: ArrayList<ScanedOrderBatchedItems.Value> = ArrayList()
            itemList_gl.clear()
            var data = ScanedOrderBatchedItems.Value("", itemCode, itemDesc, "", "", "","", "", "", "", "", "", "", 0, "", 0.0, 0.0, 0.0, 0.0, 0.0, "", 0.0, "", 0.0, 0.0, "")

            itemList_gl.add(data)

            for (i in itemList_gl.indices) {
                itemLineArrayList.add(itemList_gl[i])
            }


            var stringList: ArrayList<String> = ArrayList()
            stringList.addAll(noneQuantityList)


            if (!itemList_gl.isNullOrEmpty()) {

                Log.e("list_size-----", itemList_gl.size.toString())

                stringList.add("10")

                noneQuantityList.clear()

                for (i in stringList.indices) {
                    noneQuantityList.add(stringList[i])
                    quantityList_gl.add(stringList[i])

                }

                tempList.clear()

                for (i in quantityList_gl.indices) {
                    tempList.add(quantityList_gl[i])
                }


                //TODO sum of quantity of batches..
                tvTotalScannQty.text = "${GlobalMethods.sumBatchQuantity(position, noneQuantityList)}"


                val layoutManager: RecyclerView.LayoutManager = LinearLayoutManager(this@DemoActivity)
                rvBatchItems.layoutManager = layoutManager


                batchItemsAdapter = GoodsItemLineAdapter(this@DemoActivity, itemLineArrayList, quantityList_gl, "NoneQR", batchQuantityList, serialQuantityList) { newQuantity, pos ->
                    Log.e("Quantity===> ", "onResponse: $newQuantity")
                    var QUANTITYVAL = newQuantity

                    if (QUANTITYVAL.isEmpty()) {
                        QUANTITYVAL = "0"
                    }

                    //todo this code is use for empty qty box and set 0 default if empty, preventing from crashing-

                    if (!QUANTITYVAL.isNullOrEmpty()) {
                        if (tempList.size > pos) {
                            tempList[pos] = QUANTITYVAL

                        } else {
                            while (tempList.size <= pos) {
                                tempList.add("0")
                            }
                            tempList[pos] = QUANTITYVAL
                        }
                    }

              /*      //todo updates list set in quantity list
                    batchQuantityList.clear()
                    batchQuantityList.addAll(localBatchList)

*/
                    noneQuantityList.clear()
                    noneQuantityList.addAll(tempList)


                    Log.e("Calculate==", "Calculate_line==>: " + GlobalMethods.sumBatchQuantity(pos, noneQuantityList))
                    tvTotalScannQty.text = "${GlobalMethods.sumBatchQuantity(pos, noneQuantityList)}"

                }


                //todo call setOnItemListener Interface Function...
                batchItemsAdapter?.setOnDeleteItemClickListener(this@DemoActivity)
                rvBatchItems.adapter = batchItemsAdapter

            }


        }

    }

    private fun showPopupNotChooseScanner() {
        val builder =
            AlertDialog.Builder(this@DemoActivity, R.style.CustomAlertDialog).create()
        val view =
            LayoutInflater.from(this@DemoActivity).inflate(R.layout.custom_popup_alert, null)
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
            var intent = Intent(this@DemoActivity, HomeActivity::class.java)
            startActivity(intent)
            builder.dismiss()

        }

        builder.setCancelable(true)
        builder.show()
    }


    //TODO duplicatcy checking from list...
    fun checkDuplicate(scanedBatchedItemsList_gl: ArrayList<ScanedOrderBatchedItems.Value>, batchCode: String): Boolean {
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


    fun checkDuplicateForSerial(scanedBatchedItemsList_gl: ArrayList<ScanedOrderBatchedItems.Value>, batchCode: String): Boolean {
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


    override fun onDeleteItemClick(list: ArrayList<ScanedOrderBatchedItems.Value>, quantityHashMap1: ArrayList<String>, pos: Int, batchTypeList: ArrayList<String>, serialTypeList: ArrayList<String>) {
        var batch = ""

        if (!list[pos].Batch.isNullOrEmpty()) {
            batch = list[pos].Batch
        } else
            batch = list[pos].SerialNumber
//todo use when call bind fun.

        MaterialAlertDialogBuilder(this)
            .setTitle("Confirm...")
            .setMessage("Do you want to delete " + batch + " Item .")
            .setIcon(R.drawable.ic_trash)
            .setPositiveButton("Confirm",
                DialogInterface.OnClickListener { dialogInterface: DialogInterface?, i1: Int ->

                    Log.e("itemLineArrayList===>", itemLineArrayList.size.toString())
                    Log.e("list===>", list.size.toString())
                    Log.e("before_batch===>", quantityHashMap1.size.toString())
                    Log.e("batchTypeList===>", batchTypeList.size.toString())
                    Log.e("serialTypeList===>", serialTypeList.size.toString())


                    //todo remove particular line from lists-
                    list.removeAt(pos)

                    quantityHashMap1.removeAt(pos)

                    batchTypeList.removeAt(pos)

                    serialTypeList.removeAt(pos)


                    batchItemsAdapter?.notifyDataSetChanged()


                    var data = GlobalMethods.sumBatchQuantity(pos, quantityHashMap1!!).toString()
                    Log.e("data_value===>", data.toString())
                    binding.tvTotalScannQty.setText(data)

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


    //TODO scan item lines api here....
    private fun scanBatchLinesItem(text: String, rvBatchItems: RecyclerView, position: Int, itemCode: String?, tvTotalScannQty: TextView) {
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


//                                        itemLineArrayList.add(position, itemList_gl[0])


                                        if (!itemList_gl.isNullOrEmpty()) {

                                            Log.e("list_size-----", itemList_gl.size.toString())

                                            //todo quantity..

                                            getQuantityFromApi(text, itemList_gl[0].ItemCode, tvTotalScannQty, rvBatchItems, itemList_gl)

                                        }
                                    } else {
                                        GlobalMethods.showError(
                                            this@DemoActivity,
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
                                                this@DemoActivity,
                                                mError.error.message.value
                                            )
                                        }
                                        if (mError.error.message.value != null) {
                                            GlobalMethods.showError(
                                                this@DemoActivity,
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
            Toast.makeText(this@DemoActivity, "No Network Connection", Toast.LENGTH_SHORT)
                .show()
        }
    }


    //TODO scan item lines api here....

    var tempList: ArrayList<String> = ArrayList()


    private fun getQuantityFromApi(batchCode: String, itemCode: String, tvTotalScannQty: TextView, rvBatchItems: RecyclerView, itemList_gl: ArrayList<ScanedOrderBatchedItems.Value>) {
        if (networkConnection.getConnectivityStatusBoolean(this@DemoActivity)) {
            materialProgressDialog.show()
            var apiConfig = ApiConstantForURL()

            NetworkClients.updateBaseUrlFromConfig(apiConfig)

            QuantityNetworkClient.updateBaseUrlFromConfig(apiConfig)

            val networkClient = QuantityNetworkClient.create(this@DemoActivity)
            networkClient.getQuantityValue(sessionManagement.getCompanyDB(applicationContext)!!,batchCode, itemCode, "RM-309") //sessionManagement.getWarehouseCode(this@DemoActivity)!!
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


                                    for (i in stringList.indices) {
                                        batchQuantityList.add(stringList[i])
                                        quantityList_gl.add(stringList[i])
                                    }


                                 /*   quantityList_gl.clear()
                                    val combinedList = ArrayList<String>()
                                    combinedList.clear()
                                    combinedList.addAll(serialQuantityList)
                                    combinedList.addAll(batchQuantityList)
                                    quantityList_gl.addAll(combinedList)*/

                                    for (i in batchQuantityList.indices) {
                                        tempList.add(batchQuantityList[i])
                                    }


                                    tvTotalScannQty.text = "${GlobalMethods.sumBatchQuantity(0, quantityList_gl)}"

                                    if (stringList.isNotEmpty() && !stringList.contains("0")) {
                                        rvBatchItems.layoutManager = LinearLayoutManager(this@DemoActivity)

                                        batchItemsAdapter = GoodsItemLineAdapter(this@DemoActivity, itemLineArrayList, quantityList_gl, "IssueOrder", batchQuantityList, serialQuantityList) { newQuantity, pos ->
                                            Log.e("Quantity===> ", "onResponse: $newQuantity")
                                            var QUANTITYVAL = newQuantity

                                            if (QUANTITYVAL.isEmpty()) {
                                                QUANTITYVAL = "0"
                                            }

                                            //todo this code is use for empty qty box and set 0 default if empty, preventing from crashing-
                                            if (!QUANTITYVAL.isNullOrEmpty()) {
                                                if (tempList.size > pos) {
                                                    tempList[pos] = QUANTITYVAL
                                                } else {
                                                    while (tempList.size <= pos) {
                                                        tempList.add("0")
                                                    }
                                                    tempList[pos] = QUANTITYVAL
                                                }
                                            }

                                            //todo updates list set in quantity list
                                            batchQuantityList.clear()
                                            batchQuantityList.addAll(tempList)


                                            quantityList_gl.clear()
                                            quantityList_gl.addAll(tempList)


                                           /* quantityList_gl.clear()
                                            val combinedList = ArrayList<String>()
                                            combinedList.clear()
                                            combinedList.addAll(serialQuantityList)
                                            combinedList.addAll(batchQuantityList)
                                            quantityList_gl.addAll(combinedList)*/


                                            Log.e("Calculate==", "Calculate_line==>: " + GlobalMethods.sumBatchQuantity(pos, batchQuantityList))
                                            tvTotalScannQty.text = "${GlobalMethods.sumBatchQuantity(pos, batchQuantityList)}"

                                        }


                                        batchItemsAdapter?.setOnDeleteItemClickListener(this@DemoActivity)
                                        rvBatchItems.adapter = batchItemsAdapter

                                        batchItemsAdapter?.notifyDataSetChanged()
                                    } else {
                                        batchItemsAdapter?.notifyDataSetChanged()
                                        GlobalMethods.showError(this@DemoActivity, "Batch / Roll No. has zero Quantity of this PO.")
                                    }
                                } else {
                                    GlobalMethods.showError(this@DemoActivity, "No Quantity Found of this Production Order.")
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
            Toast.makeText(this@DemoActivity, "No Network Connection", Toast.LENGTH_SHORT)
                .show()
        }
    }


    //TODO scan item lines api here....
    private fun scanSerialLineItem(text: String, rvBatchItems: RecyclerView, position: Int, itemCode: String?, tvTotalScannQty: TextView) {
        if (networkConnection.getConnectivityStatusBoolean(this@DemoActivity)) {
            materialProgressDialog.show()
            var apiConfig = ApiConstantForURL()

            NetworkClients.updateBaseUrlFromConfig(apiConfig)

            QuantityNetworkClient.updateBaseUrlFromConfig(apiConfig)

            val networkClient = NetworkClients.create(this@DemoActivity)
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


                                        for (i in itemList_gl.indices) {
                                            itemLineArrayList.add(itemList_gl[i])
                                        }


                                        var stringList: ArrayList<String> = ArrayList()
                                        stringList.addAll(serialQuantityList)


                                        if (!itemList_gl.isNullOrEmpty()) {

                                            Log.e("list_size-----", itemList_gl.size.toString())

                                            //todo quantity..
                                            stringList.add("1")

                                            serialQuantityList.clear()

                                            for (i in stringList.indices) {
                                                serialQuantityList.add(stringList[i])
                                                quantityList_gl.add(stringList[i])

                                            }

                                            tempList.clear()

                                            for (i in quantityList_gl.indices) {
                                                tempList.add(quantityList_gl[i])
                                            }


                                            var localBatchList: ArrayList<String> = ArrayList()
                                            for (i in batchQuantityList.indices) {
                                                localBatchList.add(batchQuantityList[i])
                                            }

                                            //TODO sum of quantity of batches..
                                            tvTotalScannQty.text = "${GlobalMethods.sumBatchQuantity(position, quantityList_gl)}"


                                            val layoutManager: RecyclerView.LayoutManager = LinearLayoutManager(this@DemoActivity)
                                            rvBatchItems.layoutManager = layoutManager

//                                            batchItemsAdapter = GoodsItemLineAdapter(this@DemoActivity, itemLineArrayList, quantityList_gl, "SerialQR")

                                            batchItemsAdapter = GoodsItemLineAdapter(this@DemoActivity, itemLineArrayList, quantityList_gl, "SerialQR", batchQuantityList, serialQuantityList) { newQuantity, pos ->
                                                Log.e("Quantity===> ", "onResponse: $newQuantity")
                                                var QUANTITYVAL = newQuantity

                                                if (QUANTITYVAL.isEmpty()) {
                                                    QUANTITYVAL = "0"
                                                }

                                                //todo this code is use for empty qty box and set 0 default if empty, preventing from crashing-

                                                if (!QUANTITYVAL.isNullOrEmpty()) {
                                                    if (tempList.size > pos) {
                                                        tempList[pos] = QUANTITYVAL
                                                        localBatchList[pos] = QUANTITYVAL

                                                    } else {
                                                        while (tempList.size <= pos) {
                                                            tempList.add("0")
                                                            localBatchList.add("0")
                                                        }
                                                        tempList[pos] = QUANTITYVAL
                                                        localBatchList[pos] = QUANTITYVAL
                                                    }
                                                }

                                                //todo updates list set in quantity list
                                                batchQuantityList.clear()
                                                batchQuantityList.addAll(localBatchList)


                                              /*  quantityList_gl.clear()
                                                quantityList_gl.addAll(tempList)*/

                                                serialQuantityList.clear()
                                                serialQuantityList.addAll(tempList)


                                               /* quantityList_gl.clear()
                                                val combinedList = ArrayList<String>()
                                                combinedList.clear()
                                                combinedList.addAll(serialQuantityList)
                                                combinedList.addAll(batchQuantityList)
                                                quantityList_gl.addAll(combinedList)*/


                                                Log.e("Calculate==", "Calculate_line==>: " + GlobalMethods.sumBatchQuantity(pos, serialQuantityList))
                                                tvTotalScannQty.text = "${GlobalMethods.sumBatchQuantity(pos, serialQuantityList)}"

                                            }


                                            //todo call setOnItemListener Interface Function...
                                            batchItemsAdapter?.setOnDeleteItemClickListener(this@DemoActivity)
                                            rvBatchItems.adapter = batchItemsAdapter

                                        }
                                    } else {
                                        GlobalMethods.showError(this@DemoActivity, "Invalid Batch Code")
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
                                                this@DemoActivity,
                                                mError.error.message.value
                                            )
                                        }
                                        if (mError.error.message.value != null) {
                                            GlobalMethods.showError(
                                                this@DemoActivity,
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
            Toast.makeText(this@DemoActivity, "No Network Connection", Toast.LENGTH_SHORT)
                .show()
        }
    }


    private fun handleErrorResponse(response: Response<GetQuantityModel>) {
        materialProgressDialog.dismiss()
        val gson = GsonBuilder().create()
        try {
            val errorBody = response.errorBody()!!.string()
            val errorModel = gson.fromJson(errorBody, OtpErrorModel::class.java)
            errorModel.error.message.value?.let {
                GlobalMethods.showError(this@DemoActivity, it)
                Log.e("json_error------", it)
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }


}