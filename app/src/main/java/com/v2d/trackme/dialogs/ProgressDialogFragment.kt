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

class ProgressDialogFragment : DialogFragment() {
    private var cancelable: Boolean? = null
    private lateinit var listener: ProgressDialogFragmentListener

    interface ProgressDialogFragmentListener{
        fun onCancelClick()
    }

    companion object {
        val CANCELABLE: String = "cancelable"

        fun newInstance(cancelable: Boolean): ProgressDialogFragment {
            val f = ProgressDialogFragment()

            val args = Bundle()
            args.putBoolean(CANCELABLE, cancelable)
            f.arguments = args

            return f
        }
    }

    fun setListener(listener: ProgressDialogFragmentListener){
        this.listener = listener
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        cancelable = arguments!!.getBoolean(CANCELABLE)

        isCancelable = false
    }
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        dialog!!.window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT)) //For rounded corner

        return inflater.inflate(R.layout.fragment_dialog_progress, container)
    }
    override fun onViewCreated(view: View, @Nullable savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val btnCancel = view.findViewById<View>(R.id.buttonCancel) as TextView
        if(cancelable!!)
        {
            btnCancel.setOnClickListener {
                if(listener != null)
                    listener.onCancelClick()
                dismiss()
            }
        }
        else
            btnCancel.visibility = View.GONE

    }
}