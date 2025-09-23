package com.wms.panchkula.ui.production.adapter.batchCode

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.wms.panchkula.databinding.ScannedBatchItemLayoutBinding
import com.wms.panchkula.ui.production.model.batchCode.ProductionOrderData

class BatchItemsAdapter(
    private val batches: List<ProductionOrderData.Stage.RmItem.ScannedBatch>,
    private val stagePos: Int,
    private val rmPos: Int,
    private val onBatchClick: (stagePos: Int, rmPos: Int, batchPos: Int, batch: ProductionOrderData.Stage.RmItem.ScannedBatch) -> Unit
) : RecyclerView.Adapter<BatchItemsAdapter.BatchViewHolder>() {

    inner class BatchViewHolder(val binding: ScannedBatchItemLayoutBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BatchViewHolder {
        val binding = ScannedBatchItemLayoutBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return BatchViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BatchViewHolder, position: Int) {
        val batch = batches[position]
        holder.binding.tvItemCode.text = batch.itemCode
        holder.binding.tvBatchNumber.text = batch.batchNumber

        holder.binding.root.setOnClickListener {
            onBatchClick(stagePos, rmPos, position, batch)
        }
    }

    override fun getItemCount(): Int = batches.size
}
