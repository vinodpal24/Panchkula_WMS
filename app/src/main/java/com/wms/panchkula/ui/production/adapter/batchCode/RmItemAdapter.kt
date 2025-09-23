package com.wms.panchkula.ui.production.adapter.batchCode

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.wms.panchkula.databinding.RvItemRmBinding
import com.wms.panchkula.ui.production.model.batchCode.ProductionOrderData

class RmItemAdapter(
    private val rmItems: List<ProductionOrderData.Stage.RmItem>,
    private val stagePos: Int,
    private val onRmClick: (stagePos: Int, rmPos: Int, rmItem: ProductionOrderData.Stage.RmItem) -> Unit,
    private val onBatchClick: (stagePos: Int, rmPos: Int, batchPos: Int, batch: ProductionOrderData.Stage.RmItem.ScannedBatch) -> Unit
) : RecyclerView.Adapter<RmItemAdapter.RmViewHolder>() {

    inner class RmViewHolder(val binding: RvItemRmBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RmViewHolder {
        val binding = RvItemRmBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return RmViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RmViewHolder, position: Int) {
        val rmItem = rmItems[position]
        with(holder.binding) {
            tvItemCode.text = rmItem.itemCode
            tvItemDesc.text = rmItem.itemDesc
            tvItemType.text = rmItem.itemType
            tvOpenQtyRm.text = rmItem.openQty

            root.setOnClickListener {
                onRmClick(stagePos, position, rmItem)
            }

            rvBatchItemPo.layoutManager = LinearLayoutManager(root.context)
            rvBatchItemPo.adapter =
                BatchItemsAdapter(rmItem.scannedBatches, stagePos, position, onBatchClick)
        }
    }

    override fun getItemCount(): Int = rmItems.size
}
