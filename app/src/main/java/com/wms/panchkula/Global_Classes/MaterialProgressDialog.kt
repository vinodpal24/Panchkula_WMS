package com.wms.panchkula.Global_Classes

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.Window

class MaterialProgressDialog(context: Context):Dialog(context, com.wms.panchkula.R.style.LoadingDialogTheme){

    private val mContext: Context = context

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        val inflater = mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val inflateView: View = inflater.inflate(com.wms.panchkula.R.layout.loader, findViewById(com.wms.panchkula.R.id.loading_container))
        setCancelable(false)
        setContentView(inflateView)
    }

}
