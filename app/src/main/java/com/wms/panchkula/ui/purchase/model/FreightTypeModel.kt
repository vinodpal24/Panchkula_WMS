package com.wms.panchkula.ui.purchase.model

import java.io.Serializable

data class FreightTypeModel(
    var value: List<Value> = listOf()
) {
    data class Value(
        var Fcode: Int? = 0,
        var Fname: String? = ""
    ) : Serializable
}