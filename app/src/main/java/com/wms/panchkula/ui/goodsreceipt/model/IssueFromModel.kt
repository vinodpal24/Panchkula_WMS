package com.wms.panchkula.ui.goodsreceipt.model


import com.google.gson.annotations.SerializedName
import java.io.Serializable
import kotlin.collections.ArrayList

data class IssueFromModel(
    @SerializedName("odata.metadata") var odataMetadata: String,

    var value: ArrayList<Value>,
) : Serializable {
    data class Value(
        var Quantity: String,
        var Batch: String,
        var BinAbsEntry: String,
        var BinCode: String,
        var EnteredQTY: String,
        var IssueQuantity: Double,
        var SysNumber:String
    ) : Serializable
}