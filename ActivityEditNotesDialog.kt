package com.agricolum.ui.fragment

import android.widget.EditText
import com.agricolum.ui.fragment.ActivityEditNotesDialog.NotesDialogListener
import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.view.View
import com.agricolum.ui.activities.MainActivity
import com.agricolum.R
import android.view.WindowManager
import androidx.fragment.app.DialogFragment
import com.agricolum.interfaces.Holder

class ActivityEditNotesDialog : DialogFragment() {
    var _v: View? = null
    var mNotesText: EditText? = null

    // Use this instance of the interface to deliver action events
    var mListener: NotesDialogListener? = null
    private var mActivity: Activity? = null
    private var mNotes: String? = null

    /*
     * The activity that creates an instance of this dialog fragment must
     * implement this interface in order to receive event callbacks. Each method
     * passes the DialogFragment in case the host needs to query it.
     */
    interface NotesDialogListener {
        fun onSaveNote(note: String?)
    }

    fun setListener(listener: NotesDialogListener?) {
        mListener = listener
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onAttach(activity: Activity) {
        super.onAttach(activity)
        mActivity = activity
    }

    override fun onDetach() {
        super.onDetach()
        mActivity = null
    }

    override fun onDestroy() {
        super.onDestroy()
        (mActivity as MainActivity?)!!.showingDialog = false
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(activity)

        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout
        _v = requireActivity().layoutInflater.inflate(R.layout.dialog_activityedit_notes, null)

        // set notes
        mNotesText = _v?.findViewById(R.id.editText)
        mNotesText?.setText(mNotes)
        _v?.findViewById<View>(R.id.dlg_activityEdit_okBtn)?.setOnClickListener {
            requireActivity().window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)
            val note: String? = note
            if (note != null) {
                mListener!!.onSaveNote(note)
            }
        }
        builder.setView(_v)
        builder.create()
        return builder.create()
    }

    fun showWorking(b: Boolean) {
        if (_v != null) {
            val v = _v!!.findViewById<View>(R.id.progress)
            if (b) {
                v.visibility = View.VISIBLE
            } else {
                v.visibility = View.GONE
            }
        }
    }

    fun showData(notes: String?) {
        mNotes = notes ?: ""
    }

    fun showError(error: Int) {
        (mActivity as Holder?)!!.showError(error)
    }

    val note: String
        get() = mNotesText!!.text.toString()
}