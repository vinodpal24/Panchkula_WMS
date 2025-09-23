package com.wms.panchkula.Model

data class ModelSeries(
    var value: List<Value> = listOf()
) {
    data class Value(
        var Indicator: String = "",
        var Series: String = "",
        var SeriesName: String = ""
    )
}