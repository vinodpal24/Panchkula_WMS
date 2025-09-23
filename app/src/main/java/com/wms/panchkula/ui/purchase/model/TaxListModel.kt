package com.wms.panchkula.ui.purchase.model

import java.io.Serializable

data class TaxListModel(
    var value: List<Value> = listOf()
) {
    data class Value(
        var Freight: String? = "",
        var Rate: String? = "",
        var Tcode: String? = "",
        var Tname: String? = ""
    ) : Serializable
}