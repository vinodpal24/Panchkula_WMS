package com.wms.panchkula.Model

data class ModelWarehouseLocation(
    var value: ArrayList<Value> = arrayListOf()
) {
    data class Value(
        var Code: String? = "",
        var Name: String? = ""
    )
}