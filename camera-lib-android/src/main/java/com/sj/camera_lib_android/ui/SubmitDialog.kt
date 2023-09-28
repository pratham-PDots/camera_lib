package com.sj.camera_lib_android.ui
/**
 * @author Saurabh Kumar 11 September 2023
 * **/
import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import androidx.fragment.app.DialogFragment

class SubmitDialog(
  val prompt: String,
  val yesText: String,
  val noText: String,
  val onClick: () -> Unit
) : DialogFragment() {

  override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
    return context?.let {
      // Use the Builder class for convenient dialog construction
      val builder = AlertDialog.Builder(it)
      builder.setMessage(prompt)
        .setPositiveButton(yesText,
          DialogInterface.OnClickListener { dialog, id ->
            onClick()
          })
        .setNegativeButton(noText,
          DialogInterface.OnClickListener { dialog, id ->
            dismiss()
          })
      // Create the AlertDialog object and return it
      builder.create()
    } ?: throw IllegalStateException("Activity cannot be null")
  }
}