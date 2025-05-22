package com.example.pomo

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.progressindicator.CircularProgressIndicator

class MainActivity : AppCompatActivity() {

    private lateinit var timerText: TextView
    private lateinit var progressBar: CircularProgressIndicator
    private lateinit var quoteText: TextView
    private lateinit var playButton: ImageButton
    private lateinit var resetButton: ImageButton
    private lateinit var stopButton: ImageButton

    private val prefs by lazy { getSharedPreferences("settings", MODE_PRIVATE) }

    private var isRunning = false
    private var isWorkTime = true
    private var totalTimeInMillis = 25 * 60 * 1000L
    private var timeLeftInMillis = totalTimeInMillis
    private var countDownTimer: CountDownTimer? = null

    private val quotes = listOf(
        "성공은 매일 반복한 작은 노력들의 합이다.",
        "지금 하는 일이 미래의 나를 만든다.",
        "포기하지 마라. 위대한 일은 시간이 걸린다.",
        "시작이 반이다.",
        "매일 한 걸음이 먼 길을 만든다."
    )

    private val quoteHandler = Handler(Looper.getMainLooper())
    private val quoteRunnable = object : Runnable {
        override fun run() {
            if (prefs.getBoolean("show_quote", true)) {
                val random = quotes.random()
                quoteText.text = random
                quoteHandler.postDelayed(this, 5 * 60 * 1000L)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        timerText = findViewById(R.id.timerText)
        progressBar = findViewById(R.id.progressBar)
        quoteText = findViewById(R.id.quoteText)
        playButton = findViewById(R.id.play)
        resetButton = findViewById(R.id.rotate_ccw)
        stopButton = findViewById(R.id.power)

        playButton.setOnClickListener {
            if (isRunning) pauseTimer() else startTimer()
        }

        resetButton.setOnClickListener {
            resetTimer()
        }

        stopButton.setOnClickListener {
            stopTimerAndShowEndDialog()
        }

        quoteText.setOnClickListener {
            QuoteDialog(
                context = this,
                quote = quoteText.text.toString(),
                isSwitchOn = prefs.getBoolean("show_quote", true)
            ) { isChecked ->
                prefs.edit().putBoolean("show_quote", isChecked).apply()
                updateQuoteVisibility()
            }.show()
        }

        updateQuoteVisibility()
        updateTimerText()
        updateProgress()

        resetButton.visibility = View.GONE
        stopButton.visibility = View.GONE
    }

    private fun startTimer() {
        playButton.setImageResource(android.R.drawable.ic_media_pause)
        resetButton.visibility = View.VISIBLE
        stopButton.visibility = View.VISIBLE

        countDownTimer = object : CountDownTimer(timeLeftInMillis, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                timeLeftInMillis = millisUntilFinished
                updateTimerText()
                updateProgress()
            }

            override fun onFinish() {
                isRunning = false
                playButton.setImageResource(android.R.drawable.ic_media_play)
                val title = if (isWorkTime) "25분 종료" else "휴식 끝"
                val message = if (isWorkTime) "5분 휴식을 시작할까요?" else "다시 집중을 시작할까요?"
                showAlert(title, message) {
                    isWorkTime = !isWorkTime
                    totalTimeInMillis = if (isWorkTime) 25 * 60 * 1000L else 5 * 60 * 1000L
                    timeLeftInMillis = totalTimeInMillis
                    updateTimerText()
                    updateProgress()
                    startTimer()
                }
            }
        }.start()
        isRunning = true
    }

    private fun pauseTimer() {
        countDownTimer?.cancel()
        isRunning = false
        playButton.setImageResource(android.R.drawable.ic_media_play)
    }

    private fun resetTimer() {
        countDownTimer?.cancel()
        isRunning = false
        isWorkTime = true
        totalTimeInMillis = 25 * 60 * 1000L
        timeLeftInMillis = totalTimeInMillis
        updateTimerText()
        updateProgress()
        playButton.setImageResource(android.R.drawable.ic_media_play)
        resetButton.visibility = View.GONE
        stopButton.visibility = View.GONE
    }

    private fun stopTimerAndShowEndDialog() {
        countDownTimer?.cancel()
        isRunning = false
        isWorkTime = true
        totalTimeInMillis = 25 * 60 * 1000L
        timeLeftInMillis = totalTimeInMillis
        updateTimerText()
        updateProgress()
        playButton.setImageResource(android.R.drawable.ic_media_play)
        resetButton.visibility = View.GONE
        stopButton.visibility = View.GONE
        showAlert("타이머 종료", "홈으로 돌아갑니다.") {}
    }

    private fun showAlert(title: String, message: String, onConfirm: () -> Unit) {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setCancelable(false)
            .setPositiveButton("확인") { _, _ -> onConfirm() }
            .show()
    }

    private fun updateTimerText() {
        val minutes = (timeLeftInMillis / 1000) / 60
        val seconds = (timeLeftInMillis / 1000) % 60
        timerText.text = String.format("%02d:%02d", minutes, seconds)
    }

    private fun updateProgress() {
        val progress = ((totalTimeInMillis - timeLeftInMillis) / 1000).toInt()
        progressBar.max = (totalTimeInMillis / 1000).toInt()
        progressBar.progress = progress
    }

    private fun updateQuoteVisibility() {
        val show = prefs.getBoolean("show_quote", true)
        if (show) {
            val random = quotes.random()
            quoteText.text = random
            quoteText.visibility = TextView.VISIBLE
            quoteHandler.removeCallbacks(quoteRunnable)
            quoteHandler.postDelayed(quoteRunnable, 5 * 60 * 1000L)
        } else {
            quoteText.visibility = TextView.GONE
            quoteHandler.removeCallbacks(quoteRunnable)
        }
    }

    override fun onPause() {
        super.onPause()
        if (isRunning) {
            countDownTimer?.cancel()
            val currentTime = System.currentTimeMillis()
            prefs.edit().putLong("resume_time", currentTime).putLong("time_left", timeLeftInMillis).apply()
        }
    }

    override fun onResume() {
        super.onResume()
        if (isRunning) {
            val resumeTime = prefs.getLong("resume_time", System.currentTimeMillis())
            val previousLeft = prefs.getLong("time_left", totalTimeInMillis)
            val passed = System.currentTimeMillis() - resumeTime
            timeLeftInMillis = previousLeft - passed
            if (timeLeftInMillis < 0L) timeLeftInMillis = 0L
            startTimer()
        }
        if (prefs.getBoolean("show_quote", true)) {
            quoteHandler.postDelayed(quoteRunnable, 5 * 60 * 1000L)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        countDownTimer?.cancel()
        quoteHandler.removeCallbacks(quoteRunnable)
    }
}
