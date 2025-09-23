package com.wms.panchkula.Model

data class DBNameListModel(
    val value: List<Value> = listOf()
) {
    data class Value(
        val DBDESC: String = """TRIAL""",
        val DBName: String = """TRIAL"""
    )
}