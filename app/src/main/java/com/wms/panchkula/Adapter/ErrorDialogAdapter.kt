package com.wms.panchkula.Adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.wms.panchkula.Model.ErrorItemDetails
import com.wms.panchkula.databinding.RvErrorDialogItemBinding

class ErrorDialogAdapter(
    private val context: Context,
    private val items: ArrayList<ErrorItemDetails>
) : RecyclerView.Adapter<ErrorDialogAdapter.HomeViewHolder>() {

    inner class HomeViewHolder(val binding: RvErrorDialogItemBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HomeViewHolder {
        val binding = RvErrorDialogItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return HomeViewHolder(binding)
    }

    override fun onBindViewHolder(holder: HomeViewHolder, position: Int) {
        val item = items[position]
        holder.binding.tvSrNo.text = (position+1).toString()
        holder.binding.tvItemCode.text = item.itemCode
        holder.binding.tvBatch.text = item.batchNo
        holder.binding.tvItemDesc.text = item.itemDesc

    }

    override fun getItemCount() = items.size
}