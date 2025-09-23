package com.wms.panchkula.ui.setting.model


import com.google.gson.annotations.SerializedName

data class ModelGetBranch(
    @SerializedName("value")
    var value: List<Value>? = listOf()
) {
    data class Value(
        @SerializedName("BPLID")
        var bPLID: String? = "",
        @SerializedName("BPLName")
        var bPLName: String? = ""
    )
}