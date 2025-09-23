package com.wms.panchkula.ui.inventoryTransfer.model

import com.google.gson.annotations.SerializedName

data class Warehouse_BPLID(
    @SerializedName("odata.metadata") val odataMetadata: String,
    val value: List<Value>
    ){
    data class Value (
        val BPLID     : String,
        val BinManaged: String,
        val Series    : String

    )
}
