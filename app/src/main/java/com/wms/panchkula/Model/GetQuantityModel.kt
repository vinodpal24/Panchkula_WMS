package com.wms.panchkula.Model

class GetQuantityModel (
    var value: List<Value>,
){
    data class Value(
        var Quantity: String,
        var Warehouse: String="",
    )

}