package com.wms.panchkula.ui.returnComponents.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.wms.panchkula.Global_Classes.GlobalMethods.setTextStyleAndColor
import com.wms.panchkula.Global_Classes.GlobalMethods.updateItemType
import com.wms.panchkula.R
import com.wms.panchkula.ui.issueForProductionOrder.Model.ProductionListModel
import com.wms.panchkula.databinding.RvItemProductionOderlinesBinding
import kotlin.collections.ArrayList

class ReturnComponentsLinesAdapter(
    private val context: Context,
    var list: ArrayList<ProductionListModel.ProductionOrderLine>,
    private val onReturnItemClicked: (Int, ProductionListModel.ProductionOrderLine, tvReturnQty: TextView) -> Unit
) : RecyclerView.Adapter<ReturnComponentsLinesAdapter.ViewHolder>() {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = RvItemProductionOderlinesBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        list[position].let { holder.bindData(it) }

    }

    override fun getItemCount(): Int {
        return list.size
    }

    inner class ViewHolder(val binding: RvItemProductionOderlinesBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bindData(currentItem: ProductionListModel.ProductionOrderLine) {
            binding.apply {
                tvItemCode.text = currentItem.ItemNo
                tvItemName.text = currentItem.ItemName
                tvIssuedQty.text = currentItem.IssueQuantity.toString()

                val returnQty = if (!currentItem.binAllocationJSONs.isNullOrEmpty()) {
                    currentItem.binAllocationJSONs
                        ?.sumOf { it.Quantity?.toDoubleOrNull() ?: 0.0 } ?: 0.0
                } else {
                    0.0
                }

                tvReturnQty.text = returnQty.toString()

                if (returnQty > 0.0) {
                    mainLayout.background = ContextCompat.getDrawable(context, R.drawable.bg_docline_item_selected_dark)
                    setTextStyleAndColor(tvItemName, ContextCompat.getColor(context, R.color.black))
                    setTextStyleAndColor(tvReturnQty, ContextCompat.getColor(context, R.color.black), isBold = true)
                    setTextStyleAndColor(tvItemCode, ContextCompat.getColor(context, R.color.black))
                    setTextStyleAndColor(tvIssuedQty, ContextCompat.getColor(context, R.color.black))
                    setTextStyleAndColor(tvItemType, ContextCompat.getColor(context, R.color.black))
                } else {
                    mainLayout.background = ContextCompat.getDrawable(context, R.drawable.bg_docline_item)
                    setTextStyleAndColor(tvItemName, ContextCompat.getColor(context, R.color.grey))
                    setTextStyleAndColor(tvReturnQty, ContextCompat.getColor(context, R.color.grey), isBold = true)
                    setTextStyleAndColor(tvItemCode, ContextCompat.getColor(context, R.color.grey))
                    setTextStyleAndColor(tvIssuedQty, ContextCompat.getColor(context, R.color.grey))
                    setTextStyleAndColor(tvItemType, ContextCompat.getColor(context, R.color.grey))
                }


                updateItemType(currentItem.Batch.toString(), currentItem.Serial.toString(), currentItem.None.toString(), binding.tvItemType)

                itemView.setOnClickListener {
                    onReturnItemClicked(adapterPosition, currentItem, tvReturnQty)
                }
            }
        }
    }

    fun updateReturnQty(position: Int, newQty: Double) {
        list[position].binAllocationJSONs?.forEach { bin ->
            bin.Quantity = newQty.toString()
        }
        notifyItemChanged(position)
    }

}