package com.wms.panchkula.Global_Classes

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.WindowManager
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.wms.panchkula.BuildConfig
import com.wms.panchkula.R
import com.wms.panchkula.ui.goodsOrder.model.LocalListForGoods
import com.wms.panchkula.ui.goodsreceipt.model.GetItemstModel
import com.wms.panchkula.ui.issueForProductionOrder.Model.ProductionListModel
import com.wms.panchkula.ui.production.model.batchCode.StageUpdateRequest
import com.wms.panchkula.ui.purchase.model.FreightDataModel
import com.wms.panchkula.ui.purchase.model.PurchaseRequestModel

object AppConstants {


    //todo api response fields keys store..o
    const val SESSION_TIMEOUT = "SessionTimeout"
    const val SESSION_ID = "SessionId"
    var FromWhere = "Login"
    var WHAREHOUSE = "Warehouse"
    var WAREHOUSE_INV_REQ = "Warehouse"
    var FROM_WAREHOUSE = "From_Warehouse"
    var TO_WAREHOUSE = "To_Warehouse"
    var SCANNER_CHECK = "scanner_check"
    var LEASER_CHECK = "leaser_check"
    const val isFirstTime = "false"
    var SCANNER_TYPE = "scanner_type"
    var USER_PASSWORD = "Password"
    var USER_NAME = "UserName"
    var USER_MGMT_DATA = "USER_MGMT_DATA"
    var BPLID = "_BPLID"
    var IS_TEST_ENVIRONMENT = "_IS_TEST_ENVIRONMENT"

    var IS_SCAN = false
    var WhsCode = ""
    var BatchNo = ""
    var ItemCode = ""
    var SCANNED_ITEM = "SCANNED_ITEM"
    var GRPO_TOTAL = "_grpo_total"

    var ISSUE_FOR_PRODUCTION = "ISSUE_FOR_PRODUCTION"
    var PRODUCTION_BATCH_CARD = "PRODUCTION_BATCH_CARD"
    var SCAN_AND_VIEW = "SCAN_AND_VIEW"
    var INVENTORY_REQ = "INVENTORY_REQ"
    var GOODS_ISSUE = "GOODS_ISSUE"
    var INVENTORY_TRANSFER_GRPO = "INVENTORY_TRANSFER_GRPO"
    var GOODS_RECEIPT_PO = "GOODS_RECEIPT_PO"
    var SALE_TO_INVOICE = "SALE_TO_INVOICE"
    var RECEIPT_FROM_PRODUCTION = "RECEIPT_FROM_PRODUCTION"
    var PICK_LIST = "PICK_LIST"
    var RETURN_COMPONENTS = "RETURN_COMPONENTS"
    var GOODS_RECEIPT = "GOODS_RECEIPT"
    var INVENTORY_TRANSFER_STANDALONE = "INVENTORY_TRANSFER_STANDALONE"
    var SALE_TO_DELIVERY = "SALE_TO_DELIVERY"
    var isVpnRequired = false // true if vpn require else false
    var isTestEnvUIVisible = BuildConfig.IS_DEVELOPMENT // true if test env ui is visible else false
    var isDevelopmentForClient = BuildConfig.IS_DEVELOPMENT_CLIENT // true if test env ui is visible else false
    var credentialEnabled: Boolean = BuildConfig.IS_DEVELOPMENT // true if credential filled default else blank

    var ITEM_IN_ROW = "ITEM_IN_ROW"

    var binAllocationJSONs = ArrayList<PurchaseRequestModel.binAllocationJSONs>()
    var selectedList = mutableListOf<GetItemstModel.Value>()
    var selectedPOList = mutableListOf<PurchaseRequestModel.Value>()
    var freightDataList = ArrayList<FreightDataModel.DocumentAdditionalExpenses>()
    var stageRequest = StageUpdateRequest()
    var scannedItemForGood = mutableListOf<LocalListForGoods>()

    //  var scannedItemForIssueOrder = mutableListOf<IssueLocalListModel>()
    var scannedItemForIssueOrder = mutableListOf<ProductionListModel.ProductionOrderLine>()

    var isFixedQtyValSet = false


    //todo SQL server credentials..
    const val IP = "192.168.0.154"

    var DBUrl = "_DBURL"//115.247.228.186
    var AppIP = "_APPURL"//115.247.228.186

    //todo Test
    // const val IP = "103.194.8.40"
    const val PORT = "1433"

    //todo Live

//    const val PORT = "65430"

    var COMPANY_DB = "TRIAL"

    const val USERNAME = "B1ADMIN"
    const val PASSWORD: String = "Cinntra#@123"

    //const val PASSWORD: String = "SqLSrvR@190923"
    const val Classes = "net.sourceforge.jtds.jdbc.Driver"


    /*  var BASE_URL = "http://"+ Prefs.getString(AppConstants.DBUrl)+":50001/b1s/v1/"
     var QUANTITY_BASE_URL = "http://"+ Prefs.getString(AppConstants.AppIP)+":8080/api/"

      init {
          Log.e("IpAddre", "apiCall: "+ Prefs.getString(AppConstants.DBUrl))
          Log.e("BASE_URL", "apiCall: "+ BASE_URL)
      }
  */


    public fun showAlertDialogOld(context: Context, batch: String, quantity: String) {

        val builder = AlertDialog.Builder(context)

        // Create a custom title view
        val title = "Suggested Batch: $batch \nAnd Quantity: $quantity"

        // Create the dialog
        builder.setTitle(title)
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }

        // Show the dialog
        val dialog = builder.create()
        dialog.show()

        // Prevent touch outside of dialog to dismiss
        dialog.setCanceledOnTouchOutside(false)

        // Adjust the width and height of the dialog
        val layoutParams = dialog.window?.attributes
        layoutParams?.width = WindowManager.LayoutParams.MATCH_PARENT // Set the width to match parent
        layoutParams?.height = WindowManager.LayoutParams.WRAP_CONTENT // Set the height to wrap content
        dialog.window?.attributes = layoutParams

        // Adjust the title TextView to wrap lines correctly
        val titleTextView = dialog.findViewById<TextView>(android.R.id.title)
        titleTextView?.setMaxLines(5) // Set the max lines to 2 (or more if needed)
        // titleTextView?.ellipsize = null // Disable ellipsize to ensure full content is visible
        // titleTextView?.setText(title)

        // Optional: You can adjust the background of the dialog window here
        // dialog.window?.setBackgroundDrawable(ColorDrawable(context.resources.getColor(R.color.dialog_background, null)))
    }


    @SuppressLint("SetTextI18n")
    fun showAlertDialog(context: Context, title: String, description: String) {
        // Inflate the custom dialog layout
        val inflater = LayoutInflater.from(context)
        val dialogView = inflater.inflate(R.layout.batch_suggestion_dialog, null)

        // Set the title and description dynamically
        val titleTextView = dialogView.findViewById<TextView>(R.id.dialogTitle)
        val descriptionTextView = dialogView.findViewById<TextView>(R.id.dialogDescription)

        titleTextView.text = " Suggested Batch : " + title
        descriptionTextView.text = " Quantity : " + description

        // Build the dialog
        val builder = AlertDialog.Builder(context)
            .setView(dialogView)  // Set the custom view
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }

        // Show the dialog
        val dialog = builder.create()
        dialog.show()

        // Prevent touch outside of dialog to dismiss
        dialog.setCanceledOnTouchOutside(false)
    }

}