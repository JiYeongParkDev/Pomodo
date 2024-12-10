package com.example.pomodo_remake

import android.app.AlertDialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
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
import java.time.LocalDate
import android.Manifest
import android.content.IntentFilter
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri


class MainActivity : AppCompatActivity() {
    private lateinit var startButton: ImageView         // 시작 버튼
    private lateinit var stopButton: ImageView          // 정지 버튼
    private lateinit var timerIcon: ImageView           // 타이머 화면 아이콘
    private lateinit var leaderBoardIcon: ImageView     //기록 화면 아이콘
    private lateinit var checkListIcon: ImageView       // 플래너 화면 아이콘
    private lateinit var resetButton: ImageView         // 초기화 버튼 추가

    private var focusTime: Long = 25 * 60 * 1000L       // 기본 집중 시간 (25분)
    private var breakTime: Long = 5 * 60 * 1000L        // 기본 휴식 시간 (5분)
    private var repeatCount: Int = 4                    // 기본 반복 횟수(4번)
    private var focusRemainingTime: Long = focusTime    // 집중 시간의 남은 시간
    private var breakRemainingTime: Long = breakTime    // 휴식 시간의 남은 시간
    private var currentRepeatCount = 0                  // 현재 실행 중인 반복 횟수


    private var isFocusTimerRunning = false             // 집중 타이머 실행 상태     //실행 중일 때 true, 실행 아닐때 false
    private var isBreakTimerRunning = false             // 휴식 타이머 실행 상태     //실행 중일 때 true, 실행 아닐 때 false
    private var isFocusTimerStopState = false           // 집중 타이머 멈춤 상태     //멈췄을 때 true, 안 멈췄을 때 false
    private var isBreakTimerStopState = false           // 휴식 타이머 멈춤 상태     //멈췄을 때 true, 안 멈췄을 때 false

    private lateinit var countDownFocusTimer: CountDownTimer // 집중 타이머를 관리할 카운트 다운 변수
    private lateinit var countDownBreakTimer: CountDownTimer // 휴식 타이머를 관리할 카운트 다운 변수
    private lateinit var focusTimeView: TextView             // 집중 타이머 업데이트 화면에 필요함.
    private lateinit var breakTimeView: TextView            // 휴식 타이머 업데이트 화면에 필요함.


    private var totalFocusTimeInSeconds: Long = 0            // 총 집중 시간 (초 단위)
    private lateinit var totalFocusTimeView: TextView        // 총 집중 시간 업데이트 화면에 필요함.
    private var isFirstTick = false                          // 첫 번째 onTick 호출을 건너뛸지 여부를 확인하는 변수

    // 총 집중 시간 외에 것들(타이머 상태, 타이머 남은 시간, 반복 관련 정보, 타이머 정지 상태 )을 저장하기 위해서 사용
    private lateinit var sharedPreferences: SharedPreferences

    //데이터베이스
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
        }

        // 알림 권한 요청 (SDK 33 이상인 경우)
        if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        }

        //UI 요소 초기화
        startButton = findViewById(R.id.startButton)
        stopButton = findViewById(R.id.stopButton)
        timerIcon = findViewById(R.id.timer)
        leaderBoardIcon = findViewById(R.id.leaderBoard)
        checkListIcon = findViewById(R.id.checkList)
        resetButton = findViewById(R.id.resetButton)
        breakTimeView = findViewById(R.id.breakTime)
        focusTimeView = findViewById(R.id.focusTime)
        totalFocusTimeView = findViewById(R.id.totalFocusTimeView)


        // 오늘의 총 집중 시간을 가져오는 코드
        loadTodayFocusTime()

        // SharedPreferences 초기화
        sharedPreferences = getSharedPreferences("TimerPreferences", Context.MODE_PRIVATE)

        // 데이터 복원
        restoreTimerState()

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


            timerIcon.visibility = View.VISIBLE
            checkListIcon.visibility = View.VISIBLE
            leaderBoardIcon.visibility = View.VISIBLE
        }

        //시간 설정
        focusTimeView.setOnClickListener {
            if (!isFocusTimerRunning && !isBreakTimerRunning ||     // 처음 시작 상태
                (isFocusTimerRunning && isFocusTimerStopState) ||   // 집중 타이머 실행 중 멈춘 상태
                (isBreakTimerRunning && isBreakTimerStopState)      // 휴식 타이머 실행 중 멈춘 상태
            )
                showTimeSettingsDialog()
        }

        // checkListIcon 클릭 시 플래너 부분으로 이동
        checkListIcon.setOnClickListener {
            val intent = Intent(this, PlannerActivity::class.java)
            startActivity(intent)
        }

        // leaderBoardIcon 클릭 시 기록 부분으로 이동
        leaderBoardIcon.setOnClickListener {
            val intent = Intent(this, CalendarActivity::class.java)
            startActivity(intent)
        }

    }

    //집중 타이머 시작
    private fun startFocusTimer() {
        if (isFocusTimerRunning) return     // 이미 실행 중이면 무시
        isFocusTimerRunning = true          //집중 타이머 시작
        isBreakTimerRunning = false         //휴식 타이머는 정지 상태


        // 첫 실행 시 `isFirstTick` 초기화
        isFirstTick = false

        // 집중 타이머 시작 시 휴식 타이머 UI 초기화
        updateBreakTimeUI(breakRemainingTime)


        // 멈춘 상태에서 재개 시 첫 Tick 상태 초기화
        if (isFocusTimerStopState) {
            isFirstTick = true
        }

        // Foreground Service 시작 및 알림 업데이트
        startForegroundService(true)

        countDownFocusTimer = object : CountDownTimer(focusRemainingTime, 1000) { // 1초 단위로 카운트다운
            override fun onTick(millisUntilFinished: Long) {
                focusRemainingTime = millisUntilFinished    // 남은 시간을 업데이트
                updateFocusTimeUI(focusRemainingTime)       //집중 시간 업데이트

                // Foreground Service 알림 업데이트
                startForegroundService(true)

                if (isFirstTick) {
                    // 멈춘 후 재개 시 첫 번째 tick은 총 집중 시간 업데이트 없이 건너뜀
                    isFirstTick = false
                } else {
                    // 이후 tick에서 총 집중 시간 업데이트
                    totalFocusTimeInSeconds++
                    Log.d(
                        "TIMER_UPDATE",
                        "Updated totalFocusTimeInSeconds: $totalFocusTimeInSeconds seconds"
                    )
                    updateTotalFocusTimeUI(totalFocusTimeInSeconds)

                    // `SimpleDateFormat`을 사용해 오늘 날짜를 가져오기
                    val today = SimpleDateFormat(
                        "yyyy-MM-dd",
                        Locale.getDefault()
                    ).format(System.currentTimeMillis())

                    // `SharedPreferences`나 `RoomDB`에 저장
                    saveFocusTimeToDatabase(today, totalFocusTimeInSeconds * 1000L)  // 초 단위로 저장
                }
            }

            override fun onFinish() {
                isFocusTimerRunning = false // 타이머 상태 업데이트
                focusRemainingTime = focusTime // 다음 실행을 위해 초기화      // 남은 시간 0으로 설정
                updateFocusTimeUI(0)

                // 집중 타이머 완료 시 총 집중 시간을 저장
                val today = SimpleDateFormat(
                    "yyyy-MM-dd",
                    Locale.getDefault()
                ).format(System.currentTimeMillis())
                val focusTimeInMillis = totalFocusTimeInSeconds * 1000L // 초 -> 밀리초 변환
                saveFocusTimeToDatabase(today, focusTimeInMillis) // 초 -> 밀리초 변환
                Log.d(
                    "TIMER_STATE",
                    "Focus time finished. Saved $focusTimeInMillis ms for date $today"
                )


                // Snackbar를 중앙에 표시
                val rootView = findViewById<View>(R.id.main) // 루트 레이아웃 ID
                showCenteredSnackbar(rootView, "집중 타이머 종료", 1500L,this@MainActivity)  //1.5초동안 타이머 종료 되었다고 뜸


                startBreakTimer()           // 타이머 종료 후 휴식 타이머 시작
            }
        }.start()



        isFocusTimerStopState = false            // 시작 시 멈춘 상태 해제
        stopButton.visibility = View.VISIBLE
        startButton.visibility = View.GONE

    }

    //휴식 타이머 시작
    private fun startBreakTimer() {
        if (isBreakTimerRunning) return // 이미 실행 중이면 무시
        isBreakTimerRunning = true      // 휴식 타이머 시작
        isBreakTimerStopState = false   // 멈춘 상태 해제


        // Foreground Service 알림 업데이트
        startForegroundService(false)

        // 휴식 타이머 시작 로직
        countDownBreakTimer = object : CountDownTimer(breakRemainingTime, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                breakRemainingTime = millisUntilFinished
                updateBreakTimeUI(breakRemainingTime)


                // Foreground Service 알림 업데이트   //false면 휴식 타이머
                startForegroundService(false)

            }

            override fun onFinish() {
                isBreakTimerRunning = false
                breakRemainingTime = breakTime // 다음 실행을 위해 초기화
                updateBreakTimeUI(0)

                val rootView = findViewById<View>(R.id.main) // 루트 레이아웃
                showCenteredSnackbar(rootView, "휴식 타이머 종료", 1500L,this@MainActivity) // 1.5초 동안 알림 표시

                checkRepeat() // 반복 횟수 확인 및 다음 단계 실행
            }
        }.start()

        isFocusTimerStopState = false // 시작하면 멈춘 상태를 해제

        stopButton.visibility = View.VISIBLE
        startButton.visibility = View.GONE
    }


    private fun stopFocusTimer() {
        if (!isFocusTimerRunning) return  // 실행 중이 아니라면 무시
        isFocusTimerRunning = false      // 집중 타이머 실행 정지
        isFocusTimerStopState = true    // 멈춘 상태로 설정
        countDownFocusTimer.cancel()


        // 경과된 시간을 계산(DB 저장을 위해)
        val elapsedTimeInMillis = totalFocusTimeInSeconds * 1000L // 초 -> 밀리초 변환
        val today = SimpleDateFormat(
            "yyyy-MM-dd",
            Locale.getDefault()
        ).format(System.currentTimeMillis()) // 오늘 날짜 가져오기
        // 경과된 시간과 날짜 RoomDB에 저장
        saveFocusTimeToDatabase(today, elapsedTimeInMillis)

        // 저장 후 ViewModel에 알림
        timerViewModel.loadFocusTimesForMonth(
            LocalDate.now().year,
            LocalDate.now().monthValue
        )



        startButton.visibility = View.VISIBLE
        stopButton.visibility = View.GONE

    }

    private fun stopBreakTimer() {
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


        // 다이얼로그 생성
        val dialog = AlertDialog.Builder(this)
            .setTitle("시간 설정")
            .setView(dialogView)
            .setPositiveButton("확인") { _, _ ->
                val focusTimeInputValue = focusTimeInput.text.toString().toDoubleOrNull()
                val breakTimeInputValue = breakTimeInput.text.toString().toDoubleOrNull()
                val repeatCountInputValue = repeatCountInput.text.toString().toIntOrNull()

                focusTime = if (focusTimeInputValue != null && focusTimeInputValue in 0.1..120.0) {
                    (focusTimeInputValue * 60 * 1000).toLong() // 밀리초 단위로 변환
                } else {
                    25 * 60 * 1000 // 기본값 25분
                }

                breakTime = if (breakTimeInputValue != null && breakTimeInputValue in 0.1..60.0) {
                    (breakTimeInputValue * 60 * 1000).toLong() // 밀리초 단위로 변환
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
            focusTimeInput.setText((focusTime / 60000.0).toString())
            breakTimeInput.setText((breakTime / 60000.0).toString())
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

        // Foreground Service 중지
        stopForegroundService()

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


    // 총 집중 시간 UI 업데이트 함수
    private fun updateTotalFocusTimeUI(totalSeconds: Long) {
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60

        totalFocusTimeView.text =
            String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
    }


    // (집중/휴식)시간 종료 알림 텍스트
    fun showCenteredSnackbar(view: View, message: String, duration: Long, context: Context) {
        // 시스템 기본 알림 소리 가져오기
        val notificationUri: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val ringtone: Ringtone = RingtoneManager.getRingtone(context, notificationUri)

        // 알림 소리 재생
        ringtone.play()

        val snackbar = Snackbar.make(view, message, Snackbar.LENGTH_INDEFINITE)

        // Snackbar의 레이아웃 수정
        val snackbarView = snackbar.view
        val params = snackbarView.layoutParams as FrameLayout.LayoutParams
        params.gravity = Gravity.CENTER // 중앙에 위치시키기
        snackbarView.layoutParams = params

        snackbar.show()

        // 지정된 시간 후 스낵바 닫기 및 소리 중지
        snackbarView.postDelayed({
            snackbar.dismiss()
            if (ringtone.isPlaying) {
                ringtone.stop() // 소리 중지
            }
        }, duration)
    }

    //타이머 종료 시 roomDB에 저장
    private fun saveFocusTimeToDatabase(date: String, focusTimeInMillis: Long) {
        if (focusTimeInMillis > 0) { // 0 이상인 경우에만 저장
            val record = TimerRecords(date = date, totalFocusTime = focusTimeInMillis)
            timerViewModel.insertOrUpdateRecord(record)

            // 데이터를 저장한 후 해당 월의 데이터를 다시 로드
            val currentYear = LocalDate.now().year
            val currentMonth = LocalDate.now().monthValue
            timerViewModel.loadFocusTimesForMonth(currentYear, currentMonth)

        }
    }

    //총 집중 시간 하루동안 UI에 저장되게 하는 코드
    private fun loadTodayFocusTime() {
        val today =
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(System.currentTimeMillis())

        timerViewModel.allRecords.observe(this) { records ->
            val todayRecord = records.find { it.date == today }
            val totalFocusTimeFromDB = todayRecord?.totalFocusTime ?: 0L // ms 값 그대로 가져오기


            // ms -> 초 변환
            this.totalFocusTimeInSeconds = totalFocusTimeFromDB / 1000
            // UI 업데이트
            updateTotalFocusTimeUI(this.totalFocusTimeInSeconds)

        }
    }

    //SharedPreferences에 데이터 저장하기 위한 함수
    private fun saveTimerState() {
        val editor = sharedPreferences.edit()

        editor.putLong("focusTime", focusTime) // 변경된 focusTime 저장
        editor.putLong("breakTime", breakTime) // 변경된 breakTime 저장
        editor.putLong(
            "totalFocusTimeInSeconds",
            totalFocusTimeInSeconds
        ) // totalFocusTimeInSeconds 저장

        // 타이머 상태
        editor.putBoolean("isFocusTimerRunning", isFocusTimerRunning)
        editor.putBoolean("isBreakTimerRunning", isBreakTimerRunning)

        // 남은 시간
        editor.putLong("focusRemainingTime", focusRemainingTime)
        editor.putLong("breakRemainingTime", breakRemainingTime)

        // 반복 정보
        editor.putInt("currentRepeatCount", currentRepeatCount)
        editor.putInt("repeatCount", repeatCount)

        // 정지 상태
        editor.putBoolean("isFocusTimerStopState", isFocusTimerStopState)
        editor.putBoolean("isBreakTimerStopState", isBreakTimerStopState)

        // 시스템 시간 저장
        editor.putLong("lastSavedTime", System.currentTimeMillis()) // 마지막 저장된 시간

        // 마지막 저장된 날짜
        val today =
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(System.currentTimeMillis())
        editor.putString("lastResetDate", today) // 마지막 초기화 날짜 저장

        editor.apply()
    }

    //SharedPreferences에서 데이터를 읽어 복원하기 위한 함수
    private fun restoreTimerState() {
        // 날짜 변경 확인 및 초기화
        if (checkAndResetIfNewDay()) {
            // 날짜가 변경된 경우 초기화된 데이터를 SharedPreferences에 저장
            saveTimerState()
        }

        // 저장된 값 복원, 없으면 기본값 사용
        focusTime = sharedPreferences.getLong("focusTime", 25 * 60 * 1000L) // 기본값 25분
        breakTime = sharedPreferences.getLong("breakTime", 5 * 60 * 1000L) // 기본값 5분
        totalFocusTimeInSeconds = sharedPreferences.getLong("totalFocusTimeInSeconds", 0L) // totalFocusTimeInSeconds 복원
        focusRemainingTime = sharedPreferences.getLong("focusRemainingTime", focusTime)
        breakRemainingTime = sharedPreferences.getLong("breakRemainingTime", breakTime)

        // 기타 상태 복원
        currentRepeatCount = sharedPreferences.getInt("currentRepeatCount", 0)
        repeatCount = sharedPreferences.getInt("repeatCount", 4) // 기본값 4회

        isFocusTimerRunning = sharedPreferences.getBoolean("isFocusTimerRunning", false)
        isBreakTimerRunning = sharedPreferences.getBoolean("isBreakTimerRunning", false)
        isFocusTimerStopState = sharedPreferences.getBoolean("isFocusTimerStopState", false)
        isBreakTimerStopState = sharedPreferences.getBoolean("isBreakTimerStopState", false)

        // 시스템 시간 복원
        val lastSavedTime = sharedPreferences.getLong("lastSavedTime", 0L)

        if (lastSavedTime > 0) {
            val elapsedTime = System.currentTimeMillis() - lastSavedTime // 경과 시간 계산

            // 경과 시간에 따라 남은 시간을 조정
            if (isFocusTimerRunning) {
                focusRemainingTime = maxOf(0, focusRemainingTime - elapsedTime)
            } else if (isBreakTimerRunning) {
                breakRemainingTime = maxOf(0, breakRemainingTime - elapsedTime)
            }

        }
        // 첫 Tick 방지 플래그
        isFirstTick = true

    }


    //SharedPreferences 를 적절한 시점에 데이터를 저장하기 위함
    override fun onPause() {
        super.onPause()
        saveTimerState()

        // 경과된 시간을 계산하여 SharedPreferences에 저장
        val elapsedTimeInMillis = focusTime - focusRemainingTime // 남은 시간과 전체 시간의 차이
        sharedPreferences.edit().putLong("elapsedTime", elapsedTimeInMillis).apply()

        // 타이머 상태 저장
        saveTimerState()


    }

    //액티비티가 다시 화면에 표시될 때 restoreTimerState()를 호출해서 저장된 값을 정확히 복원한다.
    override fun onResume() {
        super.onResume()

        // SharedPreferences에서 저장된 상태 복원
        restoreTimerState()

        // UI 업데이트
        updateFocusTimeUI(focusRemainingTime)
        updateBreakTimeUI(breakRemainingTime)

        // 타이머가 실행 중이고, 첫 번째 tick이 아니면
        if (isFocusTimerRunning) {
            // 복원된 남은 시간에 맞게 총 집중 시간에 차이를 더해줌
            val elapsedTime = System.currentTimeMillis() - sharedPreferences.getLong("lastSavedTime", System.currentTimeMillis()) // 복원된 시간과 남은 시간의 차이
            totalFocusTimeInSeconds += elapsedTime / 1000 // 초 단위로 변환하여 더함

            updateTotalFocusTimeUI(totalFocusTimeInSeconds)

            // `focusRemainingTime`도 경과된 시간 만큼 차감하여 정확하게 복원
            focusRemainingTime -= elapsedTime
            if (focusRemainingTime < 0) focusRemainingTime = 0 // 시간이 음수가 되지 않도록 보정
        }

        // 첫 번째 tick을 건너뛰기 위해 isFirstTick을 false로 설정
        isFirstTick = false

    }

    // 포그라운드 알림 시작 함수
    private fun startForegroundService(isFocusTimer: Boolean) {
        val intent = Intent(this, TimerForegroundService::class.java)
        intent.action = TimerForegroundService.ACTION_UPDATE
        intent.putExtra(TimerForegroundService.EXTRA_IS_FOCUS_TIMER, isFocusTimer)
        intent.putExtra(TimerForegroundService.EXTRA_FOCUS_REMAINING, focusRemainingTime)
        intent.putExtra(TimerForegroundService.EXTRA_BREAK_REMAINING, breakRemainingTime)
        intent.putExtra(TimerForegroundService.EXTRA_REPEAT_COUNT, repeatCount - currentRepeatCount)
        intent.putExtra(
            TimerForegroundService.EXTRA_TOTAL_FOCUS_TIME,
            totalFocusTimeInSeconds
        ) // 총 집중 시간 전달

        startService(intent)
    }

    // 포그라운드 알림 중지 함수
    private fun stopForegroundService() {
        stopService(Intent(this, TimerForegroundService::class.java))
    }


    // 알림 권한 요청 처리
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Log.d("MainActivity", "POST_NOTIFICATIONS permission granted")
        } else {
            Log.d("MainActivity", "POST_NOTIFICATIONS permission denied")
        }
    }

    //날짜가 바뀌면 총 집중 시간 UI초기화, 집중 시간, 휴식 시간, 반복횟수도 설정을 기본값으로 리셋되게 하는 함수
    private fun checkAndResetIfNewDay(): Boolean {

        val sharedPreferences = getSharedPreferences("TimerPreferences", Context.MODE_PRIVATE)
        val lastResetDate = sharedPreferences.getString("lastResetDate", null)

        // 현재 날짜 가져오기
        val today =
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(System.currentTimeMillis())

        if (lastResetDate != today) {
            resetTimerSettings() // 초기화 로직 실행

            // 새로운 날짜를 저장
            sharedPreferences.edit().putString("lastResetDate", today).apply()
            Log.d("RESET_TIMER", "New day detected. Timer reset.")

            return true // 날짜가 변경되었음을 반환
        }
        return false // 날짜가 변경되지 않았음을 반환
    }

    //집중 시간, 휴식 시간, 반복횟수, 총 집중 시간을 기본값으로 초기화하는 메서드
    private fun resetTimerSettings() {
        // 기본값으로 설정
        focusTime = 25 * 60 * 1000L // 기본값 25분
        breakTime = 5 * 60 * 1000L  // 기본값 5분
        repeatCount = 4             // 기본값 4회
        totalFocusTimeInSeconds = 0 // 총 집중 시간 초기화

        // UI 업데이트
        updateFocusTimeUI(focusTime)
        updateBreakTimeUI(breakTime)
        updateTotalFocusTimeUI(totalFocusTimeInSeconds)

        // SharedPreferences에도 저장
        saveTimerState()

    }

    //알림 클릭 시 MainActivity로 데이터를 전달받아 UI를 업데이트하도록 하는 함수
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        // 알림 클릭 시 전달된 데이터를 처리
        intent?.let {
            val isFocusTimer = it.getBooleanExtra("IS_FOCUS_TIMER", true)
            val remainingTime = it.getLongExtra("REMAINING_TIME", 0L)
            val totalFocusTime = it.getLongExtra("TOTAL_FOCUS_TIME", 0L)

            // UI 업데이트
            if (isFocusTimer) {
                updateFocusTimeUI(remainingTime)
            } else {
                updateBreakTimeUI(remainingTime)
            }
            updateTotalFocusTimeUI(totalFocusTime)
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        // 스택의 모든 Activity 제거
        finishAffinity()

    }
}

