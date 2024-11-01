package com.wj.basecomponent.view

import android.content.DialogInterface
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.DatePicker
import androidx.fragment.app.DialogFragment
import com.wj.basecomponent.databinding.DialogNumberPickerBinding

/**输入对话框
 *@Create by H.W.J 2023/4/20/020
 */
class WJNumberPickerDialog() : DialogFragment() {

    private var mOnConfirmListener: ((number:Int) -> Unit)? = null

    var percent = 1

    private var mBinding: DialogNumberPickerBinding? = null

    companion object {

        @JvmStatic
        fun newInstance(percent:Int): WJNumberPickerDialog {
            val bundle = Bundle()
            bundle.putInt("number", percent)
            val dialog = WJNumberPickerDialog()
            dialog.arguments = bundle
            return dialog
        }
    }


    var mOnConfirmListener2: DialogInterface.OnClickListener? = null
    var mOnCancelListener: DialogInterface.OnClickListener? = null

    fun setOnConfirmListener(onConfirmListener: (number:Int) -> Unit) {
        mOnConfirmListener = onConfirmListener
    }
    fun setOnCancelListener(onCancelListener: DialogInterface.OnClickListener?){
        this.mOnCancelListener = onCancelListener
    }

    fun setOnConfirmListener(onConfirmListener2: DialogInterface.OnClickListener?) {
        mOnConfirmListener2 = onConfirmListener2
    }


    override fun onStart() {
        super.onStart()
        dialog?.window?.let {
            it.attributes.dimAmount = 0.55f
            it.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            it.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }
    }
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        mBinding = DialogNumberPickerBinding.inflate(inflater, container, false)
        mBinding?.run {

            arguments?.let {
                percent = it.getInt("number",1)
            }
//            val numbers = mutableListOf<String>()
//            for(i in 1 .. 100){
//                numbers.add("$i")
//            }
//            numberPicker.displayedValues = numbers.toTypedArray()
            numberPicker.maxValue = 100
            numberPicker.minValue = 1
            numberPicker.value = percent
//            numberPicker.descendantFocusability = DatePicker.FOCUS_BEFORE_DESCENDANTS
            numberPicker.setOnValueChangedListener { picker, oldVal, newVal ->
                percent = newVal
            }

            confirmButton.setOnClickListener {
                mOnConfirmListener?.invoke(percent)
                mOnConfirmListener2?.onClick(dialog, 1)
                dismiss()
            }
            cancelButton.setOnClickListener {
                mOnCancelListener?.onClick(dialog,1)
                dismiss()
            }
        }





        return mBinding?.root
    }

}