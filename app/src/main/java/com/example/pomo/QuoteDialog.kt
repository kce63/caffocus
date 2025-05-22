package com.example.pomo

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.widget.Switch
import android.widget.TextView

class QuoteDialog(
    context: Context,
    private val quote: String,
    private val isSwitchOn: Boolean,
    private val onSwitchChanged: (Boolean) -> Unit
) : Dialog(context) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val quoteSwitch = findViewById<Switch>(R.id.quoteSwitch)
        quoteSwitch.isChecked = isSwitchOn

        quoteSwitch.setOnCheckedChangeListener { _, isChecked ->
            onSwitchChanged(isChecked)
        }
    }
}
