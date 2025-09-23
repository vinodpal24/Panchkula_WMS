package com.wms.panchkula.ui.inventoryOrder.Model


import com.google.gson.annotations.SerializedName
import com.wms.panchkula.ui.purchase.model.PurchaseRequestModel
import java.io.Serializable
import kotlin.collections.ArrayList

data class InventoryRequestModel(
    @SerializedName("odata.metadata") var odataMetadata: String,
   // @SerializedName("odata.nextLink") var odataNextLink: String,
    var value: ArrayList<Value>,
    ):Serializable  {
    data class Value(
        var DocEntry: String,
        var BPLID: String,
        var Series: String,
        var CardCode: String,
        var Comments: String,
        var DocDate: String,
        var DocNum: String,
        var DocObjectCode: String,
        var DueDate: String,
        var FinancialPeriod: String,
        var FromWarehouse: String,
        var JournalMemo: String,
        var Reference1: String,
        var ToWarehouse: String,
        var TaxDate: String,
        var ShipToCode: String,
        var U_DOCTYP: String,
        var U_TRNTYP: String,
        var DocType: String,
        var NumAtCard: String,

        var U_Width: Double? = null,
        var U_GSM: Double? = null,
        var U_FLEX: Double? = null,
        var U_TBQ: Double? = null,
        var U_Length: Double? = null,

        var StockTransferLines: ArrayList<StockTransferLines>
      /*  var StockTransfer_ApprovalRequests: ArrayList<Any>,
        var ElectronicProtocols: List<Any?>,
        var DocumentReferences: List<Any?>*/

    ) : Serializable

    data class StockTransferLines(
        var ItemName: String? = null,
        var ItemNo: String? = null,
        var BaseQuantity: Double,
        var PlannedQuantity: Double,
        var IssuedQuantity: Double,
        var isScanned : Int=0,
        var totakPktQty : Int,
        var ItemCode  : String,
        var LineNum  : String,
        var ItemDescription  : String,
        var Quantity  : String,
        var Price  : String,
        var WarehouseCode  : String,
        var FromWarehouseCode  : String,
        var BaseType  : String,
        var BaseLine  : String,
        var Batch: String? = "",
        var Serial: String? = "",
        var None: String? = "",
        var Size: String? = "",
        var BaseEntry  : String,
        var UnitPrice  : String,
        var U_ACT_QTY  : String,
        var U_BOX_QTY  : String,
        var DocEntry  : String,
        var RemainingOpenQuantity  : String,
        var totalOpenDefault: Double=0.0,
        var U_IQTY  : String,
        var NavisionCode  : String? =null,
//        var hashMap: HashMap<String, ArrayList<ScanedOrderBatchedItems.Value>> = HashMap<String, ArrayList<ScanedOrderBatchedItems.Value>>()

        var DefaultBinCD  : String,
        var DefaultABSEntry  : String,
        var BinCode  : String,
        var BinABSEntry  : String,
        var BinManaged  : String,
        var binAllocationJSONs: ArrayList<PurchaseRequestModel.binAllocationJSONs>


        ): Serializable



}