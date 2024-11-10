package com.example.pomodo_ver3

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.pomodo_ver3.R
import java.util.Calendar

class CalendarActivity : AppCompatActivity() {

    private val emojiMap = mutableMapOf<Int, Int>() // 날짜와 선택된 이모지 저장

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main) // 레이아웃 파일 수정된 파일 이름

        val calendarGrid = findViewById<GridLayout>(R.id.calendarGrid)
        addDatesToCalendar(calendarGrid)
    }

    private fun addDatesToCalendar(calendarGrid: GridLayout) {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        calendar.set(year, month, 1)

        val totalDays = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        calendarGrid.removeAllViews()

        for (day in 1..totalDays) {
            val dayView = createDayView(day)
            calendarGrid.addView(dayView)

            // 날짜 클릭 리스너 설정
            dayView.setOnClickListener {
                showEmojiSelectionDialog(day, dayView)
            }
        }
    }

    private fun createDayView(day: Int): LinearLayout {
        val dayLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            layoutParams = GridLayout.LayoutParams().apply {
                width = convertDpToPx(48)
                height = convertDpToPx(64)
                setMargins(8, 8, 8, 8)
            }
        }

        // 날짜 텍스트
        val dayText = TextView(this).apply {
            text = day.toString()
            textSize = 18f
            gravity = Gravity.CENTER
        }

        // 이모지 표시용 뷰
        val emojiView = TextView(this).apply {
            textSize = 24f
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = convertDpToPx(4) // 이모지와 날짜 간격 설정
            }
        }

        // 이모지가 저장된 경우 해당 이모지 설정
        emojiMap[day]?.let { emojiRes ->
            emojiView.text = getString(emojiRes)
        }

        dayLayout.addView(dayText)
        dayLayout.addView(emojiView)
        return dayLayout
    }

    private fun showEmojiSelectionDialog(day: Int, dayView: LinearLayout) {
        val emojiList = arrayOf("😍","😄","😊","🤔","😡") // 이모지 리스트

        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_emoji_selection, null)
        val emojiContainer = dialogView.findViewById<LinearLayout>(R.id.emoji_container)

        val dialog = AlertDialog.Builder(this)
            .setTitle("오늘의 만족도는?")
            .setView(dialogView)
            .create()

        // 이모지 버튼 동적 생성
        emojiList.forEach { emoji ->
            val emojiButton = TextView(this).apply {
                text = emoji
                textSize = 36f
                gravity = Gravity.CENTER
                setOnClickListener {
                    setEmojiForDay(day, emoji, dayView)
                    dialog.dismiss()
                }
            }
            emojiContainer.addView(emojiButton)
        }

        dialog.show()
    }

    private fun setEmojiForDay(day: Int, emoji: String, dayView: LinearLayout) {
        // 이모지 설정
        emojiMap[day] = emoji.codePointAt(0) // 이모지 저장

        // View에 이모지 업데이트
        val emojiView = dayView.getChildAt(1) as TextView
        emojiView.text = emoji
    }

    private fun convertDpToPx(dp: Int): Int {
        val density = resources.displayMetrics.density
        return (dp * density).toInt()
    }
}