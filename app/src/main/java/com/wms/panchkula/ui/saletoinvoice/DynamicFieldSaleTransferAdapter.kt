package com.wms.panchkula.ui.saletoinvoice

import com.wms.panchkula.databinding.RvItemDynamicFieldSaleTransferBinding
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.RecyclerView
import com.wms.panchkula.R
import com.wms.panchkula.Model.ModelBinLocation

class DynamicFieldSaleTransferAdapter(
    private val context: Context,
    private val binLocationList: List<String>, private val binAbs: List<String>,
    private val binList: MutableList<ModelBinLocation>,
    private val onRemoveItem: (Int) -> Unit,
    private val scanType: String,
    private val binManeged: String
) : RecyclerView.Adapter<DynamicFieldSaleTransferAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: RvItemDynamicFieldSaleTransferBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(installation: ModelBinLocation, position: Int) {
            if (scanType == "Y" && binManeged == "N") {
                binding.batchLay.visibility = View.GONE
                binding.fromBinLay.visibility = View.GONE
                binding.qtyLay.visibility = View.VISIBLE

            }
            if (scanType == "Y") {
                binding.batchLay.visibility = View.GONE

            } else if (scanType == "YYY") {
                binding.batchLay.visibility = View.VISIBLE
                binding.fromBinLay.visibility = View.GONE
                binding.binSaleTransferLay.visibility = View.VISIBLE

            } else if (scanType == "YY") {
                binding.fromTitle.setText("From Bin Location")
                binding.batchNoTxt.setText("To Bin Location")
                binding.batchLay.visibility = View.VISIBLE
                binding.toBinLay.visibility   = View.GONE
                binding.fromBinLay.visibility = View.VISIBLE
                binding.batchLay.visibility = View.VISIBLE
            } else {
                binding.batchLay.visibility = View.VISIBLE
            }


            binding.acBinLocation.setText(installation.binLocation)
            binding.edtBatchNumber.setText(installation.batchNumber)
            binding.edtQuantity.setText(installation.itemQuantity)

            binding.ivRemoveItem.setOnClickListener { onRemoveItem(position) }
            binding.edtBinLocationSaleTransfer.addTextChangedListener {

                installation.binLocationCode =  binding.edtBinLocationSaleTransfer.text.toString().split(",")[2]

            }


            binding.edtBatchNumber.addTextChangedListener {
                if (scanType == "YY") {
                    if (binding.edtBatchNumber.text.toString().split(",").size >= 2) {
                        installation.batchNumber =
                            binding.edtBatchNumber.text.toString().split(",")[2]


                    } else {
                        installation.batchNumber = binding.edtBatchNumber.text.toString()
                    }
                }

                // add by tarun
                else if (scanType == "YYY") {
                    if (binding.edtBatchNumber.text.toString().split(",").size >= 2) {
                        installation.batchNumber =
                            binding.edtBatchNumber.text.toString().split(",")[1]
                        installation.WareHouseCode =
                            binding.edtBatchNumber.text.toString().split(",")[0]

                    } else {
                        installation.batchNumber = binding.edtBatchNumber.text.toString()
                    }
                }

                else {
                    installation.batchNumber = binding.edtBatchNumber.text.toString()
                }

            }
            binding.edtQuantity.addTextChangedListener {

                installation.itemQuantity = binding.edtQuantity.text.toString()
            }
            val adapter = ArrayAdapter(context, R.layout.drop_down_item_textview, binLocationList)
            binding.acBinLocation.setAdapter(adapter)

            binding.acBinLocation.setOnItemClickListener { parent, view, position, l ->
                val binLoc = parent.getItemAtPosition(position) as String
                val selectedBin = binLoc

                if (binLoc.isNotEmpty()) {
                    binding.acBinLocation.setText(selectedBin)
                    installation.binLocationCode = binAbs.get(position).trim().toString()

                    /*defaultBinCode = Abs.get(position)

                    if(absArray.any { it == defaultBinCode}){
                          val po =absArray.indexOf(defaultBinCode)
                          absArray[po]=defaultBinCode
                    }
                    else{
                          absArray.add(defaultBinCode)
                    }
                    acBinLocation.setText(selectedBin,false)*/

                } else {
                    binding.acBinLocation.setText("")
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding =
            RvItemDynamicFieldSaleTransferBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(binList[position], position)
    }

    override fun getItemCount(): Int = binList.size
}

