package com.example.customgamefield

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import kotlinx.android.synthetic.main.activity_main.*

lateinit var field : FieldView
lateinit var scoreTextView: TextView

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.i("FieldView", "onCreate")
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

    }
}
