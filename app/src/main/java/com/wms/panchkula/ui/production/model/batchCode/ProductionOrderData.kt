package com.wms.panchkula.ui.production.model.batchCode

data class ProductionOrderData(
    val stages: List<Stage>
) {
    data class Stage(
        val stageName: String,
        val stageStatus: String,
        val openQty: String,
        val rmItems: List<RmItem>
    ) {
        data class RmItem(
            val itemCode: String,
            val itemDesc: String,
            val itemType: String,
            val openQty: String,
            val scannedBatches: List<ScannedBatch>
        ) {
            data class ScannedBatch(
                val itemCode: String,
                val batchNumber: String
            )
        }
    }
}
