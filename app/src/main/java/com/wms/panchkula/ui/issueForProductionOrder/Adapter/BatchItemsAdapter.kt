package com.wms.panchkula.ui.issueForProductionOrder.Adapter

import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.wms.panchkula.Global_Classes.GlobalMethods
import com.wms.panchkula.databinding.BatchItemsScannedLayoutBinding
import com.wms.panchkula.ui.issueForProductionOrder.Model.ScanedOrderBatchedItems
import com.google.android.material.textfield.TextInputEditText
import com.wms.panchkula.Global_Classes.GlobalMethods.setDecimalLimit
import com.wms.panchkula.Global_Classes.MaterialProgressDialog
import com.wms.panchkula.Global_Notification.NetworkConnection

class BatchItemsAdapter(
    private var context: Context, private var scanedBatchedItemsList_gl: ArrayList<ScanedOrderBatchedItems.Value>, private var quantityHashMap: ArrayList<String>,
    private var flag: String, private var parentPosition: Int, private var rvBatchItems: RecyclerView, private var onTextChanged: (String, Int, TextInputEditText) -> Unit
) : RecyclerView.Adapter<BatchItemsAdapter.ViewHolder>() {
    lateinit var networkConnection: NetworkConnection
    lateinit var materialProgressDialog: MaterialProgressDialog

    constructor(
        context: Context, scannedBatchedItemsList: ArrayList<ScanedOrderBatchedItems.Value>, quantityHashMap: ArrayList<String>, flag: String,
        parentPosition: Int, rvBatchItems: RecyclerView
    ) : this(context, scannedBatchedItemsList, quantityHashMap, flag, parentPosition, rvBatchItems, { _, _, _ -> }) {
        //Additional initialization code if needed

        this.scanedBatchedItemsList_gl = scannedBatchedItemsList
        this.context = context
        this.quantityHashMap = quantityHashMap
        this.flag = flag
        this.parentPosition = parentPosition
        this.rvBatchItems = rvBatchItems

    }

    interface OnScannedItemClickListener {
        fun onScannedItemClicked(
            batchItem: ScanedOrderBatchedItems.Value,
            parentPosition: Int,
            batchItemPosition: Int
        )
    }

    private var onScannedItemClickListener: OnScannedItemClickListener? = null
    private var onDeleteItemClick: OnDeleteItemClickListener? = null

    fun setOnScannedItemClickListener(listener: OnScannedItemClickListener) {
        onScannedItemClickListener = listener
    }


    interface OnDeleteItemClickListener {
        fun onDeleteItemClick(list: ArrayList<ScanedOrderBatchedItems.Value>, quantityHashMap: ArrayList<String>, pos: Int, parentPosition: Int, rvBatchItems: RecyclerView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = BatchItemsScannedLayoutBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }


    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        with(holder) {
            setDecimalLimit(binding.tvBatchQuantity, 15, 4)
            with(scanedBatchedItemsList_gl[position]) {
                Log.e("Batch==>Data", scanedBatchedItemsList_gl.toString())
                binding.tvDocEntry.text = this.DocEntry
                binding.tvItemCode.text = this.ItemCode
                binding.tvItemDesc.text = this.ItemDescription
                binding.tvWidth.text = this.U_Width.toString()
                binding.tvBatchLength.text = this.U_Length.toString()
                binding.tvBatchGsm.text = this.U_GSM.toString()
                binding.tvBatchGrossWeigth.text = this.U_GW.toString()


                if (flag.equals("SerialQR")) {
                    binding.tvBatchType.text = "Serial No."
                    binding.tvBatch.text = this.SerialNumber
                } else if (flag.equals("NoneQR")) {
                    binding.tvBatchType.text = "None No."
                    binding.tvBatch.text = this.NoneVal
                } else {
                    binding.tvBatchType.text = "Batch No."
                    binding.tvBatch.text = this.Batch
                }
//                holder.bind(position, parentPosition)
                holder.itemView.setOnClickListener {
                    //Toast.makeText(context, "Clicked on scanned item in BatchItemsAdapter", Toast.LENGTH_SHORT).show()
                    //Log.i("SCANNED_CLICKED","Clicked on scanned item in BatchItemsAdapter")
                    onScannedItemClickListener?.onScannedItemClicked(this, parentPosition, position)
                }

                binding.ivDelete.setOnClickListener {

                    onDeleteItemClick?.onDeleteItemClick(scanedBatchedItemsList_gl, quantityHashMap, position, parentPosition, rvBatchItems)

                }

                if (flag.equals("SerialQR")) {//|| flag.equals("NoneQR")
                    binding.batchQtyLayout.visibility = View.GONE
                    binding.tvSerialQty.visibility = View.VISIBLE
                } else {
                    binding.batchQtyLayout.visibility = View.VISIBLE
                    binding.tvSerialQty.visibility = View.GONE
                }

                Log.e("BatchAdapter BP=>0", position.toString())
                Log.e("BatchAdapter BP=>1", quantityHashMap[position].toString())

                with(quantityHashMap[position]) {
                    binding.tvSerialQty.text = GlobalMethods.changeDecimal(this).toString()

                    binding.tvBatchQuantity.setText(GlobalMethods.changeDecimal(this).toString())
                }


                binding.tvBatchQuantity.addTextChangedListener(object : TextWatcher {
                    override fun beforeTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {}

                    override fun onTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {
                        quantityHashMap[adapterPosition] = charSequence.toString()
                        onTextChanged(charSequence.toString(), adapterPosition, binding.tvBatchQuantity)

                    }

                    override fun afterTextChanged(editable: Editable) {}
                })


            }
        }
    }

    override fun getItemCount(): Int {

        return quantityHashMap.size
    }

    inner class ViewHolder(val binding: BatchItemsScannedLayoutBinding) : RecyclerView.ViewHolder(binding.root)

    fun setOnDeleteItemClickListener(listener: OnDeleteItemClickListener) {
        onDeleteItemClick = listener

    }

    fun removeItem(position: Int) {
        if (position >= 0 && position < scanedBatchedItemsList_gl.size && position < quantityHashMap.size) {
            scanedBatchedItemsList_gl.removeAt(position)
            quantityHashMap.removeAt(position)
            notifyItemRemoved(position)
            notifyItemRangeChanged(position, itemCount)
            notifyDataSetChanged()
        }
    }


}