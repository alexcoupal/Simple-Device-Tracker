package com.v2d.trackme.dialogs

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.*
import com.v2d.trackme.R
import android.widget.EditText
import androidx.annotation.Nullable
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProviders
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.v2d.trackme.utilities.Constants
import com.v2d.trackme.viewmodels.MainViewModel


class DeviceNameDialogFragment : DialogFragment() {

    private lateinit var currentName: String
    private lateinit var deviceUid: String
    private lateinit var model: MainViewModel
    private lateinit var textViewContent: EditText

    companion object {
        fun newInstance(currentName: String, deviceUid: String): DeviceNameDialogFragment {
            val f = DeviceNameDialogFragment()

            // Supply num input as an argument.
            val args = Bundle()
            args.putString("currentName", currentName)
            args.putString("deviceUid", deviceUid)
            f.arguments = args

            return f
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        currentName = arguments!!.getString("currentName")
        deviceUid = arguments!!.getString("deviceUid")

        model = activity?.run {
            ViewModelProviders.of(this).get(MainViewModel::class.java)
        } ?: throw Exception("Invalid Activity") as Throwable

    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        dialog!!.window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT)) //For rounded corner

        return inflater.inflate(R.layout.fragment_dialog_mycustom, container)
    }


    override fun onViewCreated(view: View, @Nullable savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val btnCancel = view.findViewById<View>(R.id.buttonCancel) as TextView
        val btnAccept = view.findViewById<View>(R.id.buttonAccept) as TextView

        textViewContent = view.findViewById<View>(R.id.editTextContent) as EditText
        textViewContent.setText(currentName)

        btnCancel.setOnClickListener {
            textViewContent.hideKeyboard()
            dismiss()
        }

        btnAccept.setOnClickListener {
            if(!currentName.equals(textViewContent.text.toString()))
                checkIsAvailable(textViewContent.text.toString())
            else {
                textViewContent.hideKeyboard()
                dismiss()
            }
        }

    }
    fun View.hideKeyboard() {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(windowToken, 0)
    }
    private fun checkIsAvailable(newName:String) {

        val database = FirebaseDatabase.getInstance().getReference(Constants.DATABASE_REF)

        val postListener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                // Get Post object and use the values to update the UI
                if(dataSnapshot.hasChildren()) {
                    for (data: DataSnapshot in dataSnapshot.children) {
                        if (data.child(Constants.DB_DEVICENAME).value.toString().equals(newName)) {
                            //It Already exist. Can't use that name
                            textViewContent.setError("This name already exist.")
                            return
                        }
                    }
                }

                //Doesn't exist
                //Update UI
                model.myDeviceName.value = newName

                dismiss()
            }

            override fun onCancelled(databaseError: DatabaseError) {
                // Getting Post failed, log a message
                Log.w(Constants.TAG, "loadPost:onCancelled", databaseError.toException())
            }
        }

        database.addListenerForSingleValueEvent(postListener)
    }

}