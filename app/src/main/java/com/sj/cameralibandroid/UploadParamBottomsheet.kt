package com.sj.cameralibandroid

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import com.google.android.material.bottomsheet.BottomSheetDialogFragment


class UploadParamBottomsheet : BottomSheetDialogFragment() {

    interface OnSaveClickListener {
        fun onSaveClicked(key: String, value: String)
    }

    private var onSaveClickListener: OnSaveClickListener? = null
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view: View = inflater.inflate(R.layout.upload_param_bottomsheet, container, false)

        // Handle the Save button click
        val saveButton = view.findViewById<Button>(R.id.saveButton)
        saveButton.setOnClickListener { // Retrieve the key and value entered in the EditTexts
            val keyEditText = view.findViewById<EditText>(R.id.keyEditText)
            val valueEditText = view.findViewById<EditText>(R.id.valueEditText)
            val key = keyEditText.text.toString()
            val value = valueEditText.text.toString()

            onSaveClickListener?.onSaveClicked(key, value)



            // Handle the key-value pair (e.g., save it to your data structure)
            // You can implement your logic here

            // Dismiss the bottom sheet
            dismiss()
        }
        return view
    }

    fun setOnSaveClickListener(listener: OnSaveClickListener) {
        onSaveClickListener = listener
    }

}
