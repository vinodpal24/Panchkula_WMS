package com.wms.panchkula.ui.purchase

import android.content.Context
import android.content.DialogInterface
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.wms.panchkula.Global_Classes.AppConstants
import com.wms.panchkula.Global_Classes.GlobalMethods.fixToFourDecimalPlaces
import com.wms.panchkula.Global_Classes.MaterialProgressDialog
import com.wms.panchkula.Global_Notification.NetworkConnection
import com.wms.panchkula.R
import com.wms.panchkula.databinding.RvItemFreightBinding
import com.wms.panchkula.ui.purchase.model.FreightDataModel
import com.wms.panchkula.ui.purchase.model.FreightTypeModel
import com.wms.panchkula.ui.purchase.model.TaxListModel

class FreightDataAdapter(
    private val context: Context,
    private val freightTypeList: ArrayList<FreightTypeModel.Value>,
    private val freightTaxList: ArrayList<TaxListModel.Value>,
    private val freightDataList: ArrayList<FreightDataModel.DocumentAdditionalExpenses>,
    private val onRemoveFreightLayout: (Int) -> Unit,
    private val onUpdateFreightData: (ArrayList<FreightDataModel.DocumentAdditionalExpenses>) -> Unit
) : RecyclerView.Adapter<FreightDataAdapter.ViewHolder>() {
    lateinit var materialProgressDialog: MaterialProgressDialog
    lateinit var networkConnection: NetworkConnection
    var selectedFreightType: String = ""
    var selectedFreightCode: Int = 0
    var selectedFreightTaxCodeName: String = ""
    var selectedFreightTaxCode: String = ""
    var selectedFreightTaxPercentage: String = ""

    inner class ViewHolder(val binding: RvItemFreightBinding) : RecyclerView.ViewHolder(binding.root) {
        private var currentFreightAmount = 0.0

        init {
            materialProgressDialog = MaterialProgressDialog(context)
            networkConnection = NetworkConnection()

            binding.ivCancel.setOnClickListener {

                MaterialAlertDialogBuilder(context)
                    .setTitle("Confirm...")
                    .setMessage("Do you want to delete freight layout?")
                    .setIcon(R.drawable.ic_trash)
                    .setPositiveButton("Confirm") { dialogInterface: DialogInterface?, i1: Int ->
                        val currentPosition = bindingAdapterPosition
                        if (currentPosition != RecyclerView.NO_POSITION) {
                            onRemoveFreightLayout(currentPosition)
                        }
                    }
                    .setNegativeButton("Cancel") { dialogInterface, i ->
                        dialogInterface.dismiss()
                    }

                    .show()
            }

        }

        fun bind(currentItem: FreightDataModel.DocumentAdditionalExpenses) {
            setFreightTypeSpinner(binding, currentItem, freightTypeList)
            setFreightTaxSpinner(binding, currentItem, freightTaxList)
            binding.apply {

                //currentItem.ExpenseCode = freightTypeList[0].Fcode
                //currentItem.TaxCode = freightTaxList[0].Tcode.toString()
                //currentItem.LineGross = acTotalFreightAmount.text.toString().toDouble()


                /*if (etFreightAmount.text.toString().isEmpty()) {
                    GlobalMethods.showError(context, "Please enter freight amount or remove freight.")
                    return
                }*/

                if (currentItem.LineTotal!! > 0.0) {
                    etFreightAmount.setText(currentItem.LineTotal.toString())
                    // ✅ Now call the calculation with correct tax
                    setFreightTaxSpinner(binding, currentItem, freightTaxList)
                    calculatedTotal(currentItem)
                }

                etFreightAmount.addTextChangedListener(object : TextWatcher {
                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

                    }

                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                        currentFreightAmount = s.toString().toDoubleOrNull() ?: 0.0
                        currentItem.LineTotal = currentFreightAmount
                        calculatedTotal(currentItem)
                    }

                    override fun afterTextChanged(s: Editable?) {

                    }
                })
            }
        }

        private fun setFreightTypeSpinner(
            binding: RvItemFreightBinding,
            currentItem: FreightDataModel.DocumentAdditionalExpenses,
            freightTypeList: ArrayList<FreightTypeModel.Value>
        ) {
            val taxTypeList = freightTypeList.map { it.Fname }
            val adapter = ArrayAdapter(context, R.layout.drop_down_item_textview, taxTypeList)
            binding.acFreightType.setAdapter(adapter)

            // ✅ Set default if value exists
            val defaultIndex = freightTypeList.indexOfFirst { it.Fcode == currentItem.ExpenseCode }
            if (defaultIndex != -1) {
                val defaultName = freightTypeList[defaultIndex].Fname
                if (binding.acFreightType.text.toString() != defaultName) {
                    binding.acFreightType.setText(defaultName, false)
                }
            } else {
                // fallback to first item if nothing is set
                currentItem.ExpenseCode = freightTypeList[0].Fcode ?: 0
                binding.acFreightType.setText(freightTypeList[0].Fname ?: "", false)
            }

            // ✅ Set item click listener only once
            binding.acFreightType.setOnItemClickListener { parent, view, position, id ->
                val selectedName = parent.getItemAtPosition(position).toString()
                val selectedCode = freightTypeList[position].Fcode ?: 0
                currentItem.ExpenseCode = selectedCode
                binding.acFreightType.setText(selectedName, false)
                Toast.makeText(context, "Selected: $selectedName", Toast.LENGTH_SHORT).show()
            }
        }


        private fun setFreightTaxSpinner(
            binding: RvItemFreightBinding,
            currentItem: FreightDataModel.DocumentAdditionalExpenses,
            freightTaxList: ArrayList<TaxListModel.Value>
        ) {
            val taxList = freightTaxList.map { it.Tname }
            val adapter = ArrayAdapter(context, R.layout.drop_down_item_textview, taxList)
            binding.acFTaxCode.setAdapter(adapter)

            // Check if currentItem.TaxCode matches any item in freightTaxList
            val defaultIndex = freightTaxList.indexOfFirst { it.Tcode.toString() == currentItem.TaxCode }

            val selectedIndex = if (defaultIndex != -1) defaultIndex else 0

            selectedFreightTaxCodeName = taxList[selectedIndex].toString()
            selectedFreightTaxCode = freightTaxList[selectedIndex].Tcode.toString()
            selectedFreightTaxPercentage = freightTaxList[selectedIndex].Rate.toString()
            currentItem.TaxCode = selectedFreightTaxCode

            // Set default selected value
            binding.acFTaxCode.setText(taxList[selectedIndex], false)

            // Set item click listener
            binding.acFTaxCode.setOnItemClickListener { parent, view, position, id ->
                val selectedItem = parent.getItemAtPosition(position).toString()
                selectedFreightTaxCodeName = selectedItem
                selectedFreightTaxCode = freightTaxList[position].Tcode.toString()
                selectedFreightTaxPercentage = freightTaxList[position].Rate.toString()
                currentItem.TaxCode = selectedFreightTaxCode
                calculatedTotal(currentItem)
                Toast.makeText(context, "Selected: $selectedItem", Toast.LENGTH_SHORT).show()
            }
        }


        private fun calculatedTotal(currentItem: FreightDataModel.DocumentAdditionalExpenses) {
            val matchedTax = freightTaxList.firstOrNull { it.Tcode.toString() == currentItem.TaxCode }
            selectedFreightTaxPercentage = matchedTax?.Rate.toString()
            currentFreightAmount = currentItem.LineTotal!!
            val taxAmount = currentFreightAmount * (selectedFreightTaxPercentage.toDoubleOrNull() ?: 0.0) / 100
            val totalAmount = currentFreightAmount + taxAmount
            binding.acTotalFreightAmount.setText(fixToFourDecimalPlaces(totalAmount))
            currentItem.LineGross = totalAmount
            currentItem.TaxSum = taxAmount

            onUpdateFreightData(AppConstants.freightDataList)
            Log.i(
                "FREIGHT_CHARGES", "LineTotal: $currentFreightAmount, " +
                        "TaxAmount: $taxAmount, TotalAmount: $totalAmount, TaxPercentage: $selectedFreightTaxPercentage"
            )
        }
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = RvItemFreightBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int = freightDataList.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(freightDataList[position])
    }
}
