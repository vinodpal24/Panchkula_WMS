package com.wms.panchkula.Model

class GetAbsModel (
    var value: List<Value>,
){
    data class Value(
        var BinCode: String,
        var Quantity: String,
        var AbsEntry: String

    )

}