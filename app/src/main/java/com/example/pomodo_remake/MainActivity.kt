package com.example.pomodo_remake

import android.app.AlertDialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.snackbar.Snackbar
import java.util.Locale
import java.text.SimpleDateFormat


class MainActivity : AppCompatActivity() {
    private lateinit var startButton: ImageView         // 시작 버튼
    private lateinit var stopButton: ImageView          // 정지 버튼
    private lateinit var timerIcon: ImageView           // 타이머 화면 아이콘
    private lateinit var leaderBoardIcon: ImageView     //기록 화면 아이콘
    private lateinit var checkListIcon: ImageView       // 플래너 화면 아이콘
    private lateinit var resetButton: ImageView // 초기화 버튼 추가

    private var focusTime: Long = 25 * 60 * 1000L       // 기본 집중 시간 (25분)
    private var breakTime : Long = 5 * 60 * 1000L       // 기본 휴식 시간 (5분)
    private var repeatCount: Int = 4                    // 기본 반복 횟수(4번)
    private var focusRemainingTime: Long = focusTime    // 집중 시간의 남은 시간
    private var breakRemainingTime: Long = breakTime    // 휴식 시간의 남은 시간
    private var currentRepeatCount = 0                  // 현재 실행 중인 반복 횟수


    private var isFocusTimerRunning = false // 집중 타이머 실행 상태  //실행 중일 때 true, 실행 아닐때 false
    private var isBreakTimerRunning = false // 휴식 타이머 실행 상태  //실행 중일 때 true, 실행 아닐 때 false
    private var isFocusTimerStopState = false // 집중 타이머 멈춤 상태 //멈췄을 때 true, 안 멈췄을 때 false
    private var isBreakTimerStopState = false    // 휴식 타이머 멈춤 상태  //멈췄을 때 true, 안 멈췄을 때 false

    private lateinit var countDownFocusTimer: CountDownTimer // 집중 타이머를 관리할 카운트 다운 변수
    private lateinit var countDownBreakTimer: CountDownTimer // 휴식 타이머를 관리할 카운트 다운 변수
    private lateinit var focusTimeView: TextView    // 집중 타이머 업데이트 화면에 필요함.
    private lateinit var breakTimeView: TextView    // 휴식 타이머 업데이트 화면에 필요함.


    //private var lastUpdatedDate: LocalDate? = null // 마지막으로 총 집중 시간 업데이트된 날짜
    //private lateinit var totalFocusTimeChronometer: Chronometer  //총 집중 시간 카운트업 위젯
    //private var isTotalFocusTimeRunning = false                 //총 집중 시간 작동 상태
    //private var totalFocusTimeBaseTime: Long = 0L               //총 집중 시간 저장할 변수?

    private var totalFocusTimeInSeconds: Long = 0 // 총 집중 시간 (초 단위)
    private lateinit var totalFocusTImeView: TextView        // 총 집중 시간 업데이트 화면에 필요함.
    private var isFirstTick = false // 첫 번째 onTick 호출을 건너뛸지 여부를 확인하는 변수



    //데이터베이스 설계 테스트를 위해 추가해봄
    private val timerViewModel: TimerViewModel by viewModels()



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        createNotificationChannel()
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        } // 이 부분은 기본으로 생성되있는듯


        // 오늘의 총 집중 시간을 가져오는 코드
        loadTodayFocusTime()


        //UI 요소 초기화
        startButton = findViewById(R.id.startButton)
        stopButton = findViewById(R.id.stopButton)
        timerIcon = findViewById(R.id.timer)
        leaderBoardIcon = findViewById(R.id.leaderBoard)
        checkListIcon = findViewById(R.id.checkList)
        resetButton = findViewById(R.id.resetButton)
        breakTimeView = findViewById(R.id.breakTime)
        focusTimeView = findViewById(R.id.focusTime)
        //totalFocusTimeChronometer = findViewById(R.id.totalFocusTime)
        totalFocusTImeView = findViewById(R.id.totalFocusTimeView)

        // **앱 시작 시 기본값 설정**
        focusTime = 25 * 60 * 1000L // 25분
        breakTime = 5 * 60 * 1000L  // 5분
        repeatCount = 4             // 반복 횟수 4번
        focusRemainingTime = focusTime
        breakRemainingTime = breakTime

        // **UI에 기본값 반영**
        updateFocusTimeUI(focusTime)
        updateBreakTimeUI(breakTime)

        // 타이머 시작
        startButton.setOnClickListener {
            when {
                isFocusTimerStopState -> startFocusTimer() // 집중 타이머 재개
                isBreakTimerStopState -> startBreakTimer() // 휴식 타이머 재개
                !isFocusTimerRunning && !isBreakTimerRunning -> {
                    startFocusTimer()
                    //startTotalFocusChronometer()
                } // 집중 타이머 새로 시작
            }

            timerIcon.visibility = View.GONE
            checkListIcon.visibility = View.GONE
            leaderBoardIcon.visibility = View.GONE
        }

        stopButton.setOnClickListener {
            when {
                isFocusTimerRunning -> stopFocusTimer() // 집중 타이머 멈춤
                isBreakTimerRunning -> stopBreakTimer() // 휴식 타이머 멈춤
            }
            // 총 집중 시간 Chronometer 정지
            //stopTotalFocusChronometer()

            timerIcon.visibility = View.VISIBLE
            checkListIcon.visibility = View.VISIBLE
            leaderBoardIcon.visibility = View.VISIBLE
        }

        //시간 설정
        focusTimeView.setOnClickListener {
            if (!isFocusTimerRunning && !isBreakTimerRunning || // 처음 시작 상태
                (isFocusTimerRunning && isFocusTimerStopState) || // 집중 타이머 실행 중 멈춘 상태
                (isBreakTimerRunning && isBreakTimerStopState)    // 휴식 타이머 실행 중 멈춘 상태
            )
                showTimeSettingsDialog()
            }

        // checkListIcon 클릭 시 플래너 부분으로 이동 (추후 기능 추가 가능)
        checkListIcon.setOnClickListener {
            // 필요한 경우 동작 정의
        }

        // leaderBoardIcon 클릭 시 기록 부분으로 이동 (추후 기능 추가 가능)
        leaderBoardIcon.setOnClickListener{

        }

    }

    //집중 타이머 시작
    private fun startFocusTimer(){
        if (isFocusTimerRunning) return // 이미 실행 중이면 무시
        isFocusTimerRunning = true      //집중 타이머 시작
        isBreakTimerRunning = false     //휴식 타이머는 정지 상태

        // 집중 타이머 시작 시 휴식 타이머 UI 초기화
        updateBreakTimeUI(breakRemainingTime)


        // 멈춘 상태에서 재개 시 첫 Tick 상태 초기화
        if (isFocusTimerStopState) {
            isFirstTick = true
        }

        countDownFocusTimer = object : CountDownTimer(focusRemainingTime, 1000) { // 1초 단위로 카운트다운
            override fun onTick(millisUntilFinished: Long) {
                focusRemainingTime = millisUntilFinished // 남은 시간을 업데이트
                updateFocusTimeUI(focusRemainingTime)    //집중 시간 업데이트                                   // UI 업데이트


                if (isFirstTick) {
                    // 멈춘 후 재개 시 첫 번째 tick은 총 집중 시간 업데이트 없이 건너뜀
                    isFirstTick = false
                } else {
                    // 이후 tick에서 총 집중 시간 업데이트
                        totalFocusTimeInSeconds++
                    Log.d("TIMER_UPDATE", "Updated totalFocusTimeInSeconds: $totalFocusTimeInSeconds seconds")
                        updateTotalFocusTimeUI(totalFocusTimeInSeconds)
                }
            }

            override fun onFinish() {
                isFocusTimerRunning = false // 타이머 상태 업데이트
                focusRemainingTime = focusTime // 다음 실행을 위해 초기화      // 남은 시간 0으로 설정
                updateFocusTimeUI(0)

                // 집중 타이머 완료 시 총 집중 시간을 저장
                val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(System.currentTimeMillis())
                val focusTimeInMillis = totalFocusTimeInSeconds * 1000L // 초 -> 밀리초 변환
                saveFocusTimeToDatabase(today, focusTimeInMillis) // 초 -> 밀리초 변환
                Log.d("TIMER_STATE", "Focus time finished. Saved $focusTimeInMillis ms for date $today")


                // Snackbar를 중앙에 표시
                val rootView = findViewById<View>(R.id.main) // 루트 레이아웃 ID
                showCenteredSnackbar(rootView, "집중 타이머 종료",1500L)  //1.5초동안 타이머 종료 되었다고 뜸



                startBreakTimer()           // 타이머 종료 후 휴식 타이머 시작
            }
        }.start()



        isFocusTimerStopState = false // 시작 시 멈춘 상태 해제
        stopButton.visibility = View.VISIBLE
        startButton.visibility = View.GONE

    }

    //휴식 타이머 시작
    private fun startBreakTimer() {
        if (isBreakTimerRunning) return // 이미 실행 중이면 무시
        isBreakTimerRunning = true      // 휴식 타이머 시작
        isBreakTimerStopState = false   // 멈춘 상태 해제

        // 휴식 타이머 시작 로직
        countDownBreakTimer = object : CountDownTimer(breakRemainingTime, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                breakRemainingTime = millisUntilFinished
                updateBreakTimeUI(breakRemainingTime)


            }
            override fun onFinish() {
                isBreakTimerRunning = false
                breakRemainingTime = breakTime // 다음 실행을 위해 초기화
                updateBreakTimeUI(0)

                val rootView = findViewById<View>(R.id.main) // 루트 레이아웃
                showCenteredSnackbar(rootView, "휴식 타이머 종료", 1500L) // 1.5초 동안 알림 표시

                checkRepeat() // 반복 횟수 확인 및 다음 단계 실행
            }
        }.start()

        isFocusTimerStopState = false // 시작하면 멈춘 상태를 해제

        stopButton.visibility = View.VISIBLE
        startButton.visibility = View.GONE
    }

    private fun stopFocusTimer(){
        if (!isFocusTimerRunning) return  // 실행 중이 아니라면 무시
        isFocusTimerRunning = false      // 집중 타이머 실행 정지
        isFocusTimerStopState = true    // 멈춘 상태로 설정
        countDownFocusTimer.cancel()



        // 경과된 시간을 계산(DB 저장을 위해)
        val elapsedTimeInMillis = totalFocusTimeInSeconds * 1000L // 초 -> 밀리초 변환
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(System.currentTimeMillis()) // 오늘 날짜 가져오기
        // 경과된 시간과 날짜 RoomDB에 저장
        saveFocusTimeToDatabase(today, elapsedTimeInMillis)

        // 저장 후 확인 로그 출력
        Log.d("TIMER_STATE", "Focus timer stopped. Saved $elapsedTimeInMillis ms for date $today")


        startButton.visibility = View.VISIBLE
        stopButton.visibility = View.GONE

    }

    private fun stopBreakTimer(){
        if (!isBreakTimerRunning) return    // 실행 중이 아니면 무시
        isBreakTimerRunning = false         // 휴식 타이머 실행 정지
        isBreakTimerStopState = true        // 멈춘 상태로 설정
        countDownBreakTimer.cancel()

        startButton.visibility = View.VISIBLE
        stopButton.visibility = View.GONE

        //휴식 타이머 정지 로직
    }

    private fun showTimeSettingsDialog() {
        // 다이얼로그 레이아웃 가져오기
        val dialogView = layoutInflater.inflate(R.layout.time_settings_dialog, null)

        // EditText 초기화
        val focusTimeInput: EditText = dialogView.findViewById(R.id.focus_time_input)
        val breakTimeInput: EditText = dialogView.findViewById(R.id.break_time_input)
        val repeatCountInput: EditText = dialogView.findViewById(R.id.repeat_count_input)

        // 로그 추가 (디버깅용)
        Log.d("DEBUG", "focusTime: ${focusTime / 60000}, breakTime: ${breakTime / 60000}, repeatCount: $repeatCount")

        // 다이얼로그 생성
        val dialog = AlertDialog.Builder(this)
            .setTitle("시간 설정")
            .setView(dialogView)
            .setPositiveButton("확인") { _, _ ->
                val focusTimeInputValue = focusTimeInput.text.toString().toLongOrNull()
                val breakTimeInputValue = breakTimeInput.text.toString().toLongOrNull()
                val repeatCountInputValue = repeatCountInput.text.toString().toIntOrNull()

                focusTime = if (focusTimeInputValue != null && focusTimeInputValue in 1..120) {
                    focusTimeInputValue * 60 * 1000 // 밀리초 단위로 변환
                } else {
                    25 * 60 * 1000 // 기본값 25분
                }

                breakTime = if (breakTimeInputValue != null && breakTimeInputValue in 1..60) {
                    breakTimeInputValue * 60 * 1000 // 밀리초 단위로 변환
                } else {
                    5 * 60 * 1000 // 기본값 5분
                }

                repeatCount = if (repeatCountInputValue != null && repeatCountInputValue in 1..10) {
                    repeatCountInputValue
                } else {
                    4 // 기본값 4회
                }

                // 새로운 값으로 타이머 초기화
                focusRemainingTime = focusTime
                breakRemainingTime = breakTime


                // 타이머와 상태 초기화
                isFocusTimerRunning = false
                isBreakTimerRunning = false
                isFocusTimerStopState = false
                isBreakTimerStopState = false
                // 다이얼로그로 재설정했으므로 첫 번째 Tick은 총 집중 시간을 동기화
                isFirstTick = false
                currentRepeatCount = 0

                updateFocusTimeUI(focusRemainingTime)
                updateBreakTimeUI(breakRemainingTime)


            }
            .setNegativeButton("취소", null)
            .create()

        // 다이얼로그 표시 후 값 설정
        dialog.setOnShowListener {
            focusTimeInput.setText((focusTime / 60000).toString())
            breakTimeInput.setText((breakTime / 60000).toString())
            repeatCountInput.setText(repeatCount.toString())
        }

        dialog.show()
    }


    // 타이머 반복 횟수 처리
    private fun checkRepeat() {
        currentRepeatCount++
        if (currentRepeatCount < repeatCount) {
            startFocusTimer() // 다음 반복의 집중 타이머 실행
        } else {
            finishAllRepeats() // 모든 반복 완료 처리
        }
    }

    // 타이머 반복 횟수 완료 처리
    private fun finishAllRepeats() {
        isFocusTimerRunning = false
        isBreakTimerRunning = false
        isFocusTimerStopState = false
        isBreakTimerStopState = false
        currentRepeatCount = 0          //반복 횟수 초기화
        isFirstTick = false             // 첫 번째 tick을 초기화


        // 화면 요소 상태 초기화
        stopButton.visibility = View.GONE
        startButton.visibility = View.VISIBLE
        timerIcon.visibility = View.VISIBLE
        checkListIcon.visibility = View.VISIBLE
        leaderBoardIcon.visibility = View.VISIBLE

        // 타이머 UI를 00:00으로 초기화
        focusTimeView.text = String.format(Locale.getDefault(), "%02d:%02d", 0, 0)
        breakTimeView.text = String.format(Locale.getDefault(), "%02d:%02d", 0, 0)

        // **focusTime과 breakTime을 초기값으로 복원**
        focusTime = 25 * 60 * 1000L // 25분
        breakTime = 5 * 60 * 1000L  // 5분

        // 남은 시간을 초기화 (기본 시간으로 리셋)
        focusRemainingTime = focusTime
        breakRemainingTime = breakTime
        // 로그 추가 (디버깅용)
        Log.d("TIMER_STATE", "All repetitions finished. UI and timer reset.")
        
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            "timer_service_channel",
            "Timer Service Channel",
            NotificationManager.IMPORTANCE_LOW
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager?.createNotificationChannel(channel)
    }


    //집중 타이머 업데이트 화면
    private fun updateFocusTimeUI(remainingTime: Long) {
        val minutes = (remainingTime / 1000) / 60
        val seconds = (remainingTime / 1000) % 60
        focusTimeView.text = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
    }

    //휴식 타이머 업데이트 화면
    private fun updateBreakTimeUI(remainingTime: Long) {
        // 남은 시간을 시간, 분, 초로 변환
        val minutes = (remainingTime / 1000) / 60
        val seconds = (remainingTime / 1000) % 60

        // 전역 변수로 초기화된 TextView를 사용
        breakTimeView.text = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
    }

    /*
    private fun startTotalFocusChronometer() {
        if (!isTotalFocusTimeRunning) {
            totalFocusTimeChronometer.base = SystemClock.elapsedRealtime() - totalFocusTimeBaseTime
            totalFocusTimeChronometer.start()
            isTotalFocusTimeRunning = true
        }
    }*/

    /*
    private fun stopTotalFocusChronometer() {
        if (isTotalFocusTimeRunning) {
            totalFocusTimeBaseTime = SystemClock.elapsedRealtime() - totalFocusTimeChronometer.base
            totalFocusTimeChronometer.stop()
            isTotalFocusTimeRunning = false
        }
    }*/

    // 총 집중 시간 UI 업데이트 함수
    private fun updateTotalFocusTimeUI(totalSeconds: Long) {
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60

        totalFocusTImeView.text = String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
        Log.d("UI_UPDATE", "Updated total focus time: $hours:$minutes:$seconds")
    }


    // (집중/휴식)시간 종료 알림 텍스틑
    fun showCenteredSnackbar(view: View, message: String, duration: Long) {
        val snackbar = Snackbar.make(view, message, Snackbar.LENGTH_INDEFINITE)

        // Snackbar의 레이아웃 수정
        val snackbarView = snackbar.view
        val params = snackbarView.layoutParams as FrameLayout.LayoutParams
        params.gravity = Gravity.CENTER // 중앙에 위치시키기
        snackbarView.layoutParams = params

        snackbar.show()

        // 지정된 시간 후 스낵바 닫기
        snackbarView.postDelayed({
            snackbar.dismiss()
        }, duration)
    }

    //타이머 종료 시 roomDB에 저장
    private fun saveFocusTimeToDatabase(date: String, focusTimeInMillis: Long) {
        if (focusTimeInMillis > 0) { // 0 이상인 경우에만 저장
            val record = TimerRecords(date = date, totalFocusTime = focusTimeInMillis)
            timerViewModel.insertOrUpdateRecord(record)
            Log.d("DATABASE_SAVE", "Saving focus time: $focusTimeInMillis ms for date: $date")
        } else {
            Log.d("DATABASE_SAVE", "No focus time to save for date: $date")
        }
    }



    //총 집중 시간 하루동안 UI에 저장되게 하는 코드
    private fun loadTodayFocusTime() {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(System.currentTimeMillis())

        timerViewModel.allRecords.observe(this) { records ->
            val todayRecord = records.find { it.date == today }
            val totalFocusTimeFromDB = todayRecord?.totalFocusTime ?: 0L // ms 값 그대로 가져오기


            // ms -> 초 변환
            this.totalFocusTimeInSeconds = totalFocusTimeFromDB / 1000
            // UI 업데이트
            updateTotalFocusTimeUI(this.totalFocusTimeInSeconds)
            Log.d("LOAD_DATA", "Loaded focus time: ${this.totalFocusTimeInSeconds} seconds for date: $today")

        }
    }










}


