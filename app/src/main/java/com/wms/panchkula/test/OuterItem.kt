package com.wms.panchkula.test

data class OuterItem(
    var title: String,
    var innerItems: MutableList<InnerItem>
)
