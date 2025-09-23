package com.wms.panchkula.issueOrder

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.*
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.wms.panchkula.Global_Classes.AppConstants
import com.wms.panchkula.Global_Classes.GlobalMethods
import com.wms.panchkula.Global_Classes.MaterialProgressDialog
import com.wms.panchkula.Global_Notification.NetworkConnection
import com.wms.panchkula.Model.*
import com.wms.panchkula.Retrofit_Api.NetworkClients
import com.wms.panchkula.Retrofit_Api.QuantityNetworkClient
import com.wms.panchkula.SessionManagement.SessionManagement
import com.wms.panchkula.databinding.ActivityIssueOrderDetailDemoBinding
import com.wms.panchkula.interfaces.PassList
import com.wms.panchkula.Retrofit_Api.ApiConstantForURL
import com.wms.panchkula.ui.issueForProductionOrder.Model.InventoryGenExitsModel
import com.wms.panchkula.ui.issueForProductionOrder.Model.ProductionListModel
import com.wms.panchkula.ui.issueForProductionOrder.Model.ScanedOrderBatchedItems
import com.wms.panchkula.ui.issueForProductionOrder.Model.WarehouseBPL_IDModel
import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.io.IOException
import java.lang.reflect.InvocationTargetException
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException
import java.sql.Statement
import kotlin.collections.ArrayList


class IssueOrderDetailDemoActivity : AppCompatActivity(), PassList {

    private lateinit var activityFormBinding: ActivityIssueOrderDetailDemoBinding
    private var issueDemoAdapterReference: IssueDemoAdapter? = null
    private lateinit var productionOrderLineList_gl: ArrayList<ProductionListModel.ProductionOrderLine>

    //    private lateinit var productionOrderValueList_gl: ArrayList<ProductionListModel.Value>
    private lateinit var productionOrderValueList_gl: ProductionListModel.Value
    var position: Int? = 0
    lateinit var networkConnection: NetworkConnection
    lateinit var materialProgressDialog: MaterialProgressDialog
    private lateinit var sessionManagement: SessionManagement
    private var BPLIDNum = 0
    private var valueList: List<ProductionListModel.Value> = ArrayList()
    private var connection: Connection? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activityFormBinding = ActivityIssueOrderDetailDemoBinding.inflate(layoutInflater)
        setContentView(activityFormBinding.root)
        title = "Form Screen"

        deleteCache(this)

        supportActionBar?.hide()


        //todo initialization...
        networkConnection = NetworkConnection()
        materialProgressDialog = MaterialProgressDialog(this@IssueOrderDetailDemoActivity)
        sessionManagement = SessionManagement(this@IssueOrderDetailDemoActivity)


        val delayMillis = 1000 // 1 second
        handler.postDelayed({
//            setSqlServer()
        }, delayMillis.toLong())


        //todo get arguments data...
        try {
            val intent = intent
            productionOrderLineList_gl = intent.getSerializableExtra("productionLinesList") as ArrayList<ProductionListModel.ProductionOrderLine>
            productionOrderValueList_gl = intent.getSerializableExtra("productionValueList") as ProductionListModel.Value //todo getting list selected item values and lines only not all size data..
            position = intent.extras?.getInt("pos")

            valueList = listOf(productionOrderValueList_gl)

        } catch (e: IOException) {
            Log.e(TAG, "onCreate:===> " + e.message)
            e.printStackTrace()
        }

        //todo calling BPLID here...
        getBPL_IDNumber()

        activityFormBinding.tvItemNo.text = productionOrderValueList_gl.ItemNo


        activityFormBinding.ivOnback.setOnClickListener {
            onBackPressed()
        }


        //todo cancel lines...
        activityFormBinding.chipCancel.setOnClickListener {
            AppConstants.scannedItemForIssueOrder.clear()
            onBackPressed()
        }



    }

    override fun onBackPressed() {
        super.onBackPressed()
        AppConstants.scannedItemForIssueOrder.clear()
    }

    var tempList: ArrayList<ProductionListModel.ProductionOrderLine> = ArrayList()

    //todo set adapter....
    fun setAdapter() {
        //todo removing order line if BaseQuantity value in negative.... and also this way is removing IndexOutOfBoundException from list....

        for (i in 0 until productionOrderLineList_gl.size) {
            var j: Int = i
            try {
                val plannedQty = productionOrderLineList_gl[j].PlannedQuantity
                val issuedQty = productionOrderLineList_gl[j].IssuedQuantity
                val openQty = plannedQty - issuedQty
                if (productionOrderLineList_gl[j].BaseQuantity > 0.0 && openQty > 0.0) {
                    tempList.add(productionOrderLineList_gl[j])
                }
            } catch (e: IndexOutOfBoundsException) {
                e.printStackTrace()
            }
        }


        var width = productionOrderValueList_gl.U_Width
        var length = productionOrderValueList_gl.U_Length
        var gsm = productionOrderValueList_gl.U_GSM

        if (tempList.size > 0) {
            activityFormBinding.ivNoDataFound.visibility = View.GONE
            activityFormBinding.rvProductionOrderList.visibility = View.VISIBLE
            activityFormBinding.btnLinearLayout.visibility = View.VISIBLE

            val layoutManager: RecyclerView.LayoutManager = LinearLayoutManager(this)
            activityFormBinding.rvProductionOrderList.layoutManager = layoutManager
            //todo parse save button in adapter constructor for click listener on adapter...
            issueDemoAdapterReference = IssueDemoAdapter(this@IssueOrderDetailDemoActivity, tempList, networkConnection, materialProgressDialog)//getWarehouseCode
            activityFormBinding.rvProductionOrderList.adapter = issueDemoAdapterReference
            issueDemoAdapterReference?.notifyDataSetChanged()
        } else {
            activityFormBinding.ivNoDataFound.visibility = View.VISIBLE
            activityFormBinding.rvProductionOrderList.visibility = View.GONE
            activityFormBinding.btnLinearLayout.visibility = View.GONE
        }


    }


    lateinit var batchList: List<ScanedOrderBatchedItems.Value>
    lateinit var batchQuantityList: ArrayList<String>

    //todo here save issue for production lines items of order...
    private fun saveProductionOrderLinesItems() {
        var comments = valueList[0].Remarks.toString()
        var docDate = valueList[0].PostingDate
        var absoluteEntry = valueList[0].AbsoluteEntry

        var series = getSeriesValue(docDate)

        if (networkConnection.getConnectivityStatusBoolean(applicationContext)) {
            materialProgressDialog.show()

            var postedJson: JsonObject = JsonObject()
            postedJson.addProperty("BPL_IDAssignedToInvoice", BPLIDNum)
            postedJson.addProperty("Comments", comments)
            postedJson.addProperty("DocDate", GlobalMethods.getCurrentDateFormatted()) //todo current date will send here---
            postedJson.addProperty("Series", valueList[0].SeriesCode) //series

            val DocumentLinesArray = JsonArray()



            postedJson.add("DocumentLines", DocumentLinesArray)

            Log.e("success--PayLoad==>", "==>" + postedJson.toString())

            var apiConfig = ApiConstantForURL()

            NetworkClients.updateBaseUrlFromConfig(apiConfig)

            QuantityNetworkClient.updateBaseUrlFromConfig(apiConfig)

            val networkClient = NetworkClients.create(this@IssueOrderDetailDemoActivity)
            networkClient.doGetInventoryGenExits(postedJson).apply {
                enqueue(object : Callback<InventoryGenExitsModel> {
                    override fun onResponse(
                        call: Call<InventoryGenExitsModel>,
                        response: Response<InventoryGenExitsModel>
                    ) {
                        try {
                            activityFormBinding.chipSave.isEnabled = true
                            activityFormBinding.chipSave.isCheckable = true
                            materialProgressDialog.dismiss()
                            if (response.isSuccessful) {
                                if (response.code() == 201) {
                                    Log.e("success------", "Successful!")
                                    GlobalMethods.showSuccess(this@IssueOrderDetailDemoActivity, "Issue Production Order Post Successfully. "+response.body()!!.DocNum.toString())
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
                                            this@IssueOrderDetailDemoActivity,
                                            mError.error.message.value
                                        )
                                    }
                                    if (mError.error.message.value != null) {
                                        GlobalMethods.showError(
                                            this@IssueOrderDetailDemoActivity,
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

                    override fun onFailure(call: Call<InventoryGenExitsModel>, t: Throwable) {
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
                    this@IssueOrderDetailDemoActivity,
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

                val networkClient = NetworkClients.create(this@IssueOrderDetailDemoActivity)
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
//                                        sessionManagement.setWarehouseCode(this@IssueOrderDetailDemoActivity, responseModel.value[0].WarehouseCode)
                                        setAdapter()
//                                        Toast.makeText(this@IssueOrderDetailDemoActivity, BPLIDNum.toString(), Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(
                                            this@IssueOrderDetailDemoActivity,
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
                                                this@IssueOrderDetailDemoActivity,
                                                mError.error.message.value
                                            )
                                        }
                                        if (mError.error.message.value != null) {
                                            GlobalMethods.showError(
                                                this@IssueOrderDetailDemoActivity,
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
            Log.e("Result==>", data?.getStringExtra("batch_code").toString())
            issueDemoAdapterReference?.onActivityResult(requestCode, resultCode, data)
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
        val url = "jdbc:jtds:sqlserver://" + AppConstants.IP + ":" + AppConstants.PORT + "/" + sessionManagement.getCompanyDB(this)
        ActivityCompat.requestPermissions(this, arrayOf<String>(Manifest.permission.INTERNET), PackageManager.PERMISSION_GRANTED)
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


    companion object {
        private const val INTERNET_PERMISSION_REQUEST_CODE = 1
        private const val TAG = "IssueOrderDetailDemoAct"

    }

}