package com.maaransystems.eyetime

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.NumberPicker

class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        val minutesPicker = findViewById<NumberPicker>(R.id.minutesPicker)
        minutesPicker.minValue = 1
        minutesPicker.maxValue = 59
        minutesPicker.value = 10
        // Optionally handle value changes
        minutesPicker.setOnValueChangedListener { picker, oldVal, newVal ->
            // Handle the new selected value
            println("Selected Minute: $newVal")
        }
    }
}