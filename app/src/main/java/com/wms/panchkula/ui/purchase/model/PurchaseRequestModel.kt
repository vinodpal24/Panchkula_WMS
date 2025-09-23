package com.wms.panchkula.ui.purchase.model


import com.google.gson.annotations.SerializedName
import java.io.Serializable
import kotlin.collections.ArrayList

data class PurchaseRequestModel(
    @SerializedName("odata.metadata") var odataMetadata: String,
    // @SerializedName("odata.nextLink") var odataNextLink: String,
    var value: ArrayList<PurchaseRequestModel.Value>,
) : Serializable {
    data class Value(
        val DocEntry: String,
        val DocNum: String,
        val AttachmentEntry: String,
        val CurrencyBP: String,
        val GroupCode: String,
        val DocDate: String,
        val TaxDate: String,
        val DocDueDate: String, // Changed from DueDate to DocDueDate to match the JSON key
        val CardCode: String,
        val CardName: String, // Added CardName from the JSON
        val NumAtCard: String,
        val DocRate: String,
        val DocTotal: Double, // Changed to Double to handle numerical values
        val Comments: String,
        val JournalMemo: String,
        val Series: String,
        val SeriesDel: String,
        val BPLName: String,
        val BPLID: String,
        val PayToCode: String,
        val ShipToCode: String,
        val Cancelled: String,
        val UserSign: String,
        val DocType: String,
        var isItemSelected: Boolean = false,
        val DocumentLines: ArrayList<StockTransferLines> // This corresponds to the DocumentLines field in the JSON
    ) : Serializable

    data class StockTransferLines(
        val ItemCode: String,
        val LineNum: String,
        val ItemDescription: String,
        val MeasureUnit: String,
        val Price: String, //Double Changed to Double to handle numerical values
        val Quantity: String, //Int Changed to Int for quantities
        val DiscountPercent: String, //Double Changed to Double for percentages
        var WarehouseCode: String,
        val Currency: String,
        val Rate: String,   //Double
        val LineTotal: String, // Double Added LineTotal to match the JSON
        val DocEntry: String,
        val HSNEntry: String? = null,
        val Size: String? = null,
        val ItemType: String,
        val ScanType: String,
        val BinManaged: String,
        var RateField: String,
        val DefaultBinCD: String,
        val DefaultABSEntry: String,
        val BinCode: String,
        val BinABSEntry: String,
        val PaymentMethod: String,
        val PayToCode: String,
        val ShipToCode: String,
        var TaxCode: String,
        val selectedGrpoQty: Double,
        var grpoGrandTotal: Double,

        var isScanned: Int = 0,
        var totakPktQty: Int,
        var PlannedQuantity: Double = 0.0,
        var IssuedQuantity: Double = 0.0,
        var RemainingOpenQuantity: String,
        var Batch: String? = "",
        var Serial: String? = "",
        var None: String? = "",
        var NumAtCardTemp: String? = "",
        var totalOpenDefault: Double = 0.0,

        var binAllocationJSONs: ArrayList<binAllocationJSONs>,
        var binAllocationJSONsNew: ArrayList<binAllocationJSONsNew>,
        val serialManual: ArrayList<serialManual>,
        val manualBatch: ArrayList<manualBatch>
    ) : Serializable

    data class binAllocationJSONs(
        val BinLocation: String,
        val BinAbsEntry: String,
        val BatchNum: String,
        var Quantity: String,
        val WarehouseCode: String,
        val ToBinAbsEntry: String,

        var ManufacturerSerialNumber: String = "",
        var InternalSerialNumber: String = "",
        var ExpiryDate: String = "",
        var ManufacturingDate: String = ""
    ) : Serializable

    data class serialManual(
        val SerialNum: String,
        val Quantity: String
    ) : Serializable

    data class manualBatch(
        val BatchNum: String,
        val Quantity: String
    ) : Serializable

    data class binAllocationJSONsNew(
        val BinLocation: String,
        val BinAbsEntry: String,
        val BatchNum: String,
        val Quantity: String,
        val WarehouseCode: String,
        val ToBinAbsEntry: String,


        var ManufacturerSerialNumber: String = "",
        var InternalSerialNumber: String = "",
        var ExpiryDate: String = "",
        var ManufacturingDate: String = ""

    ) : Serializable
}
