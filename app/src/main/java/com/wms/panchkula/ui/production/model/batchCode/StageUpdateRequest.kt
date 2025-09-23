package com.wms.panchkula.ui.production.model.batchCode

import java.io.Serializable

data class StageUpdateRequest(
    val ProductionOrdersStages: MutableList<ProductionOrderStage> = arrayListOf()
) : Serializable {
    data class ProductionOrderStage(
        var StageID: Int = 0,
        var U_AQty: Double = 0.0,
        var U_RQty: Double = 0.0
    ) : Serializable
}
