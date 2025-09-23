package com.wms.panchkula.ui.goodsOrder.autocomplete
import android.content.Context
import android.widget.ArrayAdapter
import com.wms.panchkula.Model.GetQuantityModel

class WareHouseGoodAutoCompleteAdapter(context: Context, private val listings: List<GetQuantityModel.Value>) : ArrayAdapter<GetQuantityModel.Value>(context, android.R.layout.simple_dropdown_item_1line, listings) {

    override fun getItem(position: Int): GetQuantityModel.Value? {
        return listings[position]
    }

    override fun getView(position: Int, convertView: android.view.View?, parent: android.view.ViewGroup): android.view.View {
        val view = super.getView(position, convertView, parent)
        val textView = view.findViewById<android.widget.TextView>(android.R.id.text1)
        textView.text = listings[position].Warehouse + "      ( " + listings[position].Quantity +")"// Customize this to show what you want
        return view
    }
}
