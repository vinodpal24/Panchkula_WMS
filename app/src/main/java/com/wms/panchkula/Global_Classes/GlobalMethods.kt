package com.wms.panchkula.Global_Classes

import android.app.DatePickerDialog
import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.text.Editable
import android.text.InputFilter
import android.text.Spannable
import android.text.SpannableString
import android.text.TextWatcher
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.WindowManager
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.wms.panchkula.R
import com.wms.panchkula.databinding.CustomBinlocationErrorDialogBinding
import com.wms.panchkula.databinding.CustomSuccessDialogBinding
import com.wms.panchkula.ui.issueForProductionOrder.Model.ProductionListModel
import com.wms.panchkula.ui.issueForProductionOrder.Model.ScanedOrderBatchedItems
import com.wms.panchkula.ui.production.model.batchCode.ProductionOrderStageModel
import es.dmoral.toasty.Toasty
import org.json.JSONObject
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.ParseException
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Date
import java.util.Locale

object GlobalMethods {
    const val pullRefreshTime = 2000
    var ProdDocEntry = ""
    open fun showMessage(context: Context?, message: String?) {
        Toasty.warning(context!!, message!!, Toast.LENGTH_SHORT).show()
    }

    open fun showError(context: Context?, message: String?) {
        Toasty.error(context!!, message!!, Toast.LENGTH_SHORT).show()
    }

    open fun showSuccess(context: Context?, message: String?) {
        Toasty.success(context!!, message!!, Toast.LENGTH_SHORT).show()
    }

    fun isVpnConnected(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networks = connectivityManager.allNetworks
        for (network in networks) {
            val caps = connectivityManager.getNetworkCapabilities(network)
            if (caps != null && caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) {
                return true
            }
        }
        return false
    }

    /*fun mapStageModelToListModel(stageModel: ProductionOrderStageModel): ProductionListModel {
        val productionOrderLines = arrayListOf<ProductionListModel.ProductionOrderLine>()

        stageModel.value.forEach { value ->
            value.ProductionOrdersStages.forEach { stage ->
                stage.ProductionOrderLines.forEach { line ->
                    val mappedLine = ProductionListModel.ProductionOrderLine(
                        isScanned = line.isScanned,
                        LineNumber = line.LineNumber.takeIf { it.isNotBlank() } ?: "",
                        ItemNo = line.ItemNo.takeIf { it.isNotBlank() } ?: "",
                        BaseQuantity = line.BaseQuantity,
                        IssueQuantity = line.IssueQuantity,
                        Warehouse = line.Warehouse.takeIf { it.isNotBlank() } ?: "",
                        ItemName = line.ItemName ?: "",
                        StageID = line.StageID ?: "",
                        PlannedQuantity = line.PlannedQuantity,
                        ItemCode = line.ItemCode.takeIf { it.isNotBlank() } ?: "",
                        batchList = line.batchList ?: mutableListOf(),
                        serialList = line.serialList ?: mutableListOf(),
                        noneList = line.noneList ?: mutableListOf(),
                        IssueFromModelList = line.IssueFromModelList ?: mutableListOf(),
                        binAllocationJSONs = line.binAllocationJSONs ?: arrayListOf()
                    )
                    productionOrderLines.add(mappedLine)
                }

            }
        }

        Log.i("PO_DATA","productionOrderLines global method: ${toPrettyJson(productionOrderLines)}")

        // Build the ProductionListModel (fill other values according to your needs)
        return ProductionListModel(
            odataMetadata = "",
            odataNextLink = "",
            value = arrayListOf(
                ProductionListModel.Value(
                    AbsoluteEntry = stageModel.value.firstOrNull()?.AbsoluteEntry ?: "",
                    DocumentNumber = stageModel.value.firstOrNull()?.DocumentNumber ?: "",
                    FGName = stageModel.value.firstOrNull()?.FGName ?: "",
                    SeriesCode = "",
                    ReceiptSeriesCode = "",
                    Series = "",
                    ItemNo = stageModel.value.firstOrNull()?.ItemNo ?: "",
                    ProductionOrderStatus = "",
                    ProductionOrderType = "",
                    PlannedQuantity = 0.0,
                    CompletedQuantity = 0.0,
                    RejectedQuantity = 0.0,
                    PostingDate = stageModel.value.firstOrNull()?.PostingDate ?: "",
                    DueDate = "",
                    ProductionOrderOriginEntry = null,
                    ProductionOrderOriginNumber = null,
                    ProductionOrderOrigin = "",
                    UserSignature = 0,
                    Remarks = stageModel.value.firstOrNull()?.Remarks,
                    ClosingDate = null,
                    ReleaseDate = null,
                    CustomerCode = null,
                    Warehouse = stageModel.value.firstOrNull()?.Warehouse ?: "",
                    InventoryUOM = null,
                    JournalRemarks = "",
                    TransactionNumber = null,
                    CreationDate = "",
                    Printed = "",
                    DistributionRule = "",
                    Project = null,
                    DistributionRule2 = "",
                    DistributionRule3 = "",
                    DistributionRule4 = "",
                    DistributionRule5 = "",
                    UoMEntry = 0,
                    StartDate = "",
                    ProductDescription = null,
                    Priority = 0,
                    RoutingDateCalculation = "",
                    UpdateAllocation = "",
                    SAPPassport = null,
                    AttachmentEntry = null,
                    U_PM = null,
                    U_Width = null,
                    U_GSM = null,
                    U_FLEX = null,
                    U_TBQ = null,
                    U_Length = null,
                    U_Cal = null,
                    U_QRCode = null,
                    U_QRPath = null,
                    ProductionOrderLines = productionOrderLines,
                    ProductionOrdersSalesOrderLines = listOf(),
                    ProductionOrdersStages = listOf(),
                    ProductionOrdersDocumentReferences = listOf()
                )
            )
        )
    }*/

    fun mapStageModelToListModel(stageModel: ProductionOrderStageModel): ProductionListModel {
        val values = stageModel.value.map { value ->
            val mappedStages = value.ProductionOrdersStages.map { stage ->
                val mappedLines = stage.ProductionOrderLines.map { line ->
                    ProductionListModel.ProductionOrderLine(
                        isScanned = line.isScanned,
                        LineNumber = line.LineNumber.takeIf { !it.isNullOrBlank() } ?: "",
                        ItemNo = line.ItemNo.takeIf { !it.isNullOrBlank() } ?: "",
                        BaseQuantity = line.BaseQuantity,
                        IssueQuantity = line.IssueQuantity,
                        Warehouse = line.Warehouse.takeIf { !it.isNullOrBlank() } ?: "",
                        ItemName = line.ItemName ?: "",
                        StageID = line.StageID ?: "",
                        PlannedQuantity = line.PlannedQuantity,
                        ItemCode = line.ItemCode.takeIf { !it.isNullOrBlank() } ?: "",
                        Batch = line.Batch.takeIf { !it.isNullOrBlank() } ?: "",
                        Serial = line.Serial.takeIf { !it.isNullOrBlank() } ?: "",
                        None = line.None.takeIf { !it.isNullOrBlank() } ?: "",
                        batchList = line.batchList ?: mutableListOf(),
                        serialList = line.serialList ?: mutableListOf(),
                        noneList = line.noneList ?: mutableListOf(),
                        IssueFromModelList = line.IssueFromModelList ?: mutableListOf(),
                        binAllocationJSONs = line.binAllocationJSONs ?: arrayListOf()
                    )
                }

                // Build each stage with its lines
                ProductionListModel.ProductionOrdersStage(
                    Father = stage.Father ?: "",
                    Name = stage.Name ?: "",
                    ProductionOrderLines = mappedLines,
                    SequenceNumber = "",
                    StageEntry = "",
                    StageId = stage.StageId ?: ""
                )
            }

            // Now build each value (with its stages preserved)
            ProductionListModel.Value(
                AbsoluteEntry = value.AbsoluteEntry ?: "",
                DocumentNumber = value.DocumentNumber ?: "",
                FGName = value.FGName ?: "",
                SeriesCode = "",
                ReceiptSeriesCode = "",
                Series = "",
                ItemNo = value.ItemNo ?: "",
                ProductionOrderStatus = "",
                ProductionOrderType = "",
                PlannedQuantity = value.PlannedQuantity.toDouble(),
                CompletedQuantity = 0.0,
                RejectedQuantity = 0.0,
                PostingDate = value.PostingDate ?: "",
                DueDate = "",
                ProductionOrderOriginEntry = null,
                ProductionOrderOriginNumber = null,
                ProductionOrderOrigin = "",
                UserSignature = 0,
                Remarks = value.Remarks,
                ClosingDate = null,
                ReleaseDate = null,
                CustomerCode = null,
                Warehouse = value.Warehouse ?: "",
                InventoryUOM = null,
                JournalRemarks = "",
                TransactionNumber = null,
                CreationDate = "",
                Printed = "",
                DistributionRule = "",
                Project = null,
                DistributionRule2 = "",
                DistributionRule3 = "",
                DistributionRule4 = "",
                DistributionRule5 = "",
                UoMEntry = 0,
                StartDate = "",
                ProductDescription = null,
                Priority = 0,
                RoutingDateCalculation = "",
                UpdateAllocation = "",
                SAPPassport = null,
                AttachmentEntry = null,
                U_PM = null,
                U_Width = null,
                U_GSM = null,
                U_FLEX = null,
                U_TBQ = null,
                U_Length = null,
                U_Cal = null,
                U_QRCode = null,
                U_QRPath = null,
                ProductionOrderLines = listOf(), // keep empty here, since lines are under stages
                ProductionOrdersSalesOrderLines = listOf(),
                ProductionOrdersStages = mappedStages, // ✅ stages with lines
                ProductionOrdersDocumentReferences = listOf()
            )
        }

        return ProductionListModel(
            odataMetadata = "",
            odataNextLink = "",
            value = ArrayList(values)
        )
    }


    fun TextInputLayout.setupDynamicHint(
        editText: TextInputEditText,
        innerHint: String,
        floatingLabel: String
    ) {
        // Initial state: no floating label, show placeholder
        this.hint = null
        editText.hint = innerHint

        editText.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                // On focus → show floating label + placeholder (if empty)
                this.hint = floatingLabel
                if (editText.text.isNullOrEmpty()) {
                    editText.hint = innerHint
                }
            } else {
                if (editText.text.isNullOrEmpty()) {
                    // On focus lost and empty → hide floating label, show placeholder
                    this.hint = null
                    editText.hint = innerHint
                }
            }
        }

        editText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (s.isNullOrEmpty()) {
                    if (editText.hasFocus()) {
                        // While focused but empty → floating label + placeholder
                        this@setupDynamicHint.hint = floatingLabel
                        editText.hint = innerHint
                    } else {
                        // Empty + no focus → placeholder only, no floating label
                        this@setupDynamicHint.hint = null
                        editText.hint = innerHint
                    }
                } else {
                    // Typing → floating label only
                    this@setupDynamicHint.hint = floatingLabel
                    editText.hint = null
                }
            }

            override fun afterTextChanged(s: Editable?) = Unit
        })
    }


    fun showBinLocationErrorDialog(
        context: Context,
        itemCode: String,
        warehouseCode: String,
        distNumber: String,
        cancelable: Boolean,
        onDismiss: (() -> Unit)? = null
    ) {
        val redColor = Color.RED

        val finalMsg =
            "Bin location is enabled, but the BinAbsEntry is not mapped for Item Code: $itemCode in Warehouse: $warehouseCode with Dist. Number: $distNumber. \n\nPlease assign a valid bin location before " +
                    "proceeding."
        val spannable = SpannableString(finalMsg)

        // Highlight and bold itemCode
        val itemCodeStart = finalMsg.indexOf(itemCode)
        val itemCodeEnd = itemCodeStart + itemCode.length
        spannable.setSpan(StyleSpan(Typeface.BOLD), itemCodeStart, itemCodeEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        spannable.setSpan(ForegroundColorSpan(redColor), itemCodeStart, itemCodeEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

        // Highlight and bold warehouseCode
        val warehouseStart = finalMsg.indexOf(warehouseCode)
        val warehouseEnd = warehouseStart + warehouseCode.length
        spannable.setSpan(StyleSpan(Typeface.BOLD), warehouseStart, warehouseEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        spannable.setSpan(ForegroundColorSpan(redColor), warehouseStart, warehouseEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

        // Highlight and bold distNumber
        val distStart = finalMsg.indexOf(distNumber)
        val distEnd = distStart + distNumber.length
        spannable.setSpan(StyleSpan(Typeface.BOLD), distStart, distEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        spannable.setSpan(ForegroundColorSpan(redColor), distStart, distEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

        // Set dialog
        val dialog = Dialog(context, R.style.Theme_Dialog)
        val layoutInflater = LayoutInflater.from(context)
        val dialogBinding = CustomBinlocationErrorDialogBinding.inflate(layoutInflater)
        dialog.setContentView(dialogBinding.root)

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.setCancelable(cancelable)

        dialogBinding.tvSuccessMsg.text = spannable
        dialog.setOnDismissListener {
            onDismiss?.invoke()
        }

        dialog.show()

        val lp = WindowManager.LayoutParams()
        lp.copyFrom(dialog.window!!.attributes)
        lp.width = WindowManager.LayoutParams.MATCH_PARENT
        lp.height = WindowManager.LayoutParams.WRAP_CONTENT
        lp.gravity = Gravity.BOTTOM
        dialog.window!!.attributes = lp
    }


    fun showSuccessDialog(
        context: Context,
        title: String,
        successMsg: String,
        docNum: String,
        cancelable: Boolean,
        onDismiss: (() -> Unit)? = null
    ) {
        val finalMsg = "$successMsg $docNum"
        val spannable = SpannableString(finalMsg)

// Apply bold style to docNum part only
        val startIndex = finalMsg.indexOf(docNum)
        val endIndex = startIndex + docNum.length
        spannable.setSpan(
            StyleSpan(Typeface.BOLD),  // Use Typeface.BOLD for bold
            startIndex,
            endIndex,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        val dialog = Dialog(context, R.style.Theme_Dialog)

        val layoutInflater = LayoutInflater.from(context)
        val dialogBinding = CustomSuccessDialogBinding.inflate(layoutInflater)
        dialog.setContentView(dialogBinding.root)

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.setCancelable(cancelable)

        dialogBinding.tvTitle.text = title
        dialogBinding.tvSuccessMsg.text = spannable
        dialog.setOnDismissListener {
            onDismiss?.invoke()
        }

        // Important: show first, then modify layout params
        dialog.show()

        val lp = WindowManager.LayoutParams()
        lp.copyFrom(dialog.window!!.attributes)
        lp.width = WindowManager.LayoutParams.MATCH_PARENT
        lp.height = WindowManager.LayoutParams.WRAP_CONTENT
        lp.gravity = Gravity.BOTTOM
        dialog.window!!.attributes = lp
    }


    fun String.toEditable(): Editable = Editable.Factory.getInstance().newEditable(this)

    fun convert_dd_MM_yyyy_into_yyyy_MM_dd(inputDate: String): String {
        val inputFormat = SimpleDateFormat("dd-MM-yyyy")
        val outputFormat = SimpleDateFormat("yyyy-MM-dd")

        val date = inputFormat.parse(inputDate)
        return outputFormat.format(date)
    }

    fun setTextStyleAndColor(textView: TextView, color: Int, isBold: Boolean = false, isItalic: Boolean = false) {
        // Set text color
        textView.setTextColor(color)

        // Determine style
        val style = when {
            isBold && isItalic -> Typeface.BOLD_ITALIC
            isBold -> Typeface.BOLD
            isItalic -> Typeface.ITALIC
            else -> Typeface.NORMAL
        }

        // Set text style
        textView.setTypeface(null, style)
    }


    fun updateItemType(batch: String, serial: String, none: String, view: TextView) {
        val itemType = when {
            batch == "Y" && serial == "N" && none == "N" -> "Batch"
            serial == "Y" && batch == "N" && none == "N" -> "Serial"
            none == "Y" && batch == "N" && serial == "N" -> "None"
            else -> "" // or some default value if needed
        }
        view.text = itemType
    }

    fun fixToTwoDecimalPlaces(value: Double): String {
        val rounded = BigDecimal(value).setScale(2, RoundingMode.HALF_UP)
        val formatter = DecimalFormat("0.00")
        return formatter.format(rounded)
    }

    fun fixToFourDecimalPlaces(value: Double): String {
        val rounded = BigDecimal(value).setScale(4, RoundingMode.HALF_UP)
        val formatter = DecimalFormat("0.0000")
        return formatter.format(rounded)
    }

    //todo remove digits after decimal..
    open fun changeDecimal(input: String): String? {
        /*var tempVar = ""
        if ( input.isNullOrEmpty()){
            tempVar = "0.0"
        }else{
            tempVar = input
        }
        val df = DecimalFormat("#.###")
        return df.format(tempVar.toDouble())*/

        val tempVar = input.toDoubleOrNull() ?: 0.0
        val df = DecimalFormat("#.######")
        return df.format(tempVar)
    }


    fun <T> toPrettyJson(data: T): String {
        val gsonPretty: Gson = GsonBuilder().setPrettyPrinting().create()
        return gsonPretty.toJson(data)
    }

    fun setDecimalLimit(textInputEditText: TextInputEditText, maxBeforeDecimal: Int, maxAfterDecimal: Int) {
        val pattern = Regex("^\\d{0,$maxBeforeDecimal}(\\.\\d{0,$maxAfterDecimal})?$")

        val filter = InputFilter { source, start, end, dest, dstart, dend ->
            val newText = dest.toString().substring(0, dstart) +
                    source.toString().substring(start, end) +
                    dest.toString().substring(dend)
            if (pattern.matches(newText)) null else ""
        }

        textInputEditText.filters = arrayOf(filter)
    }


    @RequiresApi(Build.VERSION_CODES.O)
    open fun getCurrentDateFormatted(): String {
        val currentDate = LocalDate.now()
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        return currentDate.format(formatter)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    open fun getCurrentDate_dd_MM_yyyy(): String {
        val currentDate = LocalDate.now()
        val formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy")
        return currentDate.format(formatter)
    }


    @RequiresApi(Build.VERSION_CODES.O)
    fun convert_yyyy_MM_dd_T_hh_mm_ss_into_ddMMYYYY(data: String): String {
        val inputDate = "2024-07-25T00:00:00"
        val formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy")

        // Parse the input date string to LocalDateTime
        val dateTime = LocalDateTime.parse(data)

        // Format the LocalDateTime to the desired format
        val formattedDate = dateTime.format(formatter)

        return formattedDate
    }

    fun disableDatesBetweenPoDateAndToday(context: Context, editText: EditText, poDateStr: String) {
        val dateFormatter = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())

        try {
            val poDate: Date = dateFormatter.parse(poDateStr)!!
            val today = Calendar.getInstance().time

            val c = Calendar.getInstance()
            val mYear = c.get(Calendar.YEAR)
            val mMonth = c.get(Calendar.MONTH)
            val mDay = c.get(Calendar.DAY_OF_MONTH)

            val datePickerDialog = DatePickerDialog(
                context,
                { _, year, monthOfYear, dayOfMonth ->
                    val selectedDateStr = String.format("%02d-%02d-%04d", dayOfMonth, monthOfYear + 1, year)
                    val selectedDate = dateFormatter.parse(selectedDateStr)

                    if (selectedDate != null && (selectedDate.before(poDate) || selectedDate.after(today))) {
                        Toast.makeText(
                            context,
                            "Please select a date between ${dateFormatter.format(poDate)} and ${dateFormatter.format(today)}",
                            Toast.LENGTH_LONG
                        ).show()
                        editText.setText("") // Clear invalid entry
                    } else {
                        editText.setText(dateFormatter.format(selectedDate!!))
                    }
                },
                mYear,
                mMonth,
                mDay
            )

            datePickerDialog.datePicker.minDate = poDate.time
            datePickerDialog.datePicker.maxDate = today.time
            datePickerDialog.setMessage(editText.hint.toString())
            datePickerDialog.show()

        } catch (e: ParseException) {
            e.printStackTrace()
            Toast.makeText(context, "Invalid PO date format. Use dd-MM-yyyy", Toast.LENGTH_SHORT).show()
        }
    }


    fun datePicker(context: Context, editText: EditText) {
        val c = Calendar.getInstance()
        val mYear = c.get(Calendar.YEAR)
        val mMonth = c.get(Calendar.MONTH)
        val mDay = c.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(
            context,
            DatePickerDialog.OnDateSetListener { _, year, monthOfYear, dayOfMonth ->
                val s = "$dayOfMonth-${monthOfYear + 1}-$year"
                val dateFormatter = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
                try {
                    val strDate = dateFormatter.parse(s)
                    editText.setText(dateFormatter.format(strDate))
                } catch (e: ParseException) {
                    e.printStackTrace()
                }
            }, mYear, mMonth, mDay
        )

        //datePickerDialog.datePicker.minDate = System.currentTimeMillis() - 1000
        datePickerDialog.setMessage(editText.hint.toString())
        datePickerDialog.show()
    }

    open fun sumBatchQuantity(position: Int, quantityHashMap: MutableList<String>): Double {
        var quantity = 0.000
        //TODO sum of order line batch quantities and store it in open quantity..
        var batchQuantityList: MutableList<String>
//        batchQuantityList = quantityHashMap.get("Item" + position)!!
        batchQuantityList = quantityHashMap
        if (batchQuantityList.isEmpty()) {
            quantity = 0.0
        } else {
            for (i in 0 until batchQuantityList.size) {
                var temp = batchQuantityList[i].toDouble()
                quantity += temp
            }
        }

        return quantity
    }


    open fun sumBatchQuantity(position: Int, batchQuantity: MutableList<String>, serialQuantity: MutableList<String>): Double {
        var totalQuantity = 0.0

        for (quantityStr in batchQuantity) {
            totalQuantity += quantityStr.toDoubleOrNull() ?: 0.0
        }

        for (quantityStr in serialQuantity) {
            totalQuantity += quantityStr.toDoubleOrNull() ?: 0.0
        }

        return totalQuantity
    }


    open fun sumBatchGrossWeight(position: Int, valueArrayList: ArrayList<ScanedOrderBatchedItems.Value>): Double {
        var quantity = 0.000
        //TODO sum of order line batch quantities and store it in open quantity..
        var batchQuantityList: ArrayList<ScanedOrderBatchedItems.Value>
        batchQuantityList = valueArrayList
        for (i in 0 until batchQuantityList.size) {
            var temp = batchQuantityList[i].U_GW
            quantity += temp
        }
        return quantity
    }


    open fun numberToK(number: String?): String {
        var num = number
        if (num == null || num.equals("null", ignoreCase = true)) {
            num = "00"
        } else if (num.isEmpty()) {
            num = "00"
        }

        val df = DecimalFormat("0.00")
        val amount = df.format(num.toDouble()).toDouble()
        val formattedNumber = df.format(amount)

        return formattedNumber
    }

    open fun separateDateAndTime123(dateTime: String): String {
        val dateTimeFormat = SimpleDateFormat("M/d/yyyy h:mm:ss a", Locale.getDefault())
        val dateFormat = SimpleDateFormat("M/d/yyyy", Locale.getDefault())
        // Split the string by space
        val parts = dateTime.split(" ")

        // parts[0] will be the date
        val date = parts[0]

        // parts[1] and parts[2] combined will be the time
        val time = parts[1] + " " + parts[2]

        // Return date and time as a Pair
        return date
    }

    /* fun <T> toSimpleJson(data: T): String {
         val gson = Gson() // No pretty printing
         return gson.toJson(data)
     }*/

    fun <T> toSimpleJson(data: T): String {
        val gson = GsonBuilder()
            .serializeNulls()
            .create()
        return gson.toJson(data)
    }

    fun logLargeString(tag: String?, str: String) {
        if (str.length > 4000) {
            Log.d(tag, "logLargeString1: " + str.substring(0, 4000))
            logLargeString(tag, str.substring(4000))
        } else {
            Log.d(tag, "logLargeString2: $str")
        }
    }


    fun calculateSumValues(jsonString: String): Pair<Double, Double> {
        val jsonObject = JSONObject(jsonString)
        val documentLines = jsonObject.getJSONArray("DocumentLines")

        var sumOfQty = 0.0
        var sumOfTax = 0.0

        for (i in 0 until documentLines.length()) {
            val lineItem = documentLines.getJSONObject(i)
            val quantity = lineItem.getDouble("Quantity")

            if (quantity > 0) {
                val price = lineItem.getDouble("Price")
                val rateField = lineItem.optDouble("RateField", 0.0)

                val lineTotal = quantity * price
                val taxAmount = lineTotal * rateField / 100

                sumOfQty += lineTotal
                sumOfTax += taxAmount
            }
        }

        return Pair(sumOfQty, sumOfTax)
    }

    fun separateDateAndTime(dateTime: String): String {
        // Define date-time and date-only formats
        val dateTimeFormat = SimpleDateFormat("M/d/yyyy h:mm:ss a", Locale.getDefault())
        val dateFormat = SimpleDateFormat("M/d/yyyy", Locale.getDefault())

        return try {
            when {
                dateTime.contains(" ") -> {
                    // Try parsing as date-time
                    dateTimeFormat.parse(dateTime)
                    val parts = dateTime.split(" ")
                    if (parts.size == 3) {
                        parts[0] // Return the date part
                    } else {
                        "NA" // Handle incorrect format
                    }
                }

                else -> {
                    // Try parsing as date-only
                    dateFormat.parse(dateTime)
                    dateTime // Return the date string
                }
            }
        } catch (e: Exception) {
            // Handle parsing exceptions
            "NA"//Invalid date or date-time format
        }
    }


    /*fun handleFailureError(context: Context, t: Throwable) {
        when (t) {
            is java.net.ConnectException -> {
                showError(context = context, "Connection failed: ${t.localizedMessage}")
            }

            is java.net.SocketTimeoutException -> {
                showError(context = context, "Timeout: ${t.localizedMessage}")
            }

            is java.net.UnknownHostException -> {
                showError(context = context, "No internet or DNS issue: ${t.localizedMessage}")
            }

            else -> {
                showError(context = context, "Error: ${t.localizedMessage}")
            }
        }
    }*/

    fun handleFailureError(context: Context, t: Throwable) {
        when (t) {
            // Network-related errors
            is java.net.ConnectException -> {
                showError(context = context, "Connection failed. ${t.localizedMessage}")
                // Optionally log the full error: Log.e("NetworkError", "Connection failed", t)
            }

            is java.net.SocketTimeoutException -> {
                showError(context = context, "Request timed out. ${t.localizedMessage}")
            }

            is java.net.UnknownHostException -> {
                showError(context = context, "No internet connection or server not found. Please check your network settings.")
            }


            // Application-specific runtime errors
            is NullPointerException -> {
                // This usually indicates a programming error.
                // For production, avoid showing raw NPE to users.
                // Instead, provide a generic error and log the details for debugging.
                showError(context = context, "An unexpected error occurred. Please try again.")
                // Log.e("AppError", "Null Pointer Exception", t)
                // Optionally, send crash report to a crash analytics tool like Crashlytics
            }

            is IndexOutOfBoundsException -> {
                showError(context = context, "There was an issue processing data. Please try again.")
                // Log.e("AppError", "Index Out Of Bounds Exception", t)
            }

            is IllegalArgumentException -> {
                showError(context = context, "Invalid input provided. ${t.localizedMessage}")
                // Log.e("AppError", "Illegal Argument Exception", t)
            }

            is IllegalStateException -> {
                showError(context = context, "The application is in an unexpected state. ${t.localizedMessage}")
                // Log.e("AppError", "Illegal State Exception", t)
            }
            // Fallback for unhandled runtime errors or other Throwables
            else -> {
                // For any other unexpected error, provide a generic message.
                // Always log the full exception for debugging purposes.
                showError(context = context, "An unkown error occurred: ${t.localizedMessage}. Please try again later.")
                // Log.e("GenericError", "Unhandled exception", t)
            }
        }
    }

}