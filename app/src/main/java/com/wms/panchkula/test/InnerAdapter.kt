package com.wms.panchkula.test

import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.wms.panchkula.databinding.ItemInnerBinding

class InnerAdapter(
    private val innerItems: MutableList<InnerItem>,
    private val onTextChanged: (String) -> Unit
) : RecyclerView.Adapter<InnerAdapter.InnerViewHolder>() {

    inner class InnerViewHolder(val binding: ItemInnerBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InnerViewHolder {
        val binding = ItemInnerBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return InnerViewHolder(binding)
    }

    override fun onBindViewHolder(holder: InnerViewHolder, position: Int) {
        val innerItem = innerItems[position]
        holder.binding.etInnerDescription.setText(innerItem.description)

        // Remove existing TextWatcher if any
     //   holder.binding.etInnerDescription.addTextChangedListener(null)

        holder.binding.etInnerDescription.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                innerItem.description = s.toString()
                onTextChanged(s.toString())
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    override fun getItemCount(): Int {
        return innerItems.size
    }
}


