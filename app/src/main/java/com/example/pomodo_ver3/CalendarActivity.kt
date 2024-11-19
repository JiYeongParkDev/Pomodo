package com.example.pomodo_ver3

import android.content.Context
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.Button
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import java.util.Calendar

class CalendarActivity : AppCompatActivity() {

    private val emojiMap = mutableMapOf<String, String>() // 날짜와 선택된 이모지 저장
    private val calendar: Calendar = Calendar.getInstance() // 달력 객체

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 저장된 이모지 데이터를 불러오기
        loadEmojiData()

        val calendarGrid = findViewById<GridLayout>(R.id.calendarGrid)

        // 이전 달 버튼 리스너 추가
        findViewById<Button>(R.id.prevMonthButton).setOnClickListener {
            updateCalendar(-1, calendarGrid) // 이전 달로 이동
        }

        // 다음 달 버튼 리스너 추가
        findViewById<Button>(R.id.nextMonthButton).setOnClickListener {
            updateCalendar(1, calendarGrid) // 다음 달로 이동
        }

        addDatesToCalendar(calendarGrid)
        updateCurrentMonthText()
    }

    private fun updateCalendar(monthChange: Int, calendarGrid: GridLayout) {
        calendar.add(Calendar.MONTH, monthChange) // 현재 Calendar 객체의 월 변경
        addDatesToCalendar(calendarGrid) // 달력 다시 그리기
        updateCurrentMonthText() // 현재 월 텍스트 업데이트
    }

    private fun updateCurrentMonthText() {
        val monthYearTextView = findViewById<TextView>(R.id.monthYearTextView)
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1 // Calendar.MONTH는 0부터 시작
        monthYearTextView.text = "${year}년 ${month}월"
    }

    private fun addDatesToCalendar(calendarGrid: GridLayout) {
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        calendar.set(year, month, 1)

        val totalDays = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        val firstDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) - 1 // 요일(일요일: 1)
        calendarGrid.removeAllViews()

        // 요일 헤더 추가
        addWeekdayHeadersToCalendar(calendarGrid)

        // 빈칸 추가 (달의 첫날이 시작하는 요일 맞추기)
        for (i in 0 until firstDayOfWeek) {
            calendarGrid.addView(createEmptyDayView())
        }

        for (day in 1..totalDays) {
            val dayView = createDayView(day)
            calendarGrid.addView(dayView)

            // 날짜 클릭 리스너 설정
            dayView.setOnClickListener {
                showEmojiSelectionDialog(day, dayView)
            }
        }
    }

    private fun addWeekdayHeadersToCalendar(calendarGrid: GridLayout) {
        val weekdays = arrayOf("S", "M", "T", "W", "T", "F", "S") // 요일 배열
        weekdays.forEachIndexed { index,day ->
            val dayHeader = TextView(this).apply {
                text = day
                textSize = 16f
                gravity = Gravity.CENTER

                // 주말 색상 조건 설정
                setTextColor(
                    if (index == 0 || index == 6) // "S"는 주말 (첫 번째와 마지막 요일)
                        ContextCompat.getColor(context, android.R.color.holo_red_dark) // 빨간색
                    else
                        ContextCompat.getColor(context, android.R.color.black) // 검은색
                )
                layoutParams = GridLayout.LayoutParams().apply {
                    width = convertDpToPx(48)
                    height = convertDpToPx(32)
                    setMargins(8, 4, 8, 4)
                }
            }
            calendarGrid.addView(dayHeader)
        }
    }

    private fun createEmptyDayView(): LinearLayout {
        return LinearLayout(this).apply {
            layoutParams = GridLayout.LayoutParams().apply {
                width = convertDpToPx(48)
                height = convertDpToPx(64)
                setMargins(8, 8, 8, 8)
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

        // 날짜 키 생성
        val dateKey = getDateKey(day)

        // 이모지가 저장된 경우 해당 이모지 설정
        emojiMap[dateKey]?.let { emoji ->
            emojiView.text = emoji
        }

        dayLayout.addView(dayText)
        dayLayout.addView(emojiView)
        return dayLayout
    }

    private fun showEmojiSelectionDialog(day: Int, dayView: LinearLayout) {
        val emojiList = arrayOf("😍", "😄", "😊", "🤔", "😡") // 이모지 리스트

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
        val dateKey = getDateKey(day) // 날짜 키 생성
        emojiMap[dateKey] = emoji // 선택된 이모지 저장
        saveEmojiData() // 데이터 저장

        // View에 이모지 업데이트
        val emojiView = dayView.getChildAt(1) as TextView
        emojiView.text = emoji
    }

    private fun saveEmojiData() {
        val sharedPreferences = getSharedPreferences("emoji_prefs", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        emojiMap.forEach { (key, emoji) ->
            editor.putString(key, emoji) // 키-값 쌍으로 저장
        }
        editor.apply()
    }

    private fun loadEmojiData() {
        val sharedPreferences = getSharedPreferences("emoji_prefs", Context.MODE_PRIVATE)
        sharedPreferences.all.forEach { (key, emoji) ->
            emojiMap[key] = emoji.toString() // 키-값 쌍으로 불러오기
        }
    }

    private fun getDateKey(day: Int): String {
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1 // Calendar.MONTH는 0부터 시작
        return "$year-${month.toString().padStart(2, '0')}-${day.toString().padStart(2, '0')}" // YYYY-MM-DD 형식
    }

    private fun convertDpToPx(dp: Int): Int {
        val density = resources.displayMetrics.density
        return (dp * density).toInt()
    }
}