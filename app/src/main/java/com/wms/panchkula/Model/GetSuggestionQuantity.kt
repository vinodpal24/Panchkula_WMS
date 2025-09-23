package com.wms.panchkula.Model

class GetSuggestionQuantity (
    var value: List<Value>,
){
    data class Value(
        var Quantity: String,
        var Batch: String="",
    )

}