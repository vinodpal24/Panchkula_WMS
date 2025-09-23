package com.wms.panchkula.ui.inventoryOrder.Model

data class DefaultToBinLocationModel(
    var value: List<Value> = listOf()
) {
    data class Value(
        var AbsEntry: String? = "",
        var BinCode: String? = "",
        var WhsCode: String? = ""
    )
}