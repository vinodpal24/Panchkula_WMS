package com.wms.panchkula.ui.production.adapter.rfp

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.content.Context
import android.os.Build
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AutoCompleteTextView
import androidx.annotation.RequiresApi
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.RecyclerView
import com.wms.panchkula.databinding.RvItemDynamicFieldBinding
import com.google.android.material.textfield.TextInputEditText
import com.google.gson.GsonBuilder
import com.pixplicity.easyprefs.library.Prefs
import com.wms.panchkula.Global_Classes.AppConstants
import com.wms.panchkula.Global_Classes.GlobalMethods
import com.wms.panchkula.Model.ModelBinLocation
import com.wms.panchkula.Model.OtpErrorModel
import com.wms.panchkula.Retrofit_Api.ApiConstantForURL
import com.wms.panchkula.Retrofit_Api.QuantityNetworkClient
import com.wms.panchkula.ui.inventoryOrder.Model.DefaultToBinLocationModel
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.IOException
import java.net.SocketTimeoutException
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Calendar


class DynamicFieldRFPAdapter(
    private val context: Context,
    private val warehouseCode: String,
    private val binLocationList: List<String>,
    private val binAbs: List<String>,
    private val binList: MutableList<ModelBinLocation>,
    private val onRemoveItem: (Int) -> Unit,
    private val scanType: String,
    private val binManeged: String
) : RecyclerView.Adapter<DynamicFieldRFPAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: RvItemDynamicFieldBinding) :
        RecyclerView.ViewHolder(binding.root) {

        //private var warehouseCode = ""

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
            callToBibLocationApi(installation, binding.acBinLocation,warehouseCode)
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

           /* val adapter = ArrayAdapter(context, android.R.layout.simple_dropdown_item_1line, binLocationList)
            binding.acBinLocation.setAdapter(adapter)
            binding.acBinLocation.threshold = 1*/

            val currentCode = installation.binLocationCode?.trim()

           /* if (currentCode.isNullOrBlank()) {
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
            }*/

            /*binding.acBinLocation.setOnItemClickListener { _, _, position, _ ->
                installation.binLocationCode = binAbs[position].trim()
                binding.acBinLocation.setText(binLocationList[position], false)
                Log.i(
                    "BIN_SELECTION",
                    "Selection => BinAbsEntry: ${binAbs[position]}, BinLocation: ${binLocationList[position]}"
                )
            }*/
        }
    }

    private fun callToBibLocationApi(installation: ModelBinLocation, acBinLocation: AutoCompleteTextView,warehouseCode: String) {
        val apiConfig = ApiConstantForURL()
        QuantityNetworkClient.updateBaseUrlFromConfig(apiConfig)
        val networkClient = QuantityNetworkClient.create(context)
        networkClient.getToBinLocationByWarehouse(warehouseCode).apply {
            enqueue(object : Callback<DefaultToBinLocationModel> {
                override fun onResponse(call: Call<DefaultToBinLocationModel>, response: Response<DefaultToBinLocationModel>) {
                    try {
                        if (response.isSuccessful) {
                            val toBinLocationModel = response.body()!!.value.getOrNull(0)
                            installation.binLocationCode = toBinLocationModel?.AbsEntry.toString()
                            acBinLocation.setText(toBinLocationModel?.BinCode.toString(),false)

                        } else {

                            Prefs.clear()

                            val gson1 = GsonBuilder().create()
                            var mError: OtpErrorModel
                            try {
                                val s = response.errorBody()!!.string()
                                mError = gson1.fromJson(s, OtpErrorModel::class.java)
                                if (mError.error.code.equals(400)) {
                                    GlobalMethods.showError(context, mError.error.message.value)
                                }
                                if (mError.error.message.value != null) {
                                    GlobalMethods.showError(context, mError.error.message.value)
                                    Log.e("json_error------", mError.error.message.value)
                                }
                            } catch (e: IOException) {
                                e.printStackTrace()
                            }
                        }

                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                override fun onFailure(call: Call<DefaultToBinLocationModel>, t: Throwable) {
                    Log.e("login_api_failure-----", t.toString())
                    when (t) {
                        is SocketTimeoutException -> {
                            GlobalMethods.showError(context, "Connection timed out. Please try again.")
                        }

                        is IOException -> {
                            GlobalMethods.showError(context, "Network error. Please check your internet connection.")
                        }

                        else -> {
                            GlobalMethods.showError(context, "Something went wrong: ${t.localizedMessage}")
                        }
                    }
//                                Prefs.clear()
                    Log.e("DBURL==>", "onFailure: " + Prefs.getString(AppConstants.DBUrl, ""))

                    //Toast.makeText(this@PurchaseTransferLinesActivity, t.message, Toast.LENGTH_SHORT)
                }

            })
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
