package com.example.pomodo_remake

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
import androidx.lifecycle.ViewModelProvider
import java.time.LocalDate
import java.time.YearMonth
import java.util.Locale // 로케일 임포트 추가

class CalendarActivity : AppCompatActivity(){
    private lateinit var viewModel: CalendarViewModel
    private lateinit var calendarGridLayout: GridLayout

    private lateinit var timerIcon: ImageView           // 타이머 화면 아이콘
    private lateinit var leaderBoardIcon: ImageView     //기록 화면 아이콘
    private lateinit var checkListIcon: ImageView       //플래너 화면 아이콘

    private val timerViewModel: TimerViewModel by viewModels() // TimerViewModel 사용

    private var currentYear = LocalDate.now().year          // 현재 연도
    private var currentMonth = LocalDate.now().monthValue   // 현재 월

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_calendar)

        timerIcon = findViewById(R.id.timer)
        checkListIcon = findViewById(R.id.checkList)
        leaderBoardIcon = findViewById(R.id.leaderBoard)
        calendarGridLayout = findViewById(R.id.calendarGridLayout)


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
            // 필요한 경우 동작 정의
        }

        timerIcon.setOnClickListener{
            // MainActivity로 이동
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
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





}