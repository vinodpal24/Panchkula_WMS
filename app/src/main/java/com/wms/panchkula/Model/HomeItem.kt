package com.wms.panchkula.Model

data class HomeItem(
    val imageResId: Int,
    val module: String,
    val clickId: String, // Use this to differentiate click actions
    val status: String = "N"
)
