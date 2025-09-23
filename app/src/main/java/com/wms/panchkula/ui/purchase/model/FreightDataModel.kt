package com.wms.panchkula.ui.purchase.model

import java.io.Serializable

data class FreightDataModel(
    var value: List<DocumentAdditionalExpenses> = listOf()
) {
    data class DocumentAdditionalExpenses(
        var ExpenseCode: Int? = 0,
        var LineTotal: Double? = 0.0,
        var TaxLiable: String? = "tYES",
        var TaxCode: String? = "",
        var TaxSum: Double? = 0.0,
        var LineGross: Double? = 0.0,
        var DistributionMethod: String? = "aedm_None"
    ) : Serializable
}