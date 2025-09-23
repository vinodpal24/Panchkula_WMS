package com.wms.panchkula.ui.issueForProductionOrder.Model

import com.google.gson.annotations.SerializedName

data class WarehouseBPL_IDModel(
    @SerializedName("odata.metadata") val odataMetadata: String,
    val value: List<Value>
    ){
    data class Value (
        val WarehouseCode: String,
        val BusinessPlaceID: Int
    )
}
