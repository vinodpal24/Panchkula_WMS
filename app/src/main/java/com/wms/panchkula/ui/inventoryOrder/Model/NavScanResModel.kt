package com.wms.panchkula.ui.inventoryOrder.Model

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class NavScanResModel(
    @SerializedName("odata.metadata") var odataMetadata: String,
    // @SerializedName("odata.nextLink") var odataNextLink: String,
    var value: ArrayList<Value>,
):Serializable  {
    data class Value(
        var ItemCode: String,
        var ItemName: String,
        var U_PACK_QTY: String,
        var U_NAVI_CODE: String

    ) : Serializable


}