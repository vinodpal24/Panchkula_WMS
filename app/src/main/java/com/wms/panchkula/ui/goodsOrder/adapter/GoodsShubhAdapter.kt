package com.wms.panchkula.ui.goodsOrder.adapter

import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.wms.panchkula.Global_Classes.GlobalMethods
import com.wms.panchkula.databinding.BatchItemsScannedLayoutBinding
import com.wms.panchkula.Global_Classes.GlobalMethods.setDecimalLimit
import com.wms.panchkula.ui.goodsOrder.model.LocalListForGoods
import com.wms.panchkula.ui.issueForProductionOrder.Model.ScanedOrderBatchedItems

class GoodsShubhAdapter(
    private var context: Context,
    private var scannedItemForGood: MutableList<LocalListForGoods>,
    private val tvHeaderQuantity: TextView, private val listener: OnItemActionListener
) : RecyclerView.Adapter<GoodsShubhAdapter.ViewHolder>() {

    interface OnItemActionListener {
        fun onQuantityChanged(position: Int, newQuantity: String)
        fun onItemRemoved(position: Int)
    }

    var headerQuantString=""

    private var onDeleteItemClick: OnDeleteItemClickListener? = null

    interface OnDeleteItemClickListener {
        fun onDeleteItemClick(list: ArrayList<ScanedOrderBatchedItems.Value>, quantityHashMap: ArrayList<String>, pos: Int, batchTypeList: ArrayList<String>, serialTypeList: ArrayList<String>)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = BatchItemsScannedLayoutBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(scannedItemForGood[position], position)

    }


    init {
        setHasStableIds(true)
    }

    override fun getItemCount(): Int {
        return scannedItemForGood.size
    }


    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getItemViewType(position: Int): Int {
        return position
    }

    inner class ViewHolder(val binding: BatchItemsScannedLayoutBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(currentItem: LocalListForGoods, position: Int) {
            setDecimalLimit(binding.tvBatchQuantity,15,4)
            binding.tvDocEntry.text = ":   "+currentItem.DocEntry
            binding.tvItemCode.text = ":   "+ currentItem.ItemCode
            binding.tvItemDesc.text = ":   "+currentItem.ItemDescription


         /*   if (currentItem.SerialNumber == null) {
                binding.tvBatchType.text = "Batch No."
                binding.tvBatch.text = ":   " + currentItem.Batch
                binding.batchQtyLayout.visibility = View.VISIBLE
                binding.tvSerialQty.visibility = View.GONE
            } else {
                binding.tvBatchType.text = "Serial No."
                binding.tvBatch.text = ":   " + currentItem.SerialNumber
                binding.batchQtyLayout.visibility = View.GONE
                binding.tvSerialQty.visibility = View.VISIBLE
            }*/


            if (currentItem.SerialNumber == null){
                binding.tvBatchType.text = "Batch No."
                binding.tvBatch.text = ":   "+ currentItem.Batch
                binding.batchQtyLayout.visibility = View.VISIBLE
                binding.tvSerialQty.visibility = View.GONE
            }
            else if (currentItem.SerialNumber == "" && currentItem.Batch == ""){
                binding.tvBatchType.text = "None"
                binding.tvBatch.text = ":  "+currentItem.NoneVal
                binding.batchQtyLayout.visibility = View.VISIBLE
                binding.tvSerialQty.visibility = View.GONE

                binding.tvBatchType.visibility = View.GONE
                binding.tvBatch.visibility = View.GONE
                binding.tvUOM.visibility = View.GONE
                binding.tvDocEntry.visibility = View.GONE

                binding.tvItemCode.text = ":   "+ currentItem.ItemCode
                binding.tvItemDesc.text = ":   "+currentItem.ItemDescription
                binding.tvWidth.text = ":   NA"
                binding.tvBatchLength.text = ":   NA"
                binding.tvBatchGsm.text = ":   NA"
                binding.tvBatchGrossWeigth.text = ":   NA"

            }
            else{
                binding.tvBatchType.text = "Serial No."
                binding.tvBatch.text = ":   "+ currentItem.SerialNumber
                binding.batchQtyLayout.visibility = View.GONE
                binding.tvSerialQty.visibility = View.VISIBLE
            }


            binding.tvSerialQty.text = ":   " + GlobalMethods.changeDecimal(currentItem.Quantity)

            binding.tvBatchQuantity.setText(GlobalMethods.changeDecimal(currentItem.Quantity).toString())

            binding.tvBatchQuantity.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    val newQuantity = s.toString() ?: "0.0"
                    var QUANTITYVAL = newQuantity

                    if (QUANTITYVAL.isEmpty()) {
                        QUANTITYVAL = "0"
                    }

                    listener.onQuantityChanged(adapterPosition, QUANTITYVAL)
                }
            })

            binding.ivDelete.setOnClickListener {
                listener.onItemRemoved(adapterPosition)
            }


        }


    }

    fun setOnDeleteItemClickListener(listener: OnDeleteItemClickListener) {
        onDeleteItemClick = listener
    }


    fun removeItem(position: Int) {
        scannedItemForGood.removeAt(position)
        notifyItemRemoved(position)
        notifyItemRangeChanged(position, scannedItemForGood.size)
    }


}