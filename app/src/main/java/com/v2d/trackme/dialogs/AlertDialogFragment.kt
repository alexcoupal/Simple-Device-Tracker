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

class AlertDialogFragment : DialogFragment() {
    private lateinit var message: String
    private lateinit var listener: AlertDialogFragmentListener

    interface AlertDialogFragmentListener{
        fun onOkClick()
    }

    companion object {
        val MESSAGE: String = "message"

        fun newInstance(message: String): AlertDialogFragment {
            val f = AlertDialogFragment()

            val args = Bundle()
            args.putString(MESSAGE, message)
            f.arguments = args

            return f
        }
    }

    fun setListener(listener: AlertDialogFragmentListener){
        this.listener = listener
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        message = arguments!!.getString(MESSAGE)
    }
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        dialog!!.window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT)) //For rounded corner

        return inflater.inflate(R.layout.fragment_dialog_alert, container)
    }
    override fun onViewCreated(view: View, @Nullable savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val btnOk = view.findViewById<View>(R.id.buttonOk) as TextView

        val tvMessage = view.findViewById<View>(R.id.message) as TextView
        tvMessage.text = message

        btnOk.setOnClickListener {
            if(listener != null)
                listener.onOkClick()
            dismiss()
        }
    }
}