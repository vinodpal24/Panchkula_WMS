package com.wms.panchkula.ui.production.adapter.rfp

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.wms.panchkula.Global_Classes.GlobalMethods
import com.wms.panchkula.databinding.RvItemPrimaryBinding
import com.wms.panchkula.ui.production.model.rfp.RFPResponse
import kotlin.collections.ArrayList

class RFPAdapter(var list: ArrayList<RFPResponse.Value>) :
    RecyclerView.Adapter<RFPAdapter.ViewHolder>() {

    //TODO comment interface declare...
    private var onItemClickListener: ((List<RFPResponse.Value>, pos: Int) -> Unit)? = null

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
                binding.tvDocNum.text = this.DocumentNumber
                binding.tvFgItem.text = this.FGName
                binding.layoutFgName.visibility= View.VISIBLE
                //binding.docNum.text = "Doc Entry  : "
                //binding.tvProd.text = this.AbsoluteEntry

                binding.tvDocDate.text =
                    GlobalMethods.convert_yyyy_MM_dd_T_hh_mm_ss_into_ddMMYYYY(this.PostingDate)

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

    fun OnItemClickListener(listener: (List<RFPResponse.Value>, pos: Int) -> Unit) {
        onItemClickListener = listener
    }

    //todo filter search list call this function whenever the search query changes and list update..

    fun setFilteredItems(filteredItems: ArrayList<RFPResponse.Value>) {
        list = filteredItems
        notifyDataSetChanged()
    }

    fun clearItems() {
        list.clear()
        Log.e("Clear==>", "" + list.size)
        notifyDataSetChanged()
    }

}