package com.wms.panchkula.ui.deliveryOrderModule.Adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.wms.panchkula.databinding.DeliveryOrderListAdapterLayoutBinding
import com.wms.panchkula.ui.deliveryOrderModule.Model.DeliveryModel

class DeliveryListAdapter(var deliveryModelList: List<DeliveryModel.Value>, private val itemClickListener: ItemClickListener): RecyclerView.Adapter<DeliveryListAdapter.ViewHolder>() {

    //TODO comment interface declare...
    interface ItemClickListener {
        fun onItemClick(list: List<DeliveryModel.Value>,position: Int)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = DeliveryOrderListAdapterLayoutBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        with(holder){
            with(deliveryModelList[position]){
                binding.tvDocNo.text = this.DocNum
                binding.tvCardCode.text = this.CardCode

                //TODO comment interface...
                binding.cardItemView.setOnClickListener {
                    itemClickListener.onItemClick(deliveryModelList, position)
                }
            }
        }
    }

    override fun getItemCount(): Int {
        return deliveryModelList.size
    }

    class ViewHolder(val binding: DeliveryOrderListAdapterLayoutBinding) : RecyclerView.ViewHolder(binding.root)

    //todo filter search list call this function whenever the search query changes and list update..

    fun setFilteredItems(filteredItems: ArrayList<DeliveryModel.Value>) {
        deliveryModelList = filteredItems
        notifyDataSetChanged()
    }

}