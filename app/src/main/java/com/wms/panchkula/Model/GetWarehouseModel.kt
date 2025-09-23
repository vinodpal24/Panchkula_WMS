package com.wms.panchkula.Model

data class GetWarehouseModel(
    var value: List<Value?>? = listOf()
) {
    data class Value(
        var BPLid: String? = "",
        var WareHouseCode: String? = "",
        var WareHouseName: String? = ""
    )
}