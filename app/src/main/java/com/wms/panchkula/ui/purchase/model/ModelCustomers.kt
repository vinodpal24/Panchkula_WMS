package com.wms.panchkula.ui.purchase.model


import com.google.gson.annotations.SerializedName

data class ModelCustomers(
    @SerializedName("value")
    var value: List<Value> = arrayListOf()
) {
    data class Value(
        @SerializedName("CardCode")
        var cardCode: String? = "",
        @SerializedName("CardName")
        var cardName: String? = ""
    )
}