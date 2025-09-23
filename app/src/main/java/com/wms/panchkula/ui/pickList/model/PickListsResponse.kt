package com.wms.panchkula.ui.pickList.model


data class PickListsResponse(
    var Absoluteentry: Int = 0,
    var Name: String = "",
    var ObjectType: String = "",
    var OwnerCode: Int = 0,
    var OwnerName: Any = Any(),
    var PickDate: String = "",
    var PickListsLines: List<PickListsLine> = listOf(),
    var Remarks: String = "",
    var Status: String = "",
    var UseBaseUnits: String = ""
) {
    data class PickListsLine(
        var AbsoluteEntry: Int = 0,
        var BaseObjectType: Int = 0,
        var BatchNumbers: List<Any> = listOf(),
        var DocumentLinesBinAllocations: List<Any> = listOf(),
        var LineNumber: Int = 0,
        var OrderEntry: Int = 0,
        var OrderRowID: Int = 0,
        var PickStatus: String = "",
        var PickedQuantity: Double = 0.0,
        var PreviouslyReleasedQuantity: Double = 0.0,
        var ReleasedQuantity: Double = 0.0,
        var SerialNumbers: List<Any> = listOf()
    )
}