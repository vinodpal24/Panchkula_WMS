package com.wms.panchkula.ui.goodsreceipt.model


import com.google.gson.annotations.SerializedName
import com.wms.panchkula.ui.purchase.model.PurchaseRequestModel
import java.io.Serializable
import kotlin.collections.ArrayList

data class GetItemstModel(
    @SerializedName("odata.metadata") var odataMetadata: String,
   // @SerializedName("odata.nextLink") var odataNextLink: String,
    var value: ArrayList<Value>,
    ):Serializable  {
    data class Value(
        var ItemCode: String,
        var ItemName: String,
        var Size: String,
        var AutoManual: String,
        var ItemType: String,
        var Series: String,


        var Quantity : String ,
        var UnitPrice : String ,
        var BatchNo :String,


        var binAllocationJSONs: ArrayList<PurchaseRequestModel.binAllocationJSONs>

    ) : Serializable





}