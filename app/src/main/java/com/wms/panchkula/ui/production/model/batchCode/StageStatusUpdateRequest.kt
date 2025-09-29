package com.wms.panchkula.ui.production.model.batchCode

import java.io.Serializable

data class StageStatusUpdateRequest(
    val ProductionOrdersStages: MutableList<ProductionOrderStage> = arrayListOf()
) : Serializable {
    data class ProductionOrderStage(
        var StageID: Int = 0,
        var U_Status: String ="No"
    ) : Serializable
}
