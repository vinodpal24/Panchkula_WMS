package com.wms.panchkula.issueOrder

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.wms.panchkula.databinding.BatchItemsScannedLayoutBinding
import com.wms.panchkula.ui.issueForProductionOrder.Model.ProductionListModel
import com.google.android.material.textfield.TextInputEditText
import com.wms.panchkula.Global_Classes.GlobalMethods.setDecimalLimit

class InnerItemAdapter(private var context: Context, private var scanedBatchedItemsList_gl: MutableList<ProductionListModel.ProductionOrderLine>, private val tvHeaderQuantity: TextView, private val listener: OnItemActionListener, var flag : String) : RecyclerView.Adapter<InnerItemAdapter.ViewHolder>() {

    interface OnItemActionListener {
        fun onQuantityChanged(position: Int, newQuantity: String, tvBatchQuantity : TextInputEditText)
        fun onItemRemoved(position: Int)
        fun onWareHouseChanged(position: Int, newQuantity: String, warehouse:String, currentItem: ProductionListModel.ProductionOrderLine)
    }

    private var onDeleteItemClick: OnDeleteItemClickListener? = null

    interface OnDeleteItemClickListener {
        fun onDeleteItemClick(list: MutableList<ProductionListModel.ProductionOrderLine>, quantityHashMap: ArrayList<String>, pos: Int, batchTypeList: ArrayList<String>, serialTypeList: ArrayList<String>)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = BatchItemsScannedLayoutBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        with(holder) {
            setDecimalLimit(binding.tvBatchQuantity,15,4)
            with(scanedBatchedItemsList_gl[position]) {

                binding.tvDocEntry.text = ":   " + this.DocEntry
                binding.tvItemCode.text = ":   " + this.ItemCode
                binding.tvItemDesc.text = ":   " + this.ItemDescription
                binding.tvWidth.text = ":   " + this.U_Width.toString()
                binding.tvBatchLength.text = ":   " + this.U_Length.toString()
                binding.tvBatchGsm.text = ":   " + this.U_GSM.toString()
                binding.tvBatchGrossWeigth.text = ":   " + this.U_GW.toString()


                if (flag.equals("SerialQR")) {
                    binding.tvBatchType.text = "Serial No."
                    binding.tvBatch.text = ":   " + this.SerialNumber
                } else if (flag.equals("NoneQR")) {
                    binding.tvBatchType.text = "None No."
                    binding.tvBatch.text = ":   " + this.NoneVal
                } else {
                    binding.tvBatchType.text = "Batch No."
                    binding.tvBatch.text = ":   " + this.Batch
                }
//                holder.bind(position, parentPosition)


                /*binding.ivDelete.setOnClickListener {

                    onDeleteItemClick?.onDeleteItemClick(scanedBatchedItemsList_gl, quantityHashMap, position, parentPosition, rvBatchItems)

                }*/

                binding.ivDelete.setOnClickListener {
                    listener.onItemRemoved(position)
                }

                if (flag.equals("SerialQR")) {//|| flag.equals("NoneQR")
                    binding.batchQtyLayout.visibility = View.GONE
                    binding.tvSerialQty.visibility = View.VISIBLE
                } else {
                    binding.batchQtyLayout.visibility = View.VISIBLE
                    binding.tvSerialQty.visibility = View.GONE
                }

             /*   with(this.batchList) {
                    binding.tvSerialQty.text = ":   " + GlobalMethods.changeDecimal(this)

                    binding.tvBatchQuantity.setText(GlobalMethods.changeDecimal(this).toString())
                }
*/

               /* binding.tvBatchQuantity.addTextChangedListener(object : TextWatcher {
                    override fun beforeTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {}

                    override fun onTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {
                        quantityHashMap[adapterPosition] = charSequence.toString()
                        onTextChanged(charSequence.toString(), adapterPosition, binding.tvBatchQuantity)

                    }

                    override fun afterTextChanged(editable: Editable) {}
                })*/


            }
        }
    }

    override fun getItemCount(): Int {
        return scanedBatchedItemsList_gl.size
    }

    inner class ViewHolder(val binding: BatchItemsScannedLayoutBinding) : RecyclerView.ViewHolder(binding.root)

    fun setOnDeleteItemClickListener(listener: OnDeleteItemClickListener) {
        onDeleteItemClick = listener
    }

/*
    fun removeItem(position: Int) {
        if (position >= 0 && position < scanedBatchedItemsList_gl.size && position < quantityHashMap.size) {
            scanedBatchedItemsList_gl.removeAt(position)
            quantityHashMap.removeAt(position)
            notifyItemRemoved(position)
            notifyItemRangeChanged(position, itemCount)
            notifyDataSetChanged()
        }
    }*/

    fun removeItem(position: Int) {
        scanedBatchedItemsList_gl.removeAt(position)
//        notifyItemRemoved(position)
//        notifyItemRangeChanged(position, scannedItemForGood.size)
        notifyDataSetChanged()
    }

}