package com.wms.panchkula.Global_Classes

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.annotation.RequiresApi
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.wms.panchkula.Global_Classes.GlobalMethods.calculateSumValues
import com.wms.panchkula.Global_Classes.GlobalMethods.fixToFourDecimalPlaces
import com.wms.panchkula.Global_Classes.GlobalMethods.toSimpleJson
import com.wms.panchkula.R
import com.wms.panchkula.databinding.LayoutFreightDialogBinding
import com.wms.panchkula.ui.purchase.FreightDataAdapter
import com.wms.panchkula.ui.purchase.model.FreightDataModel
import com.wms.panchkula.ui.purchase.model.FreightTypeModel
import com.wms.panchkula.ui.purchase.model.TaxListModel
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.ceil
import kotlin.math.floor


class FreightBottomDialogFragment(
    private val freightTypeList: ArrayList<FreightTypeModel.Value>,
    private val freightTaxList: ArrayList<TaxListModel.Value>,
    private val postedJson: String,
    private val onUpdateFreightData: (ArrayList<FreightDataModel.DocumentAdditionalExpenses>, String, Double) -> Unit
) : BottomSheetDialogFragment(), View.OnClickListener {

    private lateinit var mContext: Context
    private lateinit var binding: LayoutFreightDialogBinding
    private lateinit var freightAdapter: FreightDataAdapter
    private var docLineTaxSum: Double = 0.0
    private var docLineTotal: Double = 0.0
    private var rounding: String = "N"
    private var roundOffValue: Double = 0.00

    override fun getTheme(): Int = R.style.BottomSheetDialog

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog{
        val dialog = BottomSheetDialog(requireContext(), theme)

        dialog.setOnShowListener {
            val bottomSheet = dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            bottomSheet?.layoutParams?.height = ViewGroup.LayoutParams.MATCH_PARENT
            BottomSheetBehavior.from(bottomSheet!!).state = BottomSheetBehavior.STATE_EXPANDED
        }

        // ⬇️ This is the key change
        dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)

        return dialog
    }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        setStyle(DialogFragment.STYLE_NORMAL, R.style.BottomSheetDialog)
        binding = LayoutFreightDialogBinding.inflate(inflater, container, false)
        setListeners()
        return binding.root

    }

    private fun setListeners() {
        binding.ivCancelDialog.setOnClickListener(this)
        binding.tvBtnDone.setOnClickListener(this)
        binding.btnAddBoxLayout.setOnClickListener(this)
        binding.etRoundOff.setOnClickListener(this)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dialog?.setCancelable(false)
        val parentLayout: FrameLayout? = dialog!!.findViewById(com.google.android.material.R.id.design_bottom_sheet)
        parentLayout?.let { it_ ->
            val behaviour = BottomSheetBehavior.from(it_)
            //showFullScreenBottomSheet(it_)
            behaviour.state = BottomSheetBehavior.STATE_EXPANDED
        }
        Log.i("FREIGHT_CHARGES", "Posted Json: $postedJson")
        //AppConstants.freightDataList.clear()
        val (totalBeforeDiscountAmount, totalBeforeDiscountAmountTax) = calculateSumValues(postedJson)
        binding.tvTbd.text = fixToFourDecimalPlaces(totalBeforeDiscountAmount)
        docLineTaxSum = totalBeforeDiscountAmountTax
        docLineTotal = totalBeforeDiscountAmount
        Log.i("FREIGHT_CHARGES", "TBD: $totalBeforeDiscountAmount, Tax: $totalBeforeDiscountAmountTax")
        /*AppConstants.freightDataList.add(
            FreightDataModel.DocumentAdditionalExpenses(
                ExpenseCode = 0,
                LineTotal = 0.0,
                TaxLiable = "tYES",
                TaxCode = "",
                TaxSum = 0.0,
                LineGross = 0.0,
                DistributionMethod = "aedm_None"
            )
        )*/

        val itemList = AppConstants.freightDataList
        val totalLineTotal = itemList.sumOf { it.LineTotal ?: 0.0 }

        // Sum of TaxSum
        val totalTaxSum = itemList.sumOf { it.TaxSum ?: 0.0 }
        val totalTaxes = totalTaxSum + docLineTaxSum
        val totalWithTax = docLineTotal + totalLineTotal + totalTaxes
        binding.tvTotalTaxes.text = fixToFourDecimalPlaces(totalTaxes)
        binding.tvFreightCharges.text = fixToFourDecimalPlaces(totalLineTotal)
        binding.tvAmountWithTaxes.text = fixToFourDecimalPlaces(totalWithTax)
        val (grossTotal, roundOff) = calculateGrossAndRoundOff(totalWithTax)
        val grandTotal = totalWithTax + binding.etRoundOff.text.toString().toDouble()
        binding.tvGrossTotal.text = fixToFourDecimalPlaces(grandTotal)
        binding.etRoundOff.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val roundOffInput = s?.toString()?.trim()
                val roundOffValue1 = roundOffInput?.toDoubleOrNull() ?: 0.00

                // Recalculate totalWithTax dynamically
                val totalWithTax = calculateTotalWithTax()

                val grandTotal = BigDecimal.valueOf(totalWithTax)
                    .add(BigDecimal.valueOf(roundOffValue1))
                    .setScale(2, RoundingMode.HALF_UP)

                if(rounding == "Y"){
                    roundOffValue = roundOffValue1
                }else{
                    binding.etRoundOff.setText("0.0")
                    roundOffValue = 0.0
                }
                binding.tvGrossTotal.text = grandTotal.toPlainString()

                Log.d("FREIGHT_CHARGES", "etRoundOff.addTextChangedListener => TotalWithTax=$totalWithTax, RoundOffValue=$roundOffValue1, GrandTotal=$grandTotal")
            }

            override fun afterTextChanged(s: Editable?) {

            }
        })

        binding.cbRoundOff.setOnCheckedChangeListener { buttonView, isChecked ->
            binding.etRoundOff.isEnabled = isChecked
            val totalWithTax = calculateTotalWithTax()

            if (isChecked) {
                rounding = "Y"
                roundOffValue = binding.etRoundOff.text.toString().toDoubleOrNull() ?: 0.0

            } else {
                rounding = "N"
                binding.etRoundOff.setText("0.0")
                roundOffValue = 0.0
            }

            val grandTotal = totalWithTax + roundOffValue
            Log.d("FREIGHT_CHARGES", "Check Box RoundOff.setOnCheckedChangeListener => TotalWithTax=$totalWithTax,Rounding=$rounding RoundOffValue=$roundOffValue, GrandTotal=$grandTotal")
            binding.tvGrossTotal.text = fixToFourDecimalPlaces(grandTotal)
        }

        //binding.etRoundOff.text = fixToFourDecimalPlaces(roundOff).toEditable()
        setFreightAdapter()

    }

    private fun calculateTotalWithTax(): Double {
        val itemList = AppConstants.freightDataList
        val totalLineTotal = itemList.sumOf { it.LineTotal ?: 0.0 }
        val totalTaxSum = itemList.sumOf { it.TaxSum ?: 0.0 }
        val totalTaxes = totalTaxSum + docLineTaxSum
        return docLineTotal + totalLineTotal + totalTaxes
    }

    private fun setFreightAdapter() {
        binding.rvFreightTaxes.apply {
            freightAdapter = FreightDataAdapter(requireContext(), freightTypeList, freightTaxList, AppConstants.freightDataList, onRemoveFreightLayout = { removedIndex ->
                if (removedIndex in AppConstants.freightDataList.indices) {
                    if (AppConstants.freightDataList.size > 1) {
                        AppConstants.freightDataList.removeAt(removedIndex)
                    } else {
                        AppConstants.freightDataList.removeAt(removedIndex)
                        AppConstants.freightDataList.clear()
                    }
                    freightAdapter.notifyDataSetChanged()
                    val itemList = AppConstants.freightDataList
                    val totalLineTotal = itemList.sumOf { it.LineTotal ?: 0.0 }
                    val totalTaxSum = itemList.sumOf { it.TaxSum ?: 0.0 }
                    val totalTaxes = totalTaxSum + docLineTaxSum
                    val totalWithTax = docLineTotal + totalLineTotal + totalTaxes
                    binding.tvTotalTaxes.text = fixToFourDecimalPlaces(totalTaxes)
                    binding.tvFreightCharges.text = fixToFourDecimalPlaces(totalLineTotal)
                    binding.tvAmountWithTaxes.text = fixToFourDecimalPlaces(totalWithTax)
                    val (grossTotal, roundOff) = calculateGrossAndRoundOff(totalWithTax)
                    val grandTotal = totalWithTax + binding.etRoundOff.text.toString().toDouble()
                    binding.tvGrossTotal.text = fixToFourDecimalPlaces(grandTotal)
                    //binding.etRoundOff.text = fixToFourDecimalPlaces(roundOff).toEditable()
                    Log.i("FREIGHTS","Remove-> Grand Total: $grandTotal (${binding.etRoundOff.text})")
                    Log.i("FREIGHT_CHARGES", "onRemoveBoxLayout.setOnClickListener: ${toSimpleJson(AppConstants.freightDataList)}")
                }
            }, onUpdateFreightData = {
                // Sum of LineTotal
                val totalLineTotal = it.sumOf { it.LineTotal ?: 0.0 }

                // Sum of TaxSum
                val totalTaxSum = it.sumOf { it.TaxSum ?: 0.0 }
                val totalTaxes = totalTaxSum + docLineTaxSum
                val totalWithTax = docLineTotal + totalLineTotal + totalTaxes
                binding.tvTotalTaxes.text = fixToFourDecimalPlaces(totalTaxes)
                binding.tvFreightCharges.text = fixToFourDecimalPlaces(totalLineTotal)
                binding.tvAmountWithTaxes.text = fixToFourDecimalPlaces(totalWithTax)
                val (grossTotal, roundOff) = calculateGrossAndRoundOff(totalWithTax)
                val grandTotal = totalWithTax + binding.etRoundOff.text.toString().toDouble()
                binding.tvGrossTotal.text = fixToFourDecimalPlaces(grandTotal)
                //binding.etRoundOff.text = fixToFourDecimalPlaces(roundOff).toEditable()
                Log.i("FREIGHTS","onUpdateFreightData-> Grand Total: $grandTotal (${binding.etRoundOff.text})")
                Log.i("FREIGHT_CHARGES", "onUpdateBoxLayout.setOnClickListener: ${toSimpleJson(AppConstants.freightDataList)}")
            })

            layoutManager = LinearLayoutManager(requireContext())
            adapter = freightAdapter
        }
    }

    fun calculateGrossAndRoundOff(totalWithTax: Double): Pair<Double, Double> {
        val decimalPart = totalWithTax - floor(totalWithTax)

        val grossTotal = if (decimalPart >= 0.50) {
            ceil(totalWithTax)
        } else {
            floor(totalWithTax)
        }

        val roundOff = String.format("%.2f", grossTotal - totalWithTax).toDouble()

        return Pair(grossTotal, roundOff)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mContext = context as FragmentActivity
    }

    override fun onStart() {
        super.onStart()
        val dialog = dialog as? BottomSheetDialog
        dialog?.let {
            val bottomSheet = it.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet) as? FrameLayout
            bottomSheet?.let { sheet ->
                val behavior = BottomSheetBehavior.from(sheet)
                sheet.layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
                behavior.skipCollapsed = true
                behavior.isDraggable = false  // Prevent dragging
            }
        }
    }


    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.ivCancelDialog -> {
                dismiss()
            }

            R.id.tvBtnDone -> {
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Confirm...")
                    .setMessage("Are you sure, you want to post GRPO?")
                    .setIcon(R.drawable.ic_trash)
                    .setPositiveButton("Confirm") { dialogInterface: DialogInterface?, i1: Int ->
                        onUpdateFreightData(AppConstants.freightDataList, rounding, roundOffValue)
                        dismiss()
                    }
                    .setNegativeButton("Cancel") { dialogInterface, i ->
                        dialogInterface.dismiss()
                    }

                    .show()
                Log.i("FREIGHT_CHARGES", "tvBtnDone=> Rounding: $rounding,  RoundOffValue: $roundOffValue")
            }

            R.id.btnAddBoxLayout -> {
                AppConstants.freightDataList.add(
                    FreightDataModel.DocumentAdditionalExpenses(
                        ExpenseCode = 0,
                        LineTotal = 0.0,
                        TaxLiable = "tYES",
                        TaxCode = "",
                        TaxSum = 0.0,
                        LineGross = 0.0,
                        DistributionMethod = "aedm_None"
                    )
                )
                freightAdapter.notifyItemInserted(AppConstants.freightDataList.size - 1)
                Log.i("FREIGHT_CHARGES", "btnAddBoxLayout.setOnClickListener: ${toSimpleJson(AppConstants.freightDataList)}")
            }
        }
    }
}