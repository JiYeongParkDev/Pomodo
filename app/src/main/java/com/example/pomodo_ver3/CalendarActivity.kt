package com.example.pomodo_combined

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.*
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModels
import com.example.pomodo_ver3.CalendarViewModel
import com.example.pomodo_ver3.R
import java.time.LocalDate
import java.time.YearMonth
import java.util.*

class CombinedCalendarActivity : AppCompatActivity() {

    // 상단 캘린더 관련 변수
    private lateinit var topCalendarGrid: GridLayout
    private val timerViewModel: CalendarViewModel by viewModels()
    private var currentYear = LocalDate.now().year
    private var currentMonth = LocalDate.now().monthValue

    // 하단 캘린더 관련 변수
    private lateinit var bottomCalendarGrid: GridLayout
    private val emojiMap = mutableMapOf<String, String>() // 날짜와 이모지 매핑
    private val bottomCalendar = Calendar.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_combined_calendar)

        // 상단 캘린더 초기화
        topCalendarGrid = findViewById(R.id.topCalendarGridLayout)
        val topMonthYearTextView = findViewById<TextView>(R.id.topMonthYearTextView)

        // 초기 데이터 로드 및 UI 업데이트
        updateTopCalendarUI(topMonthYearTextView)
        timerViewModel.loadFocusTimesForMonth(currentYear, currentMonth)
        timerViewModel.focusTimes.observe(this) { focusTimes ->
            Log.d("CombinedCalendar", "FocusTimes Observed: $focusTimes")
            updateTopCalendarUI(topMonthYearTextView, focusTimes)
        }

        // 하단 캘린더 초기화
        bottomCalendarGrid = findViewById(R.id.bottomCalendarGridLayout)
        val bottomMonthYearTextView = findViewById<TextView>(R.id.bottomMonthYearTextView)
        loadEmojiData() // 저장된 이모지 데이터 불러오기
        updateBottomCalendarUI(bottomMonthYearTextView)

        // 이전/다음 달 버튼 리스너
        findViewById<Button>(R.id.prevMonthButton).setOnClickListener {
            updateCalendar(-1, topMonthYearTextView, bottomMonthYearTextView)
        }
        findViewById<Button>(R.id.nextMonthButton).setOnClickListener {
            updateCalendar(1, topMonthYearTextView, bottomMonthYearTextView)
        }
    }

    // 상단 캘린더 업데이트 (집중 시간 기반)
    private fun updateTopCalendarUI(
        monthYearTextView: TextView,
        focusTimes: Map<String, Int> = emptyMap()
    ) {
        monthYearTextView.text = "${"currentYear년"} ${"currentMonth월"}"
        topCalendarGrid.removeAllViews()

        // 요일 헤더 추가
        val daysOfWeek = listOf("S", "M", "T", "W", "T", "F", "S")
        for (day in daysOfWeek) {
            val headerView = TextView(this).apply {
                text = day
                gravity = Gravity.CENTER
                setTextColor(if (day == "S") Color.RED else Color.BLACK)
            }
            topCalendarGrid.addView(headerView)
        }

        // 날짜 추가
        val firstDayOfMonth = LocalDate.of(currentYear, currentMonth, 1)
        val dayOfWeek = firstDayOfMonth.dayOfWeek.value % 7
        val daysInMonth = YearMonth.of(currentYear, currentMonth).lengthOfMonth()

        for (i in 0 until dayOfWeek) {
            topCalendarGrid.addView(TextView(this)) // 빈 칸
        }
        for (day in 1..daysInMonth) {
            val date = String.format("%04d-%02d-%02d", currentYear, currentMonth, day)
            val focusTime = focusTimes[date] ?: 0
            val dayView = TextView(this).apply {
                text = day.toString()
                gravity = Gravity.CENTER
                setBackgroundColor(getColorBasedOnFocusTime(focusTime))
            }
            topCalendarGrid.addView(dayView)
        }
    }

    private fun getColorBasedOnFocusTime(focusTime: Int): Int {
        return when (focusTime) {
            in 0..1800 -> Color.LTGRAY
            in 1801..3600 -> Color.CYAN
            else -> Color.BLUE
        }
    }

    // 하단 캘린더 업데이트 (이모지 선택 기반)
    private fun updateBottomCalendarUI(monthYearTextView: TextView) {
        val year = bottomCalendar.get(Calendar.YEAR)
        val month = bottomCalendar.get(Calendar.MONTH) + 1
        monthYearTextView.text = "$year년 $month월"
        bottomCalendarGrid.removeAllViews()

        // 요일 헤더 추가
        val weekdays = arrayOf("S", "M", "T", "W", "T", "F", "S")
        weekdays.forEachIndexed { index, day ->
            val headerView = TextView(this).apply {
                text = day
                gravity = Gravity.CENTER
                setTextColor(if (index == 0 || index == 6) Color.RED else Color.BLACK)
            }
            bottomCalendarGrid.addView(headerView)
        }

        // 날짜 추가
        val totalDays = bottomCalendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        val firstDayOfWeek = bottomCalendar.get(Calendar.DAY_OF_WEEK) - 1
        for (i in 0 until firstDayOfWeek) {
            bottomCalendarGrid.addView(TextView(this))
        }
        for (day in 1..totalDays) {
            val dayLayout = createDayView(day)
            bottomCalendarGrid.addView(dayLayout)
            dayLayout.setOnClickListener {
                showEmojiSelectionDialog(day, dayLayout)
            }
        }
    }

    private fun createDayView(day: Int): LinearLayout {
        val dayLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
        }

        val dayText = TextView(this).apply { text = day.toString() }
        val emojiView = TextView(this).apply {
            text = emojiMap[getDateKey(day)]
            textSize = 24f
        }

        dayLayout.addView(dayText)
        dayLayout.addView(emojiView)
        return dayLayout
    }

    private fun showEmojiSelectionDialog(day: Int, dayView: LinearLayout) {
        val emojis = arrayOf("😍", "😄", "😊", "🤔", "😡")
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_emoji_selection, null)
        val emojiContainer = dialogView.findViewById<LinearLayout>(R.id.emoji_container)

        val dialog = AlertDialog.Builder(this)
            .setTitle("오늘의 만족도는?")
            .setView(dialogView)
            .create()

        emojis.forEach { emoji ->
            val emojiButton = TextView(this).apply {
                text = emoji
                textSize = 36f
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
        emojiMap[getDateKey(day)] = emoji
        saveEmojiData()
        (dayView.getChildAt(1) as TextView).text = emoji
    }

    private fun saveEmojiData() {
        val sharedPreferences = getSharedPreferences("emoji_prefs", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        emojiMap.forEach { (key, value) -> editor.putString(key, value) }
        editor.apply()
    }

    private fun loadEmojiData() {
        val sharedPreferences = getSharedPreferences("emoji_prefs", Context.MODE_PRIVATE)
        sharedPreferences.all.forEach { (key, value) ->
            emojiMap[key] = value.toString()
        }
    }

    private fun getDateKey(day: Int): String {
        val year = bottomCalendar.get(Calendar.YEAR)
        val month = bottomCalendar.get(Calendar.MONTH) + 1
        return "$year-${month.toString().padStart(2, '0')}-${day.toString().padStart(2, '0')}"
    }

    private fun updateCalendar(
        monthChange: Int,
        topMonthYearTextView: TextView,
        bottomMonthYearTextView: TextView
    ) {
        currentMonth += monthChange
        if (currentMonth < 1) {
            currentMonth = 12
            currentYear -= 1
        } else if (currentMonth > 12) {
            currentMonth = 1
            currentYear += 1
        }
        bottomCalendar.add(Calendar.MONTH, monthChange)

        updateTopCalendarUI(topMonthYearTextView)
        updateBottomCalendarUI(bottomMonthYearTextView)
    }
}