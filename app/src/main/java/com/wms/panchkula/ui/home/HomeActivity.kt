package com.wms.panchkula.ui.home

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.AppCompatButton
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import com.wms.panchkula.Global_Classes.AppConstants
import com.wms.panchkula.Global_Classes.MaterialProgressDialog
import com.wms.panchkula.Global_Notification.NetworkConnection
import com.wms.panchkula.R
import com.wms.panchkula.Retrofit_Api.NetworkClients
import com.wms.panchkula.Retrofit_Api.QuantityNetworkClient
import com.wms.panchkula.SessionManagement.SessionManagement
import com.wms.panchkula.databinding.ActivityHomeBinding
import com.wms.panchkula.Retrofit_Api.ApiConstantForURL
import com.wms.panchkula.ui.goodsOrder.GoodsOrderActivity
import com.wms.panchkula.ui.inventoryOrder.UI.InventoryOrderActivity_ITR_GRPO
import com.wms.panchkula.ui.login.LoginActivity
import com.wms.panchkula.ui.issueForProductionOrder.UI.productionOrderLines.ProductionListActivity
import com.wms.panchkula.ui.login.Model.LoginResponseModel
import com.wms.panchkula.ui.scan_and_view.ScanQRViewActivity
import com.pixplicity.easyprefs.library.Prefs
import com.webapp.internetconnection.CheckNetwoorkConnection
import com.wms.panchkula.Adapter.HomeAdapter
import com.wms.panchkula.BuildConfig
import com.wms.panchkula.Model.HomeItem
import com.wms.panchkula.Model.ModelDashboardItem
import com.wms.panchkula.SessionManagement.UserManagementPrefs
import com.wms.panchkula.ui.goodsreceipt.GoodsReceiptActivity
import com.wms.panchkula.ui.inventoryOrder.oldUI.InventoryOrderActivity
import com.wms.panchkula.ui.inventoryTransfer.InventoryTransferActivity
import com.wms.panchkula.ui.pickList.PickListActivity
import com.wms.panchkula.ui.production.ui.batchCard.PoBatchCardActivity
import com.wms.panchkula.ui.production.ui.rfp.RFPActivity
import com.wms.panchkula.ui.purchase.PurchaseOrderActivity
import com.wms.panchkula.ui.returnComponents.ui.ReturnComponentListActivity
import com.wms.panchkula.ui.saleToDelivery.DeliveryListActivity
import com.wms.panchkula.ui.saletoinvoice.SaleToInvoiceActivity
import com.wms.panchkula.ui.setting.SettingActivity
import es.dmoral.toasty.Toasty
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class HomeActivity : AppCompatActivity() {

    /***** TODO *****/
    private lateinit var homeBinding: ActivityHomeBinding
    lateinit var networkConnection: NetworkConnection
    lateinit var materialProgressDialog: MaterialProgressDialog
    lateinit var sessionManagement: SessionManagement
    private lateinit var userPrefs: UserManagementPrefs
    private var isLoggingOut = false

    lateinit var checkNetwoorkConnection: CheckNetwoorkConnection
    val listHome: ArrayList<HomeItem> = arrayListOf()
    var userMgmtList: ArrayList<ModelDashboardItem.Value> = arrayListOf()
    var flag: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        homeBinding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(homeBinding.root)
        Log.e("Environment", "HomeActivity: ${Prefs.getBoolean(AppConstants.IS_TEST_ENVIRONMENT)}")
        //todo set title on header...
        title = "Menu Screen"
        supportActionBar?.setDisplayShowHomeEnabled(true)

        networkConnection = NetworkConnection()
        materialProgressDialog = MaterialProgressDialog(this@HomeActivity)
        checkNetwoorkConnection = CheckNetwoorkConnection(application)
        sessionManagement = SessionManagement(this)
        //homeBinding.tvDBName.text = "DB:- ${sessionManagement.getCompanyDB(this@HomeActivity)}"
        val port = ApiConstantForURL().PORT

        homeBinding.tvDBName.apply {
            text = "DB:- ${sessionManagement.getCompanyDB(this@HomeActivity)}"
            setTextColor(
                ContextCompat.getColor(context, if (port == 9090) R.color.status_live else R.color.status_test)
            )
        }

        /*homeBinding.tvAppVersion.text =
            if (port == 9090) "${BuildConfig.FORCED_VERSION_NAME} (Live)" else "${BuildConfig.FORCED_VERSION_NAME} (Test)"*/

        homeBinding.tvAppVersion.apply {
            text = if (port == 9090) {
                "${BuildConfig.FORCED_VERSION_NAME} (Live)"
            } else {
                "${BuildConfig.FORCED_VERSION_NAME} (Test)"
            }

            setTextColor(
                ContextCompat.getColor(context, if (port == 9090) R.color.status_live else R.color.status_test)
            )
        }


        //userPrefs = UserManagementPrefs(this)

        //userMgmtList = userPrefs.getUserMgmtData()?.value!!

        //Log.e("USER_MGMT", "HomeActivity: ${toPrettyJson(userMgmtList)}")
        //sessionManagement.setScannerType(this, "LEASER")
        /*for (item in userMgmtList) {
            val imageRes = if (item.module.equals(AppConstants.ISSUE_FOR_PRODUCTION, true)) {
                R.drawable.issue_prod_icon
            } else if (item.module.equals(AppConstants.SCAN_AND_VIEW, true)) {
                R.drawable.returnproduction_icon
            } else if (item.module.equals(AppConstants.GOODS_ISSUE, true)) {
                R.drawable.delivery_icon
            } else {
                R.drawable.receipt_prod_icon
            }
            val title = if (item.module.equals(AppConstants.ISSUE_FOR_PRODUCTION, true)) {
                "Issue for Production"
            } else if (item.module.equals(AppConstants.SCAN_AND_VIEW, true)) {
                "Scan & View"
            } else if (item.module.equals(AppConstants.INVENTORY_REQ, true)) {
                "Inventory Req."
            } else if (item.module.equals(AppConstants.GOODS_ISSUE, true)) {
                "Goods Issue"
            } else if (item.module.equals(AppConstants.INVENTORY_TRANSFER_GRPO, true)) {
                "Inventory Transfer (GRPO)"
            } else if (item.module.equals(AppConstants.GOODS_RECEIPT_PO, true)) {
                "Goods Receipt PO"
            } else if (item.module.equals(AppConstants.SALE_TO_INVOICE, true)) {
                "Sale To Invoice"
            } else if (item.module.equals(AppConstants.RECEIPT_FROM_PRODUCTION, true)) {
                "Receipt from Production"
            } else if (item.module.equals(AppConstants.PICK_LIST, true)) {
                "Pick List"
            } else if (item.module.equals(AppConstants.RETURN_COMPONENTS, true)) {
                "Return Components"
            } else if (item.module.equals(AppConstants.GOODS_RECEIPT, true)) {
                "Goods Receipt"
            } else if (item.module.equals(AppConstants.INVENTORY_TRANSFER_STANDALONE, true)) {
                "Inventory Transfer"
            } else {
                "Sale To Delivery"
            }
            if (item.module != AppConstants.SALE_TO_DELIVERY)
                listHome.add(HomeItem(imageRes, title, item.module, item.status))
        }*/

        //Log.e("USER_MGMT", "HomeActivity => Final list: ${toPrettyJson(listHome)}")
        val items = listOf(
            HomeItem(
                R.drawable.issue_prod_icon,
                "Production/Batch Card",
                AppConstants.PRODUCTION_BATCH_CARD,
                "Y"
            ), HomeItem(
                R.drawable.issue_prod_icon,
                "Issue for Production",
                AppConstants.ISSUE_FOR_PRODUCTION,
                "Y"
            ),
            HomeItem(R.drawable.ic_scan_view, "Scan & View", AppConstants.SCAN_AND_VIEW, "Y"),
            HomeItem(
                R.drawable.ic_inventory_req,
                "Inventory Req.",
                AppConstants.INVENTORY_REQ,
                "Y"
            ),
            HomeItem(R.drawable.delivery_icon, "Goods Issue", AppConstants.GOODS_ISSUE, "Y"),
            HomeItem(
                R.drawable.receipt_prod_icon,
                "Inventory Transfer (GRPO)",
                AppConstants.INVENTORY_TRANSFER_GRPO,
                "Y"
            ),
            HomeItem(
                R.drawable.receipt_prod_icon,
                "Goods Receipt PO",
                AppConstants.GOODS_RECEIPT_PO,
                "Y"
            ),
            HomeItem(
                R.drawable.receipt_prod_icon,
                "Sale To Invoice",
                AppConstants.SALE_TO_INVOICE,
                "N"
            ), // for demo only
            HomeItem(
                R.drawable.receipt_prod_icon,
                "Receipt from Production",
                AppConstants.RECEIPT_FROM_PRODUCTION,
                "Y"
            ),
            HomeItem(R.drawable.receipt_prod_icon, "Pick List", AppConstants.PICK_LIST, "N"),
            HomeItem(
                R.drawable.receipt_prod_icon,
                "Return Components",
                AppConstants.RETURN_COMPONENTS,
                "Y"
            ),
            HomeItem(
                R.drawable.receipt_prod_icon,
                "Goods Receipt",
                AppConstants.GOODS_RECEIPT,
                "Y"
            ),
            HomeItem(
                R.drawable.receipt_prod_icon,
                "Inventory Transfer",
                AppConstants.INVENTORY_TRANSFER_STANDALONE,
                "Y"
            ),
            HomeItem(
                R.drawable.receipt_prod_icon,
                "Sale To Delivery",
                AppConstants.SALE_TO_DELIVERY,
                "N"
            )
        )

        val filterItems = items.filter { it.status == "Y" }

        //Log.e("USER_MGMT", "HomeActivity => filterItems: ${toPrettyJson(filterItems)}")
        setHomeItemAdapter(filterItems)
        /*homeBinding.issueCard.setOnClickListener {
            //callIssueCardApi()
            flag = "Issue_Order"
            var intent: Intent = Intent(this@HomeActivity, ProductionListActivity::class.java)
            intent.putExtra("flag", flag)
            startActivity(intent)
        }

        homeBinding.scanAndView.setOnClickListener {
            // GlobalMethods.showMessage(this, "Work In Process.")
            var intent: Intent = Intent(this, ScanQRViewActivity::class.java)
            startActivity(intent)
        }

        homeBinding.receiptCard.setOnClickListener {
            // GlobalMethods.showMessage(this, "Work In Process.")
            var intent: Intent = Intent(this, InventoryOrderActivity_ITR_GRPO::class.java)
            startActivity(intent)
        }

        homeBinding.inventoryReqCard.setOnClickListener {
            // flag  = "Delivery_Order"
            Log.e("warehouse", "onCreate:inventoryReqCard ")

            var intent: Intent = Intent(this, InventoryOrderActivity::class.java)
            // intent.putExtra("flag",flag)
            startActivity(intent)
        }

        homeBinding.saleToInvoiceCard.setOnClickListener {
            // flag  = "Delivery_Order"
            var intent: Intent = Intent(this, SaleToInvoiceActivity::class.java)
            // intent.putExtra("flag",flag)
            startActivity(intent)
        }

        homeBinding.pickListCard.setOnClickListener {
            var intent = Intent(this, PickListActivity::class.java)
            startActivity(intent)
        }

        homeBinding.returnComponentsCard.setOnClickListener {
            flag = "Issue_Order"
            var intent = Intent(this@HomeActivity, ReturnComponentsListActivity::class.java)
            intent.putExtra("flag", flag)
            startActivity(intent)
        }

        homeBinding.deliveryCard.setOnClickListener {
            var intent: Intent = Intent(this, GoodsOrderActivity::class.java)
            // var intent: Intent = Intent(this, DemoActivity::class.java)
            startActivity(intent)
        }
        homeBinding.purchaseOrderCard.setOnClickListener {
            var intent: Intent = Intent(this, PurchaseOrderActivity::class.java)
            startActivity(intent)
        }

        homeBinding.receiptProductionCard.setOnClickListener {
            var intent: Intent = Intent(this, RFPActivity::class.java)
            startActivity(intent)
        }
        homeBinding.GoodReceiptCard.setOnClickListener {
            var intent: Intent = Intent(this, GoodsReceiptActivity::class.java)
            startActivity(intent)
        }
        homeBinding.coreInventoryTransfer.setOnClickListener {
            var intent: Intent = Intent(this, InventoryTransferActivity::class.java)
            startActivity(intent)
        }

        homeBinding.saleToDeliveryCard.setOnClickListener {
            var intent: Intent = Intent(this, DeliveryListActivity::class.java)
            startActivity(intent)
        }*/
    }

    private fun setHomeItemAdapter(items: List<HomeItem>) {
        val spanCount =
            if (Prefs.getInt(AppConstants.ITEM_IN_ROW) == 0) 2 else Prefs.getInt(AppConstants.ITEM_IN_ROW)
        homeBinding.rvHomeItems.apply {
            layoutManager =
                GridLayoutManager(this@HomeActivity, spanCount, GridLayoutManager.VERTICAL, false)
            val homeAdapter = HomeAdapter(items) { clickedId ->
                handleItemClick(clickedId)
            }
            adapter = homeAdapter
        }
    }

    private fun handleItemClick(clickedId: String) {
        when (clickedId) {
            AppConstants.PRODUCTION_BATCH_CARD -> {
                startActivity(Intent(this@HomeActivity, PoBatchCardActivity::class.java))
            }

            AppConstants.ISSUE_FOR_PRODUCTION -> {
                flag = "Issue_Order"
                var intent: Intent = Intent(this@HomeActivity, ProductionListActivity::class.java)
                intent.putExtra("flag", flag)
                startActivity(intent)
            }

            AppConstants.SCAN_AND_VIEW -> {
                var intent: Intent = Intent(this, ScanQRViewActivity::class.java)
                startActivity(intent)
            }

            AppConstants.INVENTORY_REQ -> {
                var intent: Intent = Intent(this, InventoryOrderActivity::class.java)
                // intent.putExtra("flag",flag)
                startActivity(intent)
            }

            AppConstants.GOODS_ISSUE -> {
                var intent: Intent = Intent(this, GoodsOrderActivity::class.java)
                startActivity(intent)
            }

            AppConstants.INVENTORY_TRANSFER_GRPO -> {
                var intent: Intent = Intent(this, InventoryOrderActivity_ITR_GRPO::class.java)
                startActivity(intent)
            }

            AppConstants.GOODS_RECEIPT_PO -> {
                var intent: Intent = Intent(this, PurchaseOrderActivity::class.java)
                startActivity(intent)
            }

            AppConstants.SALE_TO_INVOICE -> {
                flag = AppConstants.SALE_TO_INVOICE
                var intent: Intent = Intent(this, SaleToInvoiceActivity::class.java)
                intent.putExtra("flag", flag)
                startActivity(intent)
            }

            AppConstants.RECEIPT_FROM_PRODUCTION -> {
                var intent: Intent = Intent(this, RFPActivity::class.java)
                startActivity(intent)
            }

            AppConstants.PICK_LIST -> {
                var intent = Intent(this, PickListActivity::class.java)
                startActivity(intent)
            }

            AppConstants.RETURN_COMPONENTS -> {
                flag = "Issue_Order"
                var intent = Intent(this@HomeActivity, ReturnComponentListActivity::class.java)
                intent.putExtra("flag", flag)
                startActivity(intent)
            }

            AppConstants.GOODS_RECEIPT -> {
                var intent: Intent = Intent(this, GoodsReceiptActivity::class.java)
                startActivity(intent)
            }

            AppConstants.INVENTORY_TRANSFER_STANDALONE -> {
                var intent: Intent = Intent(this, InventoryTransferActivity::class.java)
                startActivity(intent)
            }

            AppConstants.SALE_TO_DELIVERY -> {
                flag = AppConstants.SALE_TO_DELIVERY
                var intent: Intent = Intent(this, DeliveryListActivity::class.java)
                intent.putExtra("flag", flag)
                startActivity(intent)
            }

        }
    }

    //todo set search icon on activity...
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.logout -> {
                //todo Handle icon click
                dialog()
                return true
            }

            R.id.settings -> {
                //chooseScannerPopupDialog()
                startActivity(Intent(this, SettingActivity::class.java))
                finish()

                return true
            }

            else -> return super.onOptionsItemSelected(item)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.logout_menu, menu)
        val item = menu.findItem(R.id.logout)
        item.setShowAsAction(MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW or MenuItem.SHOW_AS_ACTION_IF_ROOM)

        return true
    }

    fun dialog() {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(true)
        dialog.setContentView(R.layout.logout_layout)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        // Set full width
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        val cancel = dialog.findViewById<Button>(R.id.cancelbtn)
        val logoutbtn = dialog.findViewById<Button>(R.id.logoutbtn)

        cancel.setOnClickListener {
            dialog.dismiss()
        }

        logoutbtn.setOnClickListener {
            logoutApiHit()
        }

        dialog.show()
    }


    //todo issue for production api calling...
    /*fun logoutApiHit() {
        checkNetwoorkConnection.observe(this) { isConnected ->
            if (isConnected) {
                if (Prefs.getString(AppConstants.AppIP, "").equals("")) {
                    var i: Intent = Intent(this@HomeActivity, LoginActivity::class.java)
                    startActivity(i)
                } else {
                    materialProgressDialog.show()
                    var apiConfig = ApiConstantForURL()

                    NetworkClients.updateBaseUrlFromConfig(apiConfig)

                    QuantityNetworkClient.updateBaseUrlFromConfig(apiConfig)

                    val networkClient = NetworkClients.create(this)
                    networkClient.doGetLogoutCall("B1SESSION=" + sessionManagement.getSessionId(this))
                        .apply {
                            enqueue(object : Callback<LoginResponseModel> {
                                override fun onResponse(
                                    call: Call<LoginResponseModel>,
                                    response: Response<LoginResponseModel>
                                ) {
                                    try {
                                        var i = Intent(this@HomeActivity, LoginActivity::class.java)
                                        startActivity(i)
                                        Prefs.putString(AppConstants.BPLID, "")
                                        *//*Prefs.putString(AppConstants.DBUrl, "")
                                        Prefs.putString(AppConstants.AppIP, "")*//*
                                        sessionManagement.setSessionId(this@HomeActivity, "")
                                        sessionManagement.setSessionTimeout(this@HomeActivity, "")

                                        //Prefs.clear()

                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                }

                                override fun onFailure(
                                    call: Call<LoginResponseModel>,
                                    t: Throwable
                                ) {
                                    Log.e("issueCard_failure-----", t.toString())
                                    Toasty.error(this@HomeActivity, t.toString())
                                    materialProgressDialog.dismiss()
                                    var i: Intent =
                                        Intent(this@HomeActivity, LoginActivity::class.java)
                                    startActivity(i)
                                }

                            })
                        }
                }

                finish()

            } else {
                materialProgressDialog.dismiss()
                Toast.makeText(this@HomeActivity, "No Network Connection", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }*/

    fun logoutApiHit() {
        checkNetwoorkConnection.observe(this) { isConnected ->
            if (isConnected) {
                if (isLoggingOut) return@observe
                isLoggingOut = true

                if (Prefs.getString(AppConstants.AppIP, "").isEmpty()) {
                    startLoginActivity()
                } else {
                    materialProgressDialog.show()

                    val apiConfig = ApiConstantForURL()
                    NetworkClients.updateBaseUrlFromConfig(apiConfig)
                    QuantityNetworkClient.updateBaseUrlFromConfig(apiConfig)

                    val networkClient = NetworkClients.create(this)
                    networkClient.doGetLogoutCall("B1SESSION=" + sessionManagement.getSessionId(this))
                        .enqueue(object : Callback<LoginResponseModel> {
                            override fun onResponse(
                                call: Call<LoginResponseModel>,
                                response: Response<LoginResponseModel>
                            ) {
                                try {
                                    clearSessionAndNavigate()
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                    isLoggingOut = false
                                }
                            }

                            override fun onFailure(call: Call<LoginResponseModel>, t: Throwable) {
                                Log.e("Logout_failure", t.toString())
                                Toasty.error(this@HomeActivity, t.toString())
                                materialProgressDialog.dismiss()
                                clearSessionAndNavigate()
                            }
                        })
                }
            } else {
                materialProgressDialog.dismiss()
                Toast.makeText(this@HomeActivity, "No Network Connection", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

    private fun clearSessionAndNavigate() {
        Prefs.putString(AppConstants.BPLID, "")
        sessionManagement.setSessionId(this, "")
        sessionManagement.setSessionTimeout(this, "")
        startLoginActivity()
    }

    private fun startLoginActivity() {
        if (!isFinishing) {
            val intent = Intent(this@HomeActivity, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }

    //todo choose scanner type..
    @SuppressLint("MissingInflatedId")
    private fun chooseScannerPopupDialog() {
        val builder = AlertDialog.Builder(this, R.style.CustomAlertDialog).create()
        val view = LayoutInflater.from(this).inflate(R.layout.scanner_custom_alert, null)
        builder.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        builder.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        builder.window?.setGravity(Gravity.CENTER)
        builder.setView(view)

        //todo set ui text ...
        val radioGroup = view.findViewById<RadioGroup>(R.id.radio_group)
        val radioLaser = view.findViewById<RadioButton>(R.id.radioLaser)
        val radioQrScanner = view.findViewById<RadioButton>(R.id.radioQrScanner)
        val goBtn = view.findViewById<AppCompatButton>(R.id.goBtn)

        //todo get radio buttons selected id..
        var checkGender = ""

        radioGroup?.setOnCheckedChangeListener(RadioGroup.OnCheckedChangeListener { group, checkedId ->
            var radioButton = group.findViewById<RadioButton>(checkedId)
            checkGender = radioButton.text.toString()
            when (checkedId) {
                R.id.radioLaser -> {
                    radioLaser.isChecked = true
                }

                R.id.radioQrScanner -> {
                    radioQrScanner.isChecked = true
                }
            }
            /*  if (radioButton != null && checkedId != -1) {
                  Toast.makeText(this, radioButton.text, Toast.LENGTH_SHORT).show()
              } else {
                  return@OnCheckedChangeListener
              }*/
        })

        //todo validation for toggle..
        if (sessionManagement.getScannerType(this) == "LEASER") {
            radioLaser.isChecked = true
        } else if (sessionManagement.getScannerType(this) == "QR_SCANNER") {
            radioQrScanner.isChecked = true
        }

        //todo go btn..
        goBtn?.setOnClickListener {
            if (checkGender.equals("L")) {
//                sessionManagement.setLaser(1)
//                sessionManagement.setQRScanner(0)
                sessionManagement.setScannerType(this, "LEASER")
            } else if (checkGender.equals("S")) {
//                sessionManagement.setLaser(0)
//                sessionManagement.setQRScanner(1)
                sessionManagement.setScannerType(this, "QR_SCANNER")
            }
            builder.dismiss()
        }

        builder.setCancelable(true)
        builder.show()

    }

}