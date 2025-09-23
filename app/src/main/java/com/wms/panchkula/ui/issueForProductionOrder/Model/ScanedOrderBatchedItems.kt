package com.wms.panchkula.ui.issueForProductionOrder.Model

import com.google.gson.annotations.SerializedName

data class ScanedOrderBatchedItems(
    @SerializedName("odata.metadata") var odataMetadata: String,
    var value: ArrayList<Value>
    ){
    data class Value (
        var DocEntry: String = "",
        var ItemCode: String = "",
        var ItemDescription: String = "",
        var Status: String = "",
        var Batch: String = "",
        var NoneVal: String = "",
        var Quantity: String,
        var BatchAttribute1: Any? = null,
        var BatchAttribute2: Any? = null,
        var AdmissionDate: String,
        var ManufacturingDate: Any? = null,
        var ExpirationDate: Any? = null,
        var Details: Any? = null,
        var SystemNumber: Long = 0,
        var SerialNumber: String = "",
        var U_Length: Double = 0.0,
        var U_Width: Double = 0.0,
        var U_GSM: Double = 0.0,
        var U_GW: Double = 0.0,
        var U_NW: Any? = null,
        var U_Type: String = "",
        var U_AGSM: Double = 0.0,
        var U_GType: Any? = null,
        var U_RQ: Double = 0.0,
        var U_PC: Any? = null,
        var U_RG: String = "",


        var ItemName : String = "",
        var UOM : String = "",
        var Min : String = "",
        var Max : String = "",
        var LeadTime : String = "",
        var InStock : String = "",
        var Commited : String = "",
        var Ordered  : String = "",
        var AvailableStock : String = "",
        var WhsCode : String = "",
        var InDate : String = "",
        var Qty : String = "",
        var MfgDate : String = "",
        var ExpiryDate : String = "",
        var Batch_Lot_Serial : String = "",
        var Size: String = ""

    )

}
