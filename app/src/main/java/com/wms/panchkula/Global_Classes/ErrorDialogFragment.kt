package com.wms.panchkula.Global_Classes

import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.wms.panchkula.Adapter.ErrorDialogAdapter
import com.wms.panchkula.Model.ErrorItemDetails
import com.wms.panchkula.R
import com.wms.panchkula.databinding.DialogErrorItemsBinding

class ErrorDialogFragment(private val value: ArrayList<ErrorItemDetails>) : DialogFragment() {
    private var _binding: DialogErrorItemsBinding? = null
    private val binding get() = _binding!!
    private var errorDialogAdapter: ErrorDialogAdapter? = null


    companion object {
        const val TAG = "ReturnItemDialogFragment"
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_FRAME, R.style.CustomDialog)
        //isCancelable = true
    }

    @SuppressLint("SetTextI18n")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        try {
            _binding = DialogErrorItemsBinding.inflate(inflater, container, false)
            return binding.root
        } catch (e: Exception) {
            Log.e("DialogError", "Inflation failed: ${e.localizedMessage}")
            return super.onCreateView(inflater, container, savedInstanceState)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews()
        clickListeners()
    }

    private fun clickListeners() {
        binding.chipOk.setOnClickListener {
            dismiss()
        }
    }

    private fun initViews() {
        setAdapter()
    }

    private fun setAdapter() {
        binding.apply {
            rvErrorDialog.run {
                layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
                errorDialogAdapter = ErrorDialogAdapter(requireContext(), value)
                adapter = errorDialogAdapter
                errorDialogAdapter?.notifyDataSetChanged()
                setHasFixedSize(true)
            }
        }
    }


    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)/*val window = dialog?.window
            // Set the margin by adjusting the window attributes
            val params: WindowManager.LayoutParams = window?.attributes!!
            params.horizontalMargin = 0.05f // 5% margin on both sides (you can adjust this)
            window.attributes = params*/
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

}