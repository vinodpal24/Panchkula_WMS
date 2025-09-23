package com.wms.panchkula.ui.issueForProductionOrder.Model

import com.google.gson.annotations.SerializedName

data class InventoryGenExitsModel
    (
    @SerializedName("odata.metadata") var odataMetadata: String,

    @SerializedName("odata.etag") var odataEtag: String,
    var DocEntry: Long,
    var DocNum: String,
    var DocType: String,
    var DocDate: String,
    var DocDueDate: String,
    var DocTotal: Double,
    var DocCurrency: String,
    var DocRate: Double,
    var Reference1: String,
    var JournalMemo: String,
    var DocTime: String,
    var Series: Long,
    var CreationDate: String,
    var UpdateDate: String,
    var DocumentStatus: String,
    var Document_ApprovalRequests: List<Any?>,
    var DocumentLines: List<DocumentLine>,
    var ElectronicProtocols: List<Any?>,
    var DocumentReferences: List<Any?>
) {
    data class DocumentLine(
        var LineNum: Long,
        var ItemCode: String,
        var ItemDescription: String,
        var Quantity: Double,
        var ShipDate: Any? = null,
        var Price: Double,
        var Currency: String,
        var Rate: Double,
        var WarehouseCode: String,
        var TreeType: String,
        var AccountCode: String,
        var BarCode: Any? = null,
        var UnitsOfMeasurment: Double,
        var LineTotal: Double,
        var UnitPrice: Double,
        var LineStatus: String,
        var RemainingOpenQuantity: Double,
        var OpenAmount: Double,
        var OpenAmountFC: Double,
        var OpenAmountSC: Double,
        var LineTaxJurisdictions: List<Any?>,
        var SerialNumbers: List<Any?>,
        var BatchNumbers: List<BatchNumber>,
        var CCDNumbers: List<Any?>,
        var DocumentLinesBinAllocations: List<Any?>
    )

    data class BatchNumber (
        var BatchNumber: String,
        var ManufacturerSerialNumber: Any? = null,
        var AddmisionDate: String,
        var Location: Any? = null,
        var Quantity: Double,
        var BaseLineNumber: Long,
        var ItemCode: String,
        var SystemSerialNumber: Long,
    )
}
