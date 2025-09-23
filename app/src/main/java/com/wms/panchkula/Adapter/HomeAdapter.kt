package com.wms.panchkula.Adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.wms.panchkula.databinding.ItemHomeBinding
import com.wms.panchkula.Model.HomeItem

class HomeAdapter(
    private val items: List<HomeItem>,
    private val onItemClick: (clickId: String) -> Unit
) : RecyclerView.Adapter<HomeAdapter.HomeViewHolder>() {

    inner class HomeViewHolder(val binding: ItemHomeBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HomeViewHolder {
        val binding = ItemHomeBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return HomeViewHolder(binding)
    }

    override fun onBindViewHolder(holder: HomeViewHolder, position: Int) {
        val item = items[position]
        holder.binding.ivHomeIcon.setImageResource(item.imageResId)
        holder.binding.tvTitleHome.text = item.module

        holder.binding.root.setOnClickListener {
            onItemClick(item.clickId)
        }
    }

    override fun getItemCount() = items.size
}