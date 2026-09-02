package com.wj.androidm3.business.countdown.ui

import android.os.Bundle
import android.graphics.Color
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.wj.androidm3.R
import com.wj.androidm3.business.countdown.data.CountdownEntity
import com.wj.androidm3.business.countdown.data.CountdownRepository
import com.wj.androidm3.business.countdown.data.CountdownScreenshotStore
import com.wj.androidm3.business.countdown.data.CountdownStatus
import com.wj.androidm3.business.countdown.data.CountdownTime
import com.wj.androidm3.databinding.ActivityCountdownListBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CountdownListActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCountdownListBinding
    private lateinit var adapter: CountdownListAdapter
    private val repository by lazy { CountdownRepository.getInstance(this) }
    private val screenshotStore by lazy { CountdownScreenshotStore(this) }
    private var latestTasks: List<CountdownEntity> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCountdownListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.backButton.setOnClickListener { finish() }

        adapter = CountdownListAdapter(
            screenshotStore = screenshotStore,
            onAction = ::handleTaskAction,
            onScreenshot = ::showScreenshot,
            onDelete = ::confirmDelete
        )
        binding.countdownList.layoutManager = LinearLayoutManager(this)
        binding.countdownList.adapter = adapter

        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                repository.deleteExpiredAndCompleted()
            }
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    repository.observeAll().collect { tasks ->
                        latestTasks = tasks
                        renderTasks(System.currentTimeMillis())
                        binding.emptyText.visibility = if (tasks.isEmpty()) View.VISIBLE else View.GONE
                    }
                }
                launch {
                    while (true) {
                        renderTasks(System.currentTimeMillis())
                        delay(1_000L)
                    }
                }
            }
        }
    }

    private fun renderTasks(nowEpochMs: Long) {
        adapter.updateClock(nowEpochMs)
        val sortedTasks = CountdownTime.sortedByRemaining(latestTasks, nowEpochMs)
        if (adapter.currentList != sortedTasks) {
            adapter.submitList(sortedTasks)
        }
    }

    private fun handleTaskAction(task: CountdownEntity) {
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                when (task.status) {
                    CountdownStatus.PENDING -> repository.start(task.id)
                    CountdownStatus.RUNNING -> repository.pause(task.id)
                    CountdownStatus.PAUSED -> repository.resume(task.id)
                    CountdownStatus.COMPLETED -> Unit
                }
            }
        }
    }

    private fun confirmDelete(task: CountdownEntity) {
        MaterialAlertDialogBuilder(this)
            .setTitle("删除${task.displayName}？")
            .setMessage("删除后无法恢复。")
            .setNegativeButton("取消", null)
            .setPositiveButton("删除") { _, _ ->
                lifecycleScope.launch(Dispatchers.IO) {
                    repository.delete(task.id)
                }
            }
            .show()
    }

    private fun showScreenshot(task: CountdownEntity) {
        val path = task.screenshotPath ?: return
        val bitmap = screenshotStore.decodeSampled(
            path,
            resources.displayMetrics.widthPixels,
            resources.displayMetrics.heightPixels
        )
        if (bitmap == null) {
            Toast.makeText(this, R.string.countdown_screenshot_missing, Toast.LENGTH_SHORT).show()
            return
        }
        val padding = (16 * resources.displayMetrics.density).toInt()
        val imageView = ImageView(this).apply {
            setImageBitmap(bitmap)
            setBackgroundColor(Color.BLACK)
            adjustViewBounds = true
            scaleType = ImageView.ScaleType.FIT_CENTER
            setPadding(padding, padding, padding, padding)
        }
        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle(task.displayName)
            .setView(imageView)
            .setPositiveButton(R.string.close, null)
            .create()
        dialog.setOnDismissListener {
            imageView.setImageDrawable(null)
            bitmap.recycle()
        }
        dialog.show()
    }
}
