package com.wj.androidm3.business.ui.main.fragment

import android.app.TimePickerDialog
import android.text.Editable
import android.text.TextWatcher
import com.wj.androidm3.R
import com.wj.androidm3.databinding.FragmentCalcChargeTimeBinding
import com.wj.basecomponent.ui.BaseMVVMFragment
import com.wj.basecomponent.view.WJNumberPickerDialog

/**
 *@Create by H.W.J 2024/10/21/021
 */
class CalcChargeTimeFragment : BaseMVVMFragment<CalcChargeTimeViewModel, FragmentCalcChargeTimeBinding>() {

    override fun firstCreateView() {
        mViewBinding?.run {
            viewModel = mViewModel

            startPercent.setOnClickListener {
                showNumberPicker(mViewModel.startPercent) {
                    mViewModel.startPercent = it
                    mViewModel.calcEndTime()
                }
            }
            endPercent.setOnClickListener {
                showNumberPicker(mViewModel.endPercent) {
                    mViewModel.endPercent = it
                    mViewModel.calcEndTime()
                }
            }

            startTime.setOnClickListener {
                val dialog = TimePickerDialog(
                    requireContext(),
                    { view, hourOfDay, minute ->
                        mViewModel.startHour = hourOfDay
                        mViewModel.startMinute = minute
                        mViewModel.calcEndTime()
                    },
                    mViewModel.startHour,
                    mViewModel.startMinute,
                    true
                )
                dialog.show()
            }
            endTime.setOnClickListener {
                val dialog = TimePickerDialog(
                    requireContext(),
                    { view, hourOfDay, minute ->
                        mViewModel.endHour = hourOfDay
                        mViewModel.endMinute = minute
                        mViewModel.calcStartTime()
                    },
                    mViewModel.startHour,
                    mViewModel.startMinute,
                    true
                )
                dialog.show()
            }

            mViewModel.calcEndTime()
        }
    }

    private fun showNumberPicker(originalNumber: Int, onNumberSelect: (Int) -> Unit) {
        val dialog = WJNumberPickerDialog.newInstance(originalNumber)
        dialog.setOnConfirmListener {
            onNumberSelect.invoke(it)
        }
        dialog.show(parentFragmentManager, javaClass.simpleName)
    }

    private fun showTimePick(timeSelect: TimePickerDialog.OnTimeSetListener) {
        val dialog = TimePickerDialog(
            requireContext(),
            { view, hourOfDay, minute ->
                mViewModel.startHour = hourOfDay
                mViewModel.endMinute = minute
                mViewModel.calcStartTime()
            }, mViewModel.startHour, mViewModel.startMinute, true
        )
        dialog.show()
    }


    override fun getLayoutId(): Int {
        return R.layout.fragment_calc_charge_time
    }
}