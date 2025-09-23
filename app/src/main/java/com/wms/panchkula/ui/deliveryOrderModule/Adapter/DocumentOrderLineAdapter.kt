package com.wms.panchkula.ui.deliveryOrderModule.Adapter

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.StrictMode
import android.util.Log
import android.view.*
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.AppCompatButton
import androidx.core.app.ActivityCompat
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
import com.wms.panchkula.ui.deliveryOrderModule.Model.DeliveryModel
import com.wms.panchkula.ui.deliveryOrderModule.UI.DeliveryDocumentLineActivity
import com.wms.panchkula.ui.home.HomeActivity
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
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException

class DocumentOrderLineAdapter(
    val context: Context, val documentLineList_gl: ArrayList<DeliveryModel.DocumentLine>, private val chipSave: Chip,
    private val callback: AdapterCallback) : RecyclerView.Adapter<DocumentOrderLineAdapter.ViewHolder>(), BatchItemsAdapter.OnDeleteItemClickListener {

    //todo 19-05-23
    private var connection: Connection? = null
    val REQUEST_CODE = 100
    private lateinit var sessionManagement: SessionManagement
    var hashMap: HashMap<String, ArrayList<ScanedOrderBatchedItems.Value>> = HashMap<String, ArrayList<ScanedOrderBatchedItems.Value>>()
    var quantityHashMap: HashMap<String, ArrayList<String>> = HashMap<String, ArrayList<String>>()
    private var scanedBatchedItemsList_gl: ArrayList<ScanedOrderBatchedItems.Value> = ArrayList()
    private lateinit var recyclerView: RecyclerView
    private var pos: Int = 0
    private var itemCode = ""
    var batchItemsAdapter: BatchItemsAdapter? = null
    lateinit var networkConnection: NetworkConnection
    lateinit var materialProgressDialog: MaterialProgressDialog
    lateinit var tvOpenQty : TextView
    var remainingOpenQuantity : Double = 0.0
    var width = 0.0
    var U_gsmso = 0.0
    lateinit var tvTotalScanQty: TextView
    lateinit var tvTotalScanGW: TextView

    init {
        setSqlServer()
        sessionManagement = SessionManagement(context)
        networkConnection = NetworkConnection()
        materialProgressDialog = MaterialProgressDialog(context)
    }

    //todo interfaces...
    interface AdapterCallback {
        fun onApiResponse(response: HashMap<String, ArrayList<ScanedOrderBatchedItems.Value>>, quantityResponse: HashMap<String, ArrayList<String>>, )
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ProductionOrderLinesAdapterLayoutBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return DocumentOrderLineAdapter.ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        with(holder) {
            with(documentLineList_gl[position]) {
                binding.tvItemName.text = this.ItemDescription
                binding.tvOpenQtyLabel.text = "Quantity"
                binding.tvOpenQty.text = this.RemainingOpenQuantity.toString()
                binding.tvWidth.text = this.Factor1.toString()
                binding.tvLength.text = this.Factor2.toString()
//                binding.tvGsm.text = ":   "+ this.U_GSMSO.toString()

                binding.trTotalScanFields.visibility = View.VISIBLE

                sessionManagement.setWarehouseCode(context, this.WarehouseCode)

                //todo add adapter size in hashmap at once.
                var count = 0
                for (i in 0 until documentLineList_gl.size){
                    //TODO set count adapter position size store in list for batch scan...
                    var itemList_gl: ArrayList<ScanedOrderBatchedItems.Value> = ArrayList()
                    if (hashMap.size != documentLineList_gl.size) {
                        hashMap.put("Item" + count, itemList_gl)
                    }

                    //TODO set count adapter position size store in list for batch quantity...
                    var stringList: ArrayList<String> = ArrayList()
                    if (quantityHashMap.size != documentLineList_gl.size) {
                        quantityHashMap.put("Item" + count, stringList)
                    }
                    count ++
                }
                Log.e("count ===> ", count.toString())


                //TODO for manual batch entry..
                binding.edBatchCodeScan.setOnEditorActionListener { v, actionId, event ->
                    if (actionId == KeyEvent.ACTION_DOWN && actionId == KeyEvent.KEYCODE_ENTER || actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_GO || actionId == EditorInfo.IME_ACTION_SEND) {
                        var text = binding.edBatchCodeScan.text.toString().trim()
                        if (checkDuplicate(hashMap.get("Item" + position)!!, text)) {
                            recyclerView = binding.rvBatchItems
                            //TODO batch scan api..
                            scanOrderLinesItem(text, binding.rvBatchItems, adapterPosition, this.ItemCode, binding.tvOpenQty, this.RemainingOpenQuantity,
                                this.Factor1, this.U_GSMSO, binding.tvTotalScannQty, binding.tvTotalScanGw)
                        }
                        binding.edBatchCodeScan.setText("")

                        true

                    } else {
                        false
                    }
                }

                Log.d("scanner_type===>", sessionManagement.getScannerType(context).toString())

                //todo if leaser type choose..
                if (sessionManagement.getScannerType(context) == "LEASER") { //sessionManagement.getLeaserCheck()!! == 1 && sessionManagement.getQRScannerCheck()!! == 0
                    binding.ivScanBatchCode.visibility = View.GONE

                    binding.edBatchCodeScan.setOnTouchListener(android.view.View.OnTouchListener { view1: View, motionEvent: MotionEvent? ->
                        binding.edBatchCodeScan.setText("")
                        binding.edBatchCodeScan.setCursorVisible(true)
                        binding.edBatchCodeScan.setFocusableInTouchMode(true)
                        binding.edBatchCodeScan.requestFocus()
                        val imm = context.getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as InputMethodManager
                        imm.hideSoftInputFromWindow(view1.windowToken, 0)
                        true
                    })

                /*    binding.edBatchCodeScan.setOnFocusChangeListener { view1, hasFocus ->
                         if (hasFocus) {
                             val imm1 = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                             if (imm1 != null) {
                                 imm1.hideSoftInputFromWindow(view1.windowToken, 0)
                                 binding.edBatchCodeScan.requestFocus()
                                 binding.edBatchCodeScan.setBackgroundResource(R.drawable.gradient_list_div_color)
                             }
                             true
                         } else {
                             return@setOnFocusChangeListener
                         }
                     }*/

                    binding.edBatchCodeScan.setOnKeyListener { view1, keyCode, keyEvent ->
                        if (binding.edBatchCodeScan.isFocused() && binding.edBatchCodeScan.isCursorVisible() && binding.edBatchCodeScan.hasFocus()) {
                            var inputMethodManager = context.getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as InputMethodManager
                            inputMethodManager.hideSoftInputFromWindow(binding.edBatchCodeScan.getWindowToken(), 0)
                        }
                        if (keyEvent.action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {

                            var text = binding.edBatchCodeScan.text.toString().trim()
                                var x = text
                                if(x.contains(",")) { //todo validation for scan QR code from laser device and get batch code.
                                    x = x.split(",")[0]
                                    if (checkDuplicate(hashMap.get("Item" + position)!!, x)) {
                                        //todo scan call api here...
                                        scanOrderLinesItem(x, binding.rvBatchItems, adapterPosition, this.ItemCode, binding.tvOpenQty, this.RemainingOpenQuantity,
                                            this.Factor1, this.U_GSMSO, binding.tvTotalScannQty, binding.tvTotalScanGw)
                                    }
                                }else{
                                    if (checkDuplicate(hashMap.get("Item" + position)!!, text)) {
                                        //todo scan call api here...
                                        scanOrderLinesItem(text, binding.rvBatchItems, adapterPosition, this.ItemCode, binding.tvOpenQty, this.RemainingOpenQuantity,
                                            this.Factor1, this.U_GSMSO, binding.tvTotalScannQty, binding.tvTotalScanGw)
                                    }
                                }
                            binding.edBatchCodeScan.setText("")
                            binding.edBatchCodeScan.requestFocus()
                        }
                         return@setOnKeyListener true
                    }

                }

                //todo is qr scanner type choose..
                else if (sessionManagement.getScannerType(context) == "QR_SCANNER" || sessionManagement.getScannerType(context) == null){ //|| sessionManagement.getLeaserCheck()!! == 0 && sessionManagement.getQRScannerCheck()!! == 1 || sessionManagement.getLeaserCheck()!! == 0 && sessionManagement.getQRScannerCheck()!! == 0
                    binding.ivScanBatchCode.visibility = View.VISIBLE

                    //TODO click on barcode scanner for popup..
                    binding.ivScanBatchCode.setOnClickListener {
                        var text = binding.edBatchCodeScan.text.toString().trim()
                        recyclerView = binding.rvBatchItems
                        itemCode = this.ItemCode
                        tvOpenQty = binding.tvOpenQty
                        tvTotalScanQty = binding.tvTotalScannQty
                        tvTotalScanGW = binding.tvTotalScanGw
                        width = this.Factor1
                        U_gsmso = this.U_GSMSO
                        remainingOpenQuantity = this.RemainingOpenQuantity
                        if (sessionManagement.getScannerType(context) == null) {
                            showPopupNotChooseScanner()
                        }
                        else if (sessionManagement.getScannerType(context) == "QR_SCANNER") {
                            val intent = Intent(context, QRScannerActivity::class.java)
                            pos = adapterPosition
                            (context as DeliveryDocumentLineActivity).startActivityForResult(intent, REQUEST_CODE)
                        }/* else if (sessionManagement.getLeaserCheck()!! == 1 && sessionManagement.getQRScannerCheck()!! == 0) {
                        //TODO laser popup
                         laserScannerPopupDialog(binding.rvBatchItems, adapterPosition, this.ItemCode, binding.tvOpenQty)
                        } */
                    }

                    //todo for manual batch entry..
                    binding.edBatchCodeScan.setOnEditorActionListener { v, actionId, event ->
                        //event.action == KeyEvent.ACTION_DOWN && actionId == KeyEvent.KEYCODE_ENTER ||
                        if (actionId == KeyEvent.ACTION_DOWN && actionId == KeyEvent.KEYCODE_ENTER || actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_GO || actionId == EditorInfo.IME_ACTION_SEND) {
                            var text = binding.edBatchCodeScan.text.toString().trim()
                            if (checkDuplicate(hashMap.get("Item" + position)!!, text)) {
                                //todo scan call api here...
                                scanOrderLinesItem(text, binding.rvBatchItems, adapterPosition, this.ItemCode, binding.tvOpenQty, RemainingOpenQuantity, this.Factor1, this.U_GSMSO, binding.tvTotalScannQty, binding.tvTotalScanGw)
                            }
                            binding.edBatchCodeScan.setText("")
                            true

                        } else {
                            false
                        }
                    }
                }



                //TODO save order lines listener by interface...
                chipSave.setOnClickListener {
                    callback.onApiResponse(hashMap, quantityHashMap)
                }


            }
        }
    }

    override fun getItemCount(): Int {
        return documentLineList_gl.size
    }

    //todo viewholder...
    class ViewHolder(val binding: ProductionOrderLinesAdapterLayoutBinding) : RecyclerView.ViewHolder(binding.root)

    //TODO scan item lines api here....
    private fun scanOrderLinesItem(text: String, rvBatchItems: RecyclerView, position: Int, itemCode: String?, tvOpenQty: TextView,
        remainingOpenQuantity: Double, factor1: Double, uGsm: Double, tvTotalScannQty: TextView, tvTotalScanGw: TextView) {
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
                                    Log.e("response---------", response.message())

                                    var responseModel = response.body()!!
                                    if (responseModel.value.size > 0 && !responseModel.value.isNullOrEmpty()) {

                                        //todo validation for line width and batch width----
                                        if (factor1 == responseModel.value[0].U_Width || factor1 > responseModel.value[0].U_Width ){
                                            if (uGsm == responseModel.value[0].U_GSM){
                                                var modelResponse = responseModel.value
                                                scanedBatchedItemsList_gl.addAll(modelResponse)

                                                var itemList_gl: ArrayList<ScanedOrderBatchedItems.Value> = ArrayList()
                                                itemList_gl.addAll(hashMap.get("Item" + position)!!)

                                                var stringList: ArrayList<String> = ArrayList()
                                                stringList.addAll(quantityHashMap.get("Item" + position)!!)

                                                itemList_gl.add(responseModel.value[0])

                                                if (!itemList_gl.isNullOrEmpty()) {

                                                    Log.e("list_size-----", itemList_gl.size.toString())

                                                    //todo quantity..
                                                    getQuantityFromApi(text, itemList_gl[0].ItemCode, position, stringList, tvOpenQty, tvTotalScannQty,tvTotalScanGw, rvBatchItems, itemList_gl )




                                                }

                                            }else{
                                                GlobalMethods.showError(context, "GSM must be same of SO.")
                                            }

                                        }
                                        else{
                                            GlobalMethods.showError(context, "Invalid QR Code / Width not matched.")
                                        }
                                    }
                                    else {
                                        GlobalMethods.showError(context, "Invalid Batch Code, Not Found Data")
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
                                        stringList.add(responseModel.value[0].Quantity)
                                        quantityHashMap.put("Item" + position, stringList)
                                        //TODO sum of quantity of batches..
                                        tvTotalScannQty.text = "  : "+GlobalMethods.sumBatchQuantity(position, quantityHashMap.get("Item" + position)!!).toString()


                                        if (quantityHashMap.get("Item" + position)!!.size > 0 ){
                                            if (!quantityHashMap.get("Item" + position)!!.contains("0") ){
                                                hashMap.put("Item" + position, itemList_gl)

                                                val layoutManager: RecyclerView.LayoutManager = LinearLayoutManager(context)
                                                rvBatchItems.layoutManager = layoutManager
//                                                batchItemsAdapter = BatchItemsAdapter(context, hashMap.get("Item" + position)!!, quantityHashMap.get("Item" + position)!!, "SalesOrder")
                                                //todo call setOnItemListener Interface Function...
                                                batchItemsAdapter?.setOnDeleteItemClickListener(this@DocumentOrderLineAdapter)
                                                rvBatchItems.adapter = batchItemsAdapter

                                                var totalGrossWeight = GlobalMethods.changeDecimal(GlobalMethods.sumBatchGrossWeight(position, hashMap.get("Item" + position)!!).toString())
                                                tvTotalScanGw.text = ":   "+ totalGrossWeight


                                            }else{
                                                batchItemsAdapter?.notifyDataSetChanged()
                                                GlobalMethods.showError(context, "Batch / Roll No. has zero Quantity of this SO.")
                                            }

                                        }else{
                                            batchItemsAdapter?.notifyDataSetChanged()
                                            GlobalMethods.showError(context, "No Quantity Found of this SO.")
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





    override fun onDeleteItemClick(list: ArrayList<ScanedOrderBatchedItems.Value>, quantityHashMap1: ArrayList<String>, pos: Int, parentPosition : Int, rvBatchItems : RecyclerView) {
        MaterialAlertDialogBuilder(context)
            .setTitle("Confirm...")
            .setMessage("Do you want to delete " + list[pos].Batch + " Batch Item .")
            .setIcon(R.drawable.ic_trash)
            .setPositiveButton("Confirm",
                DialogInterface.OnClickListener { dialogInterface: DialogInterface?, i1: Int ->
                    Log.e("before_valueList===>", list.size.toString())
                    Log.e("before_batch===>", quantityHashMap1.size.toString())
                    list.removeAt(pos)
                    quantityHashMap1.removeAt(pos)
                    batchItemsAdapter?.notifyDataSetChanged()

                    Log.e("after_valueList===>", list.size.toString())
                    Log.e("after_batch===>", quantityHashMap1.size.toString())
                })
            .setNegativeButton("Cancel",
                DialogInterface.OnClickListener { dialogInterface, i ->
                    dialogInterface.dismiss()
                })
            .show()
    }


    //TODO duplicatcy checking from list...
    fun checkDuplicate(scanedBatchedItemsList_gl: ArrayList<ScanedOrderBatchedItems.Value>, batchCode: String): Boolean {
        var status: Boolean = true;
        for (items in scanedBatchedItemsList_gl) {
            if (items.Batch.equals(batchCode)) {
                status = false
                Toast.makeText(context, "Duplicate Roll No. / Batch no. Already Exists!", Toast.LENGTH_SHORT).show()
            }
        }
        return status
    }


    open fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            val result = data?.getStringExtra("batch_code")
            Log.e("ItemCode===>", itemCode)
            //todo spilt string and get string at 0 index...
            if (checkDuplicate(hashMap.get("Item" + pos)!!, result.toString().split(",")[0])) {
                scanOrderLinesItem(result.toString().split(",")[0], recyclerView, pos, itemCode, tvOpenQty, remainingOpenQuantity, width, U_gsmso, tvTotalScanQty, tvTotalScanGW)
            }

        }
    }

    //TODO set sql server for query...
    private fun setSqlServer() {
        val url = "jdbc:jtds:sqlserver://" + AppConstants.IP + ":" + AppConstants.PORT + "/" + sessionManagement.getCompanyDB(context)
        ActivityCompat.requestPermissions(context as Activity, arrayOf<String>(Manifest.permission.INTERNET), PackageManager.PERMISSION_GRANTED)
        val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
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
    }

    //TODO POPUP DIALOG...

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

    //TODO popup for laser scanner...
    fun laserScannerPopupDialog(rvBatchItems: RecyclerView, position: Int, itemCode: String?, tvOpenQty : TextView) {
        val builder = AlertDialog.Builder(context, R.style.CustomAlertDialog).create()
        val view = LayoutInflater.from(context).inflate(R.layout.laser_scanner_dialog_layout, null)
        builder.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        builder.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        builder.window?.setGravity(Gravity.CENTER)
        builder.setView(view)

        var ed_batch_code_scan = view.findViewById<EditText>(R.id.ed_batch_code_scan)
        var cancelBtn = view.findViewById<AppCompatButton>(R.id.cancelBtn)
        var okBtn = view.findViewById<AppCompatButton>(R.id.okBtn)

        cancelBtn?.setOnClickListener {
            builder.dismiss()
        }

        //todo for laser scanner..

        ed_batch_code_scan.setOnTouchListener(View.OnTouchListener { view1: View, motionEvent: MotionEvent? ->
            ed_batch_code_scan.setText("")
            ed_batch_code_scan.setCursorVisible(true)
            ed_batch_code_scan.setFocusableInTouchMode(true)
            ed_batch_code_scan.requestFocus()
            val imm = view.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(view1.windowToken, 0)
            true
        })

        ed_batch_code_scan.setOnKeyListener { view1, keyCode, keyEvent ->
            if (ed_batch_code_scan.isFocused() && ed_batch_code_scan.isCursorVisible() && ed_batch_code_scan.hasFocus()) {
                var inputMethodManager = view.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                inputMethodManager.hideSoftInputFromWindow(ed_batch_code_scan.getWindowToken(), 0)
            }
            if (keyEvent.action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
                var text = ed_batch_code_scan.text.toString().trim()
                var size = 0
                //todo validation for stop multiple scanning at one order line...
                if (hashMap.get("Item" + position) is List<*>) {
                    val list = hashMap.get("Item" + position) as List<*>
                    if (list.size == size) {
                        Log.e("size===>", list.size.toString())
                        //todo scan call api here...
//                        scanOrderLinesItem(text, rvBatchItems, position, itemCode, tvOpenQty, this.BaseOpenQuantity)
                    } else {
                        GlobalMethods.showError(context, "Can not Scan multiple Batch")
                        Log.e("SizeGreaterThanOne===>", "Error")
                    }
                }
                ed_batch_code_scan.setText("")
                ed_batch_code_scan.requestFocus()
            }
            return@setOnKeyListener false
        }

        //todo for manual batch entry..
        ed_batch_code_scan?.setOnEditorActionListener { v, actionId, event ->
            //event.action == KeyEvent.ACTION_DOWN && actionId == KeyEvent.KEYCODE_ENTER ||
            if (actionId == KeyEvent.ACTION_DOWN && actionId == KeyEvent.KEYCODE_ENTER || actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_GO || actionId == EditorInfo.IME_ACTION_SEND) {
                var text = ed_batch_code_scan.text.toString().trim()
                if (checkDuplicate(hashMap.get("Item" + position)!!, text)) {
                    //todo scan call api here...
//                    scanOrderLinesItem(text, rvBatchItems, position, itemCode, tvOpenQty, this.BaseOpenQuantity)
                }
                builder.dismiss()
                true

            } else {
                false
            }
        }

        //todo scanned batch lines...
        okBtn?.setOnClickListener {
            var text = ed_batch_code_scan?.text.toString().trim()
            if (checkDuplicate(hashMap.get("Item" + position)!!, text)) {
                //todo scan call api here...
//                scanOrderLinesItem(text, rvBatchItems, position, itemCode, tvOpenQty, this.BaseOpenQuantity)
            }
            builder.dismiss()
        }

        builder.setCancelable(true)
        builder.show()
    }


}