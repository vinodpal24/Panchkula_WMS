package com.wms.panchkula.ui.goodsreceipt.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.wms.panchkula.databinding.RvItemPrimaryBinding
import com.wms.panchkula.ui.goodsreceipt.model.GetItemstModel
import kotlin.collections.ArrayList

class SearchAdapter(var list: ArrayList<GetItemstModel.Value>) :
    RecyclerView.Adapter<SearchAdapter.ViewHolder>() {

    //TODO comment interface declare...
    private var onItemClickListener: ((List<GetItemstModel.Value>, pos: Int) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = RvItemPrimaryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        with(holder) {
            with(list[position]) {

                binding.layoutTop.visibility = View.GONE
                binding.tvFgItem.text = this.ItemName
                binding.tvFgItemLabel.text = "Item Name"
                binding.layoutFgName.visibility = View.VISIBLE
                //TODO comment interface...
                binding.cvListItem.setOnClickListener {
                    onItemClickListener?.let { click ->
                        click(list, position)
                    }
                }
            }
        }
    }

    override fun getItemCount(): Int {
        return list.size
    }


    class ViewHolder(val binding: RvItemPrimaryBinding) :
        RecyclerView.ViewHolder(binding.root)

    fun OnItemClickListener(listener: (List<GetItemstModel.Value>, pos: Int) -> Unit) {
        onItemClickListener = listener
    }

    //todo filter search list call this function whenever the search query changes and list update..

    fun setFilteredItems(filteredItems: ArrayList<GetItemstModel.Value>) {
        list = filteredItems
        notifyDataSetChanged()
    }

    fun clearItems() {
        list.clear()
        Log.e("Clear==>", "" + list.size)
        notifyDataSetChanged()
    }

}