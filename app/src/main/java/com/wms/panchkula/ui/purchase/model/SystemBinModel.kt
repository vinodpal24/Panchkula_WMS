package com.wms.panchkula.ui.purchase.model

data class SystemBinModel(
    var value: List<Value> = listOf()
) {
    data class Value(
        var AbsEntry: String = "",
        var Code: String = ""
    )
}