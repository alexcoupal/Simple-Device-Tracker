package com.v2d.trackme.dialogs

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.Nullable
import androidx.fragment.app.DialogFragment
import com.v2d.trackme.R

class ConfirmDialogFragment : DialogFragment() {
    private lateinit var message: String
    private lateinit var listener: ConfirmDialogFragmentListener

    interface ConfirmDialogFragmentListener{
        fun onPositiveClick()
        fun onNegativeClick()
    }

    companion object {
        val MESSAGE: String = "message"

        fun newInstance(message: String): ConfirmDialogFragment {
            val f = ConfirmDialogFragment()

            val args = Bundle()
            args.putString(MESSAGE, message)
            f.arguments = args

            return f
        }
    }

    fun setListener(listener: ConfirmDialogFragmentListener){
        this.listener = listener
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        message = arguments!!.getString(MESSAGE)
    }
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        dialog!!.window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT)) //For rounded corner

        return inflater.inflate(R.layout.fragment_dialog_confirm, container)
    }
    override fun onViewCreated(view: View, @Nullable savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val btnCancel = view.findViewById<View>(R.id.buttonCancel) as TextView
        val btnAccept = view.findViewById<View>(R.id.buttonOk) as TextView

        val tvMessage = view.findViewById<View>(R.id.message) as TextView
        tvMessage.text = message

        btnCancel.setOnClickListener {
            if(listener != null)
                listener.onNegativeClick()
            dismiss()
        }

        btnAccept.setOnClickListener {
            dismiss()
            if(listener != null)
                listener.onPositiveClick()
        }
    }
}