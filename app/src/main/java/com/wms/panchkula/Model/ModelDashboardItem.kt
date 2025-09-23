package com.wms.panchkula.Model


import com.google.gson.annotations.SerializedName

data class ModelDashboardItem(
    @SerializedName("value")
    var value: ArrayList<Value> = arrayListOf()
) {
    data class Value(
        @SerializedName("Module")
        var module: String = "",
        @SerializedName("Status")
        var status: String = ""
    )
}