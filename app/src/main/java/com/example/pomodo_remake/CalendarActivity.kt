package com.example.pomodo_remake

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.widget.Button
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.lifecycle.ViewModelProvider
import java.time.LocalDate
import java.time.YearMonth
import java.util.Locale
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.w3c.dom.Text
import java.util.*

class CalendarActivity : AppCompatActivity(){
    private lateinit var viewModel: CalendarViewModel
    private lateinit var calendarGridLayout: GridLayout // 상단 캘린더


    private lateinit var timerIcon: ImageView           // 타이머 화면 아이콘
    private lateinit var leaderBoardIcon: ImageView     //기록 화면 아이콘
    private lateinit var checkListIcon: ImageView       //플래너 화면 아이콘

    private val timerViewModel: TimerViewModel by viewModels() // TimerViewModel 사용

    private var currentYear = LocalDate.now().year          // 현재 연도
    private var currentMonth = LocalDate.now().monthValue   // 현재 월

    //새로 추가된 부분(이모지)
    private lateinit var emojiCalendarCardView: GridLayout


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //setContentView(R.layout.activity_calendar)  // 원래 내꺼!!!
        setContentView(R.layout.activity_calendar_combined) // 통합 레이아웃 사용

        timerIcon = findViewById(R.id.timer)
        checkListIcon = findViewById(R.id.checkList)
        leaderBoardIcon = findViewById(R.id.leaderBoard)
        calendarGridLayout = findViewById(R.id.calendarGridLayout)

        //새로 추가된 부분
        emojiCalendarCardView = findViewById(R.id.emojiCalendarGrid)
        generateSimpleCalendar()
        // 이 윗부분까지 이모지 부분임

        // 현재 연도와 월을 화면에 표시
        updateMonthYearText()


        // 초기 달력 데이터를 로드
        timerViewModel.loadFocusTimesForMonth(currentYear, currentMonth)

        // LiveData 관찰
        // 기존 allRecords 관찰 제거 및 focusTimes 관찰 추가
        timerViewModel.focusTimes.observe(this) { focusTimes ->
            Log.d("CalendarActivity", "FocusTimes Observed: $focusTimes")
            generateCalendar(focusTimes)
        }


        // 이전달로 이동
        findViewById<Button>(R.id.prevMonthButton).setOnClickListener {
            updateCalendar(-1)
        }

        //다음 달로 이동
        findViewById<Button>(R.id.nextMonthButton).setOnClickListener {
            updateCalendar(1) //다음 달로 이동
        }

        // 플래너 부분으로 이동
        checkListIcon.setOnClickListener {
            startActivity(Intent(this, PlannerActivity::class.java))
        }

        timerIcon.setOnClickListener{
            startActivity(Intent(this, MainActivity::class.java))
        }


    }

    //현재 연도와 월 업데이트
    private fun updateCalendar(increment: Int) {
        currentMonth += increment
        if (currentMonth < 1) {
            currentMonth = 12
            currentYear -= 1
        } else if (currentMonth > 12) {
            currentMonth = 1
            currentYear += 1

        }

        // 새 연도와 월 표시
        updateMonthYearText()

        // 특정 월의 집중 시간을 ViewModel에 요청
        timerViewModel.loadFocusTimesForMonth(currentYear, currentMonth)

        // 이모지 달력 갱신
        generateSimpleCalendar()
    }



    //달력 만들기
    private fun generateCalendar(focusTimes: Map<String, Int>) {
        calendarGridLayout.removeAllViews()

        // 요일 헤더 추가
        val daysOfWeek = listOf("S", "M", "T", "W", "T", "F", "S")
        for (day in daysOfWeek) {
            val headerView = TextView(this).apply {
                text = day
                gravity = Gravity.CENTER
                layoutParams = GridLayout.LayoutParams().apply {
                    width = 0
                    height = 100
                    columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                }
                setTextColor(if (day == "S") Color.RED else Color.BLACK)
            }
            calendarGridLayout.addView(headerView)

        }

        // 빈 칸과 날짜 개수 계산
        val firstDayOfMonth = LocalDate.of(currentYear, currentMonth, 1)
        val dayOfWeek = firstDayOfMonth.dayOfWeek.value % 7 // 일요일(0) ~ 토요일(6)
        val blankDays = dayOfWeek
        val daysInMonth = YearMonth.of(currentYear, currentMonth).lengthOfMonth()
        val totalCells = blankDays + daysInMonth


        // 행 개수 설정
        val rows = Math.ceil(totalCells / 7.0).toInt()
        calendarGridLayout.rowCount = rows + 1 // 요일 헤더 포함


        // 첫 주의 빈 칸 추가
        for (i in 0 until blankDays) {
            val emptyView = TextView(this).apply {
                text = "" // 빈 칸
                gravity = Gravity.CENTER
                layoutParams = GridLayout.LayoutParams().apply {
                    width = 0
                    height = 100
                    columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                }
            }
            calendarGridLayout.addView(emptyView)
        }


        //날짜 및 이모지 추가
        for (day in 1..daysInMonth) {
            val date = String.format(Locale.US, "%04d-%02d-%02d", currentYear, currentMonth, day)
            val focusTime = focusTimes[date] ?: 0

            val dayView = TextView(this).apply {
                text = day.toString()
                gravity = Gravity.CENTER
                layoutParams = GridLayout.LayoutParams().apply {
                    width = 0
                    height = 100
                    columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                }
                setBackgroundColor(getColorBasedOnFocusTime(focusTime))
                setTextColor(Color.BLACK)
            }
            calendarGridLayout.addView(dayView)

        }
    }


    //UI에 현재 년도, 월을 뜨도록 함
    private fun updateMonthYearText() {
        val monthYearText = getString(R.string.month_year_format, currentYear, currentMonth)
        findViewById<TextView>(R.id.monthYearTextView).text = monthYearText
    }


    // 총 집중 시간에 따라 색상 계산
    private fun getColorBasedOnFocusTime(focusTime: Int): Int {
        return when (focusTime) {
            in 0..0 -> Color.WHITE // 기본값
            in 1..1800 -> Color.parseColor("#F0F8FF") // 1초 ~ 30분
            in 1801..3600 -> Color.parseColor("#CBEAF9") // 30분 1초 ~ 1시간
            in 3601..7200 -> Color.parseColor("#B7E1F9") // 1시간 1초 ~ 2시간
            in 7201..14400 -> Color.parseColor("#A2D6F8") // 2시간 1초 ~ 4시간
            in 14401..21600 -> Color.parseColor("#8DCBF7") // 4시간 1초 ~ 6시간
            in 21601..28800 -> Color.parseColor("#79C0F7") // 6시간 1초 ~ 8시간
            in 28801..72000 -> Color.parseColor("#64B5F6") // 8시간 1초 ~ 20시간
            else -> Color.WHITE // 예상치 못한 값에 대한 기본값
        }
    }

    // 기록 부분 액티비티가 다시 열릴때 해당 월의 데이터를 강제로 다시 로드함
    override fun onResume() {
        super.onResume()
        timerViewModel.loadFocusTimesForMonth(currentYear, currentMonth)
    }

    //새로 추가된 이모지 부분
    private fun generateSimpleCalendar() {
        emojiCalendarCardView.removeAllViews()

        // 요일 헤더 추가
        val daysOfWeek = listOf("S", "M", "T", "W", "T", "F", "S")
        for (day in daysOfWeek) {
            val headerView = TextView(this).apply {
                text = day
                gravity = Gravity.CENTER
                layoutParams = GridLayout.LayoutParams().apply {
                    width = 0
                    height = 100
                    columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                }
                setTextColor(if (day == "S") Color.RED else Color.BLACK)
            }
            emojiCalendarCardView.addView(headerView)
        }

        // 빈 칸과 날짜 추가
        val firstDayOfMonth = LocalDate.of(currentYear, currentMonth, 1)
        val dayOfWeek = firstDayOfMonth.dayOfWeek.value % 7 // 일요일(0) ~ 토요일(6)
        val blankDays = dayOfWeek
        val daysInMonth = YearMonth.of(currentYear, currentMonth).lengthOfMonth()
        val totalCells = blankDays + daysInMonth

        for (i in 0 until totalCells) {
            val dayView = TextView(this).apply {
                text = if (i >= blankDays) (i - blankDays + 1).toString() else "" // 빈 칸 또는 날짜
                gravity = Gravity.CENTER
                layoutParams = GridLayout.LayoutParams().apply {
                    width = 0
                    height = 100
                    columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                }
                setTextColor(Color.BLACK)

                if (i >= blankDays) {
                    val day = i - blankDays + 1
                    setOnClickListener {
                        showEmojiSelectionDialog(day, this) // 다이얼로그를 표시
                    }
                    loadEmojiForDate(day, this) // 저장된 이모지를 불러와서 표시
                }

            }
            emojiCalendarCardView.addView(dayView)
        }

    }

    private fun showEmojiSelectionDialog(day: Int, dayView: TextView) {
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
                    val dateKey = getDateKey(day)
                    saveEmojiToDatabase(dateKey, emoji) // 이모지를 RoomDB에 저장
                    dayView.text = "$day\n$emoji" // 날짜 아래에 이모지 표시
                    dialog.dismiss()
                }
            }
            emojiContainer.addView(emojiButton)
        }

        dialog.show()
    }


    private fun saveEmojiToDatabase(date: String, emoji: String) {
        val database = AppDatabase.getDatabase(this)
        CoroutineScope(Dispatchers.IO).launch {
            database.calendarEmojiDao().insertEmojiForDate(CalendarEmoji(date, emoji))
        }
    }

    private fun loadEmojiForDate(day: Int, dayView: TextView) {
        val database = AppDatabase.getDatabase(this)
        val dateKey = getDateKey(day) // 날짜를 `YYYY-MM-DD` 형식으로 생성
        CoroutineScope(Dispatchers.IO).launch {
            val emoji = database.calendarEmojiDao().getEmojiForDate(dateKey)?.emoji
            emoji?.let {
                runOnUiThread {
                    dayView.text = "$day\n$it" // 날짜 아래에 이모지를 표시
                }
            }
        }
    }

    private fun getDateKey(day: Int): String {
        return "$currentYear-${currentMonth.toString().padStart(2, '0')}-${day.toString().padStart(2, '0')}"
    }






}