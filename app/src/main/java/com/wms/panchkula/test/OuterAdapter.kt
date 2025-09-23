package com.wms.panchkula.test

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.wms.panchkula.databinding.ItemOuterBinding

class OuterAdapter(private val outerItems: MutableList<OuterItem>) : RecyclerView.Adapter<OuterAdapter.OuterViewHolder>() {

    inner class OuterViewHolder(val binding: ItemOuterBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OuterViewHolder {
        val binding = ItemOuterBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return OuterViewHolder(binding)
    }

    override fun onBindViewHolder(holder: OuterViewHolder, position: Int) {
        val outerItem = outerItems[position]
        holder.binding.tvOuterTitle.text = outerItem.title

        val innerAdapter = InnerAdapter(outerItem.innerItems) { description ->
            outerItem.title = description
            holder.binding.tvOuterTitle.text = description
        }

        holder.binding.rvInnerList.layoutManager = LinearLayoutManager(holder.binding.rvInnerList.context)
        holder.binding.rvInnerList.adapter = innerAdapter
    }

    override fun getItemCount(): Int {
        return outerItems.size
    }
}


