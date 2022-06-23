package com.wj.androidm3.business.ui.conversationincome

import android.media.Ringtone
import android.media.RingtoneManager
import com.wj.androidm3.R
import com.wj.androidm3.business.services.CONVERSATION_NOTIFICATION_ID
import com.wj.androidm3.databinding.FragmentConversationIncomeBinding
import com.wj.basecomponent.ui.BaseMVVMFragment
import com.wj.basecomponent.util.notification.cancelNotification

class ConversationIncomeFragment :
    BaseMVVMFragment<ConversationIncomeViewModel, FragmentConversationIncomeBinding>() {

    private var mRingtone: Ringtone? = null

    override fun firstCreateView() {
        mViewBinding?.viewModel = mViewModel
        playRingTone()
        mViewBinding?.run {
            handUpBtn.setOnClickListener {
                stopRingTone()
                requireActivity().finish()
                cancelNotification(requireActivity(), CONVERSATION_NOTIFICATION_ID)
            }
            answerBtn.setOnClickListener {
                stopRingTone()
                cancelNotification(requireActivity(), CONVERSATION_NOTIFICATION_ID)
            }
        }
    }


    private fun playRingTone() {
        val notificationRingTonUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        mRingtone = RingtoneManager.getRingtone(requireActivity(), notificationRingTonUri)
        mRingtone?.play()
    }

    private fun stopRingTone() {
        mRingtone?.run {
            if (isPlaying) {
                stop()
            }
        }
    }


    override fun getLayoutId() = R.layout.fragment_conversation_income

}