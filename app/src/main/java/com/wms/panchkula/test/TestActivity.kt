package com.wms.panchkula.test

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import com.wms.panchkula.databinding.ActivityTestBinding

class TestActivity : AppCompatActivity() {
    lateinit var binding : ActivityTestBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTestBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val outerItems = mutableListOf(
            OuterItem("Title 1", mutableListOf(InnerItem("Description 1.1"), InnerItem("Description 1.2"))),
            OuterItem("Title 2", mutableListOf(InnerItem("Description 2.1"), InnerItem("Description 2.2"))),
            OuterItem("Title 3", mutableListOf(InnerItem("Description 3.1"), InnerItem("Description 3.2"))),
            OuterItem("Title 4", mutableListOf(InnerItem("Description 4.1"), InnerItem("Description 4.2"))),
            OuterItem("Title 5", mutableListOf(InnerItem("Description 5.1"), InnerItem("Description 5.2"))),
            OuterItem("Title 6", mutableListOf(InnerItem("Description 6.1"), InnerItem("Description 6.2"))),
            OuterItem("Title 7", mutableListOf(InnerItem("Description 7.1"), InnerItem("Description 7.2"))),
            OuterItem("Title 8", mutableListOf(InnerItem("Description 8.1"), InnerItem("Description 8.2"))),
            OuterItem("Title 9", mutableListOf(InnerItem("Description 9.1"), InnerItem("Description 9.2"))),
            OuterItem("Title 10", mutableListOf(InnerItem("Description 10.1"), InnerItem("Description 10.2"))),
            OuterItem("Title 11", mutableListOf(InnerItem("Description 11.1"), InnerItem("Description 11.2"))),
            OuterItem("Title 12", mutableListOf(InnerItem("Description 12.1"), InnerItem("Description 12.2"))),
            OuterItem("Title 13", mutableListOf(InnerItem("Description 13.1"), InnerItem("Description 13.2"))),
            OuterItem("Title 14", mutableListOf(InnerItem("Description 14.1"), InnerItem("Description 14.2"))),
        )

        var outerAdapter = OuterAdapter(outerItems)
        binding.multirecyclerview.layoutManager = LinearLayoutManager(this)
        binding.multirecyclerview.adapter = outerAdapter

    }
}