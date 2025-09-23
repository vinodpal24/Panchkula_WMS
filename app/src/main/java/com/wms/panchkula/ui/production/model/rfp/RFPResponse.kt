package com.wms.panchkula.ui.production.model.rfp

import com.google.gson.annotations.SerializedName

import java.io.Serializable

data class RFPResponse(
    @SerializedName("odata.metadata") var odataMetadata: String,

    var value: ArrayList<Value>,

    ): Serializable {
    data class Value(

        val Remarks: String,
        val ItemNo: String,
        val DocumentNumber: String,
        val FGName: String,

        val AbsoluteEntry: String,
        val WarehouseCode: String,
        val Series: String,
        val PostingDate: String,
        val DefaultBinCD: String,
        val DefaultABSEntry: String,
        val DocTotal: String,
        val DocType: String,
        var RemainingOpenQuantity: String,
        val BinManaged: String,
        val ItemType: String,
        val ItemName:  String,
        val Quantity: String,
        val BinCode: String,
        val BinABSEntry: String,
        val BPLID: String,



        var isScanned : Int=0,
        var totakPktQty : Int,
        var PlannedQuantity: Double=0.0,
        var IssuedQuantity: Double=0.0,

        var Batch: String? = "",
        var Serial: String? = "",
        var None: String? = "",
        var NumAtCardTemp: String? = "",

        var binAllocationJSONs : ArrayList<binAllocationJSONs>,
        val serialManual       : ArrayList<serialManual>,
        val manualBatch        : ArrayList<manualBatch>


    ):Serializable

    data class binAllocationJSONs(
        val BinLocation: String,
        val BinAbsEntry: String,
        val Quantity: String,
        val WarehouseCode: String,
        val ToBinAbsEntry: String,
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
}