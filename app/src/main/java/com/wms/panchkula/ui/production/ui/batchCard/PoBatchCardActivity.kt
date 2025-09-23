package com.wms.panchkula.ui.production.ui.batchCard

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.gson.GsonBuilder
import com.pixplicity.easyprefs.library.Prefs
import com.webapp.internetconnection.CheckNetwoorkConnection
import com.wms.panchkula.Global_Classes.AppConstants
import com.wms.panchkula.Global_Classes.GlobalMethods
import com.wms.panchkula.Global_Classes.GlobalMethods.mapStageModelToListModel
import com.wms.panchkula.Global_Classes.GlobalMethods.toPrettyJson
import com.wms.panchkula.Global_Classes.MaterialProgressDialog
import com.wms.panchkula.Global_Notification.NetworkConnection
import com.wms.panchkula.Model.OtpErrorModel
import com.wms.panchkula.Retrofit_Api.ApiConstantForURL
import com.wms.panchkula.Retrofit_Api.NetworkClients
import com.wms.panchkula.Retrofit_Api.QuantityNetworkClient
import com.wms.panchkula.SessionManagement.SessionManagement
import com.wms.panchkula.databinding.ActivityPoBatchCodeBinding
import com.wms.panchkula.ui.deliveryOrderModule.Adapter.DeliveryListAdapter
import com.wms.panchkula.ui.deliveryOrderModule.Model.DeliveryModel
import com.wms.panchkula.ui.issueForProductionOrder.Adapter.IssueOderAdapter
import com.wms.panchkula.ui.issueForProductionOrder.Model.ProductionListModel
import com.wms.panchkula.ui.issueForProductionOrder.UI.productionOrderLines.ProductionOrderLinesActivity
import com.wms.panchkula.ui.login.LoginActivity
import com.wms.panchkula.ui.production.adapter.batchCode.StageItemAdapter
import com.wms.panchkula.ui.production.model.batchCode.ProductionOrderData
import com.wms.panchkula.ui.production.model.batchCode.ProductionOrderStageModel
import com.wms.panchkula.ui.production.model.batchCode.StageUpdateRequest
import com.wms.panchkula.ui.production.ui.rfp.RFPLinesActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.IOException
import java.io.Serializable

class PoBatchCardActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPoBatchCodeBinding
    lateinit var materialProgressDialog: MaterialProgressDialog
    lateinit var checkNetwoorkConnection: CheckNetwoorkConnection
    private lateinit var networkConnection: NetworkConnection

    private var poStagesList: ArrayList<ProductionOrderStageModel.Value> = ArrayList()
    private lateinit var productionOrderStageModel: ProductionOrderStageModel
    private lateinit var sessionManagement: SessionManagement
    private lateinit var stageAdapter: StageItemAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPoBatchCodeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initViews()
        clickListener()
    }

    private fun initViews() {
        title = "Batch Card (PO)"
        binding.edPoCodeScan.requestFocus()
        materialProgressDialog = MaterialProgressDialog(this@PoBatchCardActivity)
        checkNetwoorkConnection = CheckNetwoorkConnection(application)
        networkConnection = NetworkConnection()
        sessionManagement = SessionManagement(this@PoBatchCardActivity)
        //loadPoListItems("11")

    }

    private fun clickListener() {
        binding.apply {

            chipRfp.setOnClickListener {
                val docNum = poStagesList[0].DocumentNumber
                startActivity(Intent(this@PoBatchCardActivity,RFPLinesActivity::class.java).apply {
                    putExtra("PO_NO",docNum)
                })
            }

            edPoCodeScan.addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {
                    try {
                        val scannedData = s.toString().trim()

                        Log.i("PO_DATA", "PO Scan Data: $scannedData")
                        if (scannedData.isNotEmpty()) {
                            edPoCodeScan.setText("")
                            loadPoListItems(scannedData)
                        }

                        Handler(Looper.getMainLooper()).postDelayed({
                            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                            if (imm != null && edPoCodeScan != null) {
                                imm.hideSoftInputFromWindow(edPoCodeScan.windowToken, 0)
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
    }

    fun loadPoListItems(docNum: String) {
        checkNetwoorkConnection.observe(this) { isConnected ->
            if (isConnected) {
                materialProgressDialog.show()
                var apiConfig = ApiConstantForURL()
                QuantityNetworkClient.updateBaseUrlFromConfig(apiConfig)

                val networkClient = QuantityNetworkClient.create(this)
                val bplId = if (Prefs.getString(AppConstants.BPLID, "").isNotEmpty()) Prefs.getString(AppConstants.BPLID, "") else ""
                networkClient.doGetProductionListByQr(sessionManagement.getCompanyDB(this)!!, bplId, docNum)
                    .apply { // "odata.maxpagesize=" + 100, "ProductionOrderStatus eq '" + "boposReleased" + "'","" + skip,
                        // "AbsoluteEntry desc"
                        enqueue(object : Callback<ProductionOrderStageModel> {
                            override fun onResponse(call: Call<ProductionOrderStageModel>, response: Response<ProductionOrderStageModel>) {
                                try {
                                    if (response.isSuccessful) {
                                        Log.e("api_hit_response===>", response.toString())
                                        materialProgressDialog.dismiss()
                                        productionOrderStageModel = response.body()!!
                                        var productionList_gl = productionOrderStageModel.value
                                        if (productionList_gl.isEmpty()) {
                                            binding.tvNoDataFound.visibility = View.VISIBLE
                                            binding.rvPoStages.visibility = View.INVISIBLE
                                            binding.layoutData.visibility = View.INVISIBLE
                                        } else {
                                            binding.tvNoDataFound.visibility = View.INVISIBLE
                                            binding.rvPoStages.visibility = View.VISIBLE
                                            binding.layoutData.visibility = View.VISIBLE
                                            //Toast.makeText(this@ProductionListActivity, "Successfully!", Toast.LENGTH_SHORT)
                                            poStagesList.clear() // <- make sure old data is removed
                                            poStagesList.addAll(productionList_gl)
                                            binding.tvDocNum.text = poStagesList[0].DocumentNumber
                                            binding.tvDocDate.text = poStagesList[0].PostingDate
                                            setStageAdapter()
                                            stageAdapter?.notifyDataSetChanged()
                                        }
                                    } else {
                                        materialProgressDialog.dismiss()
                                        val gson1 = GsonBuilder().create()
                                        var mError: OtpErrorModel
                                        try {
                                            val s = response.errorBody()!!.string()
                                            mError = gson1.fromJson(s, OtpErrorModel::class.java)
                                            if (mError.error.code == 400) {
                                                GlobalMethods.showError(this@PoBatchCardActivity, mError.error.message.value)
                                            }
                                            if (mError.error.code == 306 && mError.error.message.value != null) {
                                                GlobalMethods.showError(this@PoBatchCardActivity, mError.error.message.value)
                                                val mainIntent = Intent(this@PoBatchCardActivity, LoginActivity::class.java)
                                                startActivity(mainIntent)
                                                finish()
                                            }
                                            /*if (mError.error.message.value != null) {
                                                AppConstants.showError(this@ProductionListActivity, mError.error.message.value)
                                                Log.e("json_error------", mError.error.message.value)
                                            }*/
                                        } catch (e: IOException) {
                                            e.printStackTrace()
                                        }
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }

                            override fun onFailure(call: Call<ProductionOrderStageModel>, t: Throwable) {
                                Log.e("issueCard_failure-----", t.toString())
                                materialProgressDialog.dismiss()
                            }
                        })
                    }

            } else {
                materialProgressDialog.dismiss()
                GlobalMethods.showError(this, "No Network Connection")
            }
        }

    }

    private fun setStageAdapter() {
        stageAdapter = StageItemAdapter(
            stages = poStagesList[0].ProductionOrdersStages,
            plannedQuantity = poStagesList[0].PlannedQuantity,
            onStageClick = { stagePos, stage ->
                if (stage.ProductionOrderLines.isNotEmpty()) {
                    val productionListModel = mapStageModelToListModel(productionOrderStageModel)
                    Log.e("PO_DATA", "productionListModel : ${toPrettyJson(productionListModel)}")
                    var productionValueList = productionListModel.value[0]
                    var productionLinesList = productionListModel.value[0].ProductionOrdersStages[stagePos].ProductionOrderLines

                    Toast.makeText(this, "Stage [$stagePos]: ${stage.Name}", Toast.LENGTH_SHORT).show()
                    Log.i("PO_DATA", "Stage Item[$stagePos] List : ${stage.ProductionOrderLines}")
                    startActivity(Intent(this@PoBatchCardActivity, ProductionOrderLinesActivity::class.java).apply {
                        putExtra("productionLinesList", productionLinesList as Serializable)
                        putExtra("productionValueList", productionValueList as Serializable)
                    })

                    if (productionLinesList.size > 0) {
                        sessionManagement.setWarehouseCode(this@PoBatchCardActivity, productionLinesList[0].Warehouse)
                    }
                } else {
                    GlobalMethods.showError(this@PoBatchCardActivity, "RM item is not available in this stage.")
                }

            },
            onStageUpdate = { stagePos, updatedStage ->

                // Updated stage object with AcceptQty & RejectQty
                Log.d("STAGE_UPDATE", "Stage ${updatedStage.StageId} => Accept: ${updatedStage.AcceptQty}, Reject: ${updatedStage.RejectQty}")
                val stageRequest = StageUpdateRequest()

                // Add stage dynamically
                stageRequest.ProductionOrdersStages.add(
                    StageUpdateRequest.ProductionOrderStage(
                        StageID = updatedStage.StageId.toInt(),
                        U_AQty = updatedStage.AcceptQty ?: 0.0,
                        U_RQty = updatedStage.RejectQty ?: 0.0
                    )
                )

                Log.i("STAGE_UPDATE", "JSON: ${toPrettyJson(stageRequest)}")
                callPOStageApi(stageRequest)
            }
        )

        binding.rvPoStages.layoutManager = LinearLayoutManager(this)
        binding.rvPoStages.adapter = stageAdapter
    }

    private fun callPOStageApi(requestModel: StageUpdateRequest) {
        if (networkConnection.getConnectivityStatusBoolean(applicationContext)) {
            materialProgressDialog.show()
            var apiConfig = ApiConstantForURL()

            NetworkClients.updateBaseUrlFromConfig(apiConfig)

            val networkClient = NetworkClients.create(this@PoBatchCardActivity)
            networkClient.updateProductionOrderStage(productionOrderStageModel.value[0].AbsoluteEntry, requestModel).apply {
                enqueue(object : Callback<Void> {
                    override fun onResponse(
                        call: Call<Void>,
                        response: Response<Void>
                    ) {
                        try {
                            materialProgressDialog.dismiss()
                            if (response.isSuccessful && response.code() == 204) {
                                Log.d("success------", "PickList updated successfully")
                                runOnUiThread {
                                    GlobalMethods.showSuccess(
                                        this@PoBatchCardActivity,
                                        "Stage updated successfully."
                                    )
                                }

                                Handler(Looper.getMainLooper()).postDelayed({
                                    finish()
                                }, 1000)
                            } else {
                                materialProgressDialog.dismiss()
                                val gson1 = GsonBuilder().create()
                                var mError: OtpErrorModel
                                try {
                                    val s = response.errorBody()!!.string()
                                    mError = gson1.fromJson(s, OtpErrorModel::class.java)
                                    if (mError.error.code.equals(400)) {
                                        GlobalMethods.showError(
                                            this@PoBatchCardActivity,
                                            mError.error.message.value
                                        )
                                    }
                                    if (mError.error.message.value != null) {
                                        GlobalMethods.showError(
                                            this@PoBatchCardActivity,
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

                    override fun onFailure(call: Call<Void>, t: Throwable) {
                        Log.e("orderLines_failure-----", t.toString())
                        materialProgressDialog.dismiss()
                    }

                })
            }
        } else {
            materialProgressDialog.dismiss()
            GlobalMethods.showError(this@PoBatchCardActivity, "No network connection")
        }
    }
}