import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.content.Context
import android.os.Build
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.annotation.RequiresApi
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.RecyclerView
import com.wms.panchkula.databinding.RvItemDynamicFieldBinding
import com.google.android.material.textfield.TextInputEditText
import com.wms.panchkula.Model.ModelBinLocation
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Calendar


class DynamicFieldAdapter(
    private val context: Context,
    private val binLocationList: List<String>,
    private val binAbs: List<String>,
    private val binList: MutableList<ModelBinLocation>,
    private val onRemoveItem: (Int) -> Unit,
    private val scanType: String,
    private val binManeged: String
) : RecyclerView.Adapter<DynamicFieldAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: RvItemDynamicFieldBinding) :
        RecyclerView.ViewHolder(binding.root) {

        private var warehouseCode = ""

        @SuppressLint("SuspiciousIndentation")
        @RequiresApi(Build.VERSION_CODES.O)
        fun bind(installation: ModelBinLocation, position: Int) {
            if (scanType == "Y" && binManeged == "N") {
                binding.batchLay.visibility = View.GONE
                binding.fromBinLay.visibility = View.GONE
                binding.qtyLay.visibility = View.VISIBLE

            }
            if (scanType == "Y") {
                binding.batchLay.visibility = View.GONE

            } else if (scanType == "YB") {
                binding.batchLay.visibility = View.GONE
                binding.linearLayoutNew.visibility = View.VISIBLE
                binding.batchLay.visibility = View.VISIBLE
            } else if (scanType == "YYY") {
                binding.batchLay.visibility = View.VISIBLE
                binding.fromBinLay.visibility = View.GONE
            } else if (scanType == "YY_No_TO") {
                binding.fromTitle.setText("From Bin Location")
                binding.batchNoTxt.setText("To Bin Location")
                binding.batchLay.visibility = View.VISIBLE
                binding.toBinLay.visibility = View.GONE
                binding.fromBinLay.visibility = View.VISIBLE
                binding.batchLay.visibility = View.GONE
            } else if (scanType == "YY") {
                binding.fromTitle.setText("From Bin Location")
                binding.batchNoTxt.setText("To Bin Location")
                binding.batchLay.visibility = View.VISIBLE
                binding.toBinLay.visibility = View.GONE
                binding.fromBinLay.visibility = View.VISIBLE
                binding.batchLay.visibility = View.VISIBLE
            } else {
                binding.batchLay.visibility = View.VISIBLE
                binding.layoutAttributes.visibility = View.GONE
                binding.linearLayoutNew.visibility = View.VISIBLE
            }

            Log.e("Test==>Data=>", installation.toString())

            if (position == 0) {
                binding.ivRemoveItem.visibility = View.GONE
            }else{
                binding.ivRemoveItem.visibility = View.VISIBLE
            }

            binding.ivRemoveItem.setOnClickListener { onRemoveItem(position) }
            //binding.acBinLocation.setSelection(0)
            binding.edtBatchNumber.setText(installation.batchNumber)
            binding.edtQuantity.setText(installation.itemQuantity)

            binding.mfgDate.setText(installation.ManufacturingDate)
            binding.expDate.setText(installation.ExpiryDate)
            binding.attribute2.setText(installation.InternalSerialNumber)
            binding.attribute1.setText(installation.ManufacturerSerialNumber)


            // todo code for datePicker (Tarun)
            binding.mfgDate.setOnClickListener {

                showDatePickerDialog(binding.mfgDate, installation, "MFG")

            }

            binding.expDate.setOnClickListener {

                showDatePickerDialog(binding.expDate, installation, "EXP")


            }

            binding.attribute1.addTextChangedListener {

                installation.ManufacturerSerialNumber = binding.attribute1.text.toString()
            }
            binding.attribute2.addTextChangedListener {
                installation.InternalSerialNumber = binding.attribute2.text.toString()
            }


            binding.edtBatchNumber.addTextChangedListener {
                if (scanType == "YY") {
                    if (binding.edtBatchNumber.text.toString().split(",").size >= 2) {
                        installation.batchNumber =
                            binding.edtBatchNumber.text.toString().split(",")[2]
                        installation.WareHouseCode =
                            binding.edtBatchNumber.text.toString().split(",")[0]

                    } else {
                        installation.batchNumber = binding.edtBatchNumber.text.toString()
                    }
                } else {
                    installation.batchNumber = binding.edtBatchNumber.text.toString()
                }

            }
            binding.edtQuantity.addTextChangedListener {

                installation.itemQuantity = binding.edtQuantity.text.toString()
            }
            Log.e("BIN_SELECTION", "binLocationList => ${binLocationList}")

            val adapter = ArrayAdapter(context, android.R.layout.simple_dropdown_item_1line, binLocationList)
            binding.acBinLocation.setAdapter(adapter)
            binding.acBinLocation.threshold = 1

            /*// Set default if available
            installation.binLocationCode?.let { defaultCode ->
                val defaultIndex = binAbs.indexOfFirst { it.trim() == defaultCode.trim() }
                if (defaultIndex >= 0) {
                    // Set label text from binLocationList for the matched binAbs
                    binding.acBinLocation.setText(binLocationList[defaultIndex], false)
                    Log.i("BIN_SELECTION", "Default => BinAbsEntry: ${binAbs[defaultIndex]}, BinLocation: ${binLocationList[defaultIndex]}")
                }
            } ?: run {
                // If no previous selection, select first/default bin
                if (binAbs.isNotEmpty() && binLocationList.isNotEmpty()) {
                    installation.binLocationCode = binAbs[0].trim()
                    binding.acBinLocation.setText(binLocationList[0], false)
                    Log.i("BIN_SELECTION", "Auto-selected Default Bin: ${binAbs[0]}, BinLocation: ${binLocationList[0]}")
                }
            }*/

            val currentCode = installation.binLocationCode?.trim()

            if (currentCode.isNullOrBlank()) {
                // First time: set default bin
                val defaultIndex = binAbs.indexOfFirst { it.isNotBlank() }
                if (defaultIndex >= 0) {
                    installation.binLocationCode = binAbs[defaultIndex].trim()
                    binding.acBinLocation.setText(binLocationList[defaultIndex], false)
                    Log.i("BIN_SELECTION", "First time -> Setting default BinAbsEntry: ${binAbs[defaultIndex]}, BinLocation: ${binLocationList[defaultIndex]}")
                }
            } else {
                // Already selected: retain previous selection
                val selectedIndex = binAbs.indexOfFirst { it.trim() == currentCode }
                if (selectedIndex >= 0) {
                    binding.acBinLocation.setText(binLocationList[selectedIndex], false)
                    Log.i("BIN_SELECTION", "Previous selection -> BinAbsEntry: ${binAbs[selectedIndex]}, BinLocation: ${binLocationList[selectedIndex]}")
                }
            }

            binding.acBinLocation.setOnItemClickListener { _, _, position, _ ->
                installation.binLocationCode = binAbs[position].trim()
                binding.acBinLocation.setText(binLocationList[position], false)
                Log.i(
                    "BIN_SELECTION",
                    "Selection => BinAbsEntry: ${binAbs[position]}, BinLocation: ${binLocationList[position]}"
                )
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding =
            RvItemDynamicFieldBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        Log.d("jhbsd", "onBindViewHolder: $binLocationList")
        holder.bind(binList[position], position)
    }

    override fun getItemCount(): Int = binList.size


    // Function to show DatePickerDialog and set the selected date
    @RequiresApi(Build.VERSION_CODES.O)
    private fun showDatePickerDialog(editText: TextInputEditText, installation: ModelBinLocation, type: String): String {
        // Get the current date
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        // Show DatePickerDialog
        val datePickerDialog = DatePickerDialog(
            context, // Context
            { _, selectedYear, selectedMonth, selectedDay ->
                // Set the selected date into the TextInputEditText

                val formattedDate = "${selectedYear}-${selectedMonth + 1}-$selectedDay"
                val currentDate = LocalDateTime.now()
                // Convert formattedDate string to LocalDateTime
                val localDate = LocalDateTime.of(selectedYear, selectedMonth + 1, selectedDay, currentDate.hour, currentDate.minute, currentDate.second)

                // Format it to the required format
                val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")
                val date = localDate.format(formatter)

                // Set the formatted date to the EditText
                editText.setText(formattedDate)  // Set the date to the corresponding EditText

                // Update installation object based on the type
                if (type.equals("MFG", ignoreCase = true)) {
                    installation.ManufacturingDate = date.trim()
                } else {
                    installation.ExpiryDate = date.trim()
                }
            },
            year,
            month,
            day
        )
        datePickerDialog.show()

        return editText.text.toString().trim()
    }


}
