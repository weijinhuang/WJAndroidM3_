package com.wj.androidm3.business.countdown.ui

import android.app.Activity
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.media.projection.MediaProjectionConfig
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.ResultReceiver
import android.view.View
import android.view.WindowManager
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.wj.androidm3.R
import com.wj.androidm3.business.countdown.screenshot.CountdownScreenshotService
import com.wj.androidm3.databinding.ActivityAddCountdownBinding

class AddCountdownActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAddCountdownBinding
    private var captureInProgress = false
    private var pendingSeconds = 0
    private var pendingName: String? = null

    private val screenCaptureLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode != Activity.RESULT_OK || result.data == null) {
            showCaptureError(getString(R.string.countdown_capture_denied))
            return@registerForActivityResult
        }
        hideDialogForCapture()
        try {
            CountdownScreenshotService.capture(
                this,
                result.resultCode,
                requireNotNull(result.data),
                captureResultReceiver,
                pendingSeconds,
                pendingName
            )
        } catch (error: Throwable) {
            showCaptureError(error.message ?: getString(R.string.countdown_capture_failed))
        }
    }

    private val captureResultReceiver = object : ResultReceiver(Handler(Looper.getMainLooper())) {
        override fun onReceiveResult(resultCode: Int, resultData: Bundle?) {
            when (resultCode) {
                CountdownScreenshotService.RESULT_SUCCESS -> {
                    val path = resultData?.getString(CountdownScreenshotService.KEY_SCREENSHOT_PATH)
                    if (path == null) {
                        showCaptureError(getString(R.string.countdown_capture_failed))
                    } else {
                        finish()
                    }
                }
                else -> showCaptureError(
                    resultData?.getString(CountdownScreenshotService.KEY_ERROR)
                        ?: getString(R.string.countdown_capture_failed)
                )
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setFinishOnTouchOutside(false)
        binding = ActivityAddCountdownBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val twoDigits = android.widget.NumberPicker.Formatter { value -> "%02d".format(value) }
        binding.minutePicker.minValue = 0
        binding.minutePicker.maxValue = 59
        binding.minutePicker.setFormatter(twoDigits)
        binding.minutePicker.value = DEFAULT_MINUTES

        val selectableSeconds = (0..55 step SECOND_STEP_SECONDS).toList()
        binding.secondPicker.minValue = 0
        binding.secondPicker.maxValue = selectableSeconds.lastIndex
        binding.secondPicker.displayedValues = selectableSeconds
            .map { value -> "%02d".format(value) }
            .toTypedArray()
        binding.secondPicker.value = DEFAULT_SECONDS / SECOND_STEP_SECONDS

        binding.cancelButton.setOnClickListener { finish() }
        binding.confirmButton.setOnClickListener { requestScreenshotAndCreate() }
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (!captureInProgress) {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }

    private fun requestScreenshotAndCreate() {
        val selectedSeconds = binding.secondPicker.value * SECOND_STEP_SECONDS
        val totalSeconds = binding.minutePicker.value * 60 + selectedSeconds
        if (totalSeconds <= 0) {
            binding.errorText.visibility = View.VISIBLE
            return
        }
        binding.errorText.visibility = View.GONE
        pendingSeconds = totalSeconds
        pendingName = binding.countdownNameInput.text?.toString()?.trim()?.takeIf { it.isNotEmpty() }
        captureInProgress = true
        binding.confirmButton.isEnabled = false
        binding.cancelButton.isEnabled = false

        val projectionManager = getSystemService(MediaProjectionManager::class.java)
        val captureIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            projectionManager.createScreenCaptureIntent(MediaProjectionConfig.createConfigForDefaultDisplay())
        } else {
            projectionManager.createScreenCaptureIntent()
        }
        screenCaptureLauncher.launch(captureIntent)
    }

    private fun hideDialogForCapture() {
        binding.root.visibility = View.INVISIBLE
        window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
    }

    private fun showCaptureError(message: String) {
        captureInProgress = false
        binding.root.visibility = View.VISIBLE
        window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        window.attributes = window.attributes.apply { dimAmount = 0.32f }
        window.setBackgroundDrawable(ColorDrawable(Color.WHITE))
        binding.errorText.text = message
        binding.errorText.visibility = View.VISIBLE
        binding.confirmButton.isEnabled = true
        binding.cancelButton.isEnabled = true
    }

    companion object {
        private const val DEFAULT_MINUTES = 5
        private const val DEFAULT_SECONDS = 30
        private const val SECOND_STEP_SECONDS = 5
    }
}
