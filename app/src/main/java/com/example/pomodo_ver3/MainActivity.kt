package com.example.pomodo_ver3

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import android.graphics.Paint
import android.graphics.Typeface
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.pomodo_ver3.R
import java.util.Calendar
import com.example.pomodo_ver3.MyApplication
import com.example.pomodo_ver3.data.dao.TaskDao
import com.example.pomodo_ver3.data.database.AppDatabase
import com.example.pomodo_ver3.data.entity.Task
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var taskDao: TaskDao

    private lateinit var calendarButton: ImageView // 중앙 아이콘 버튼

    private lateinit var addTaskButton: TextView // + 버튼
    private lateinit var taskContainer: LinearLayout // 할 일 목록 컨테이너
    private lateinit var editButton: TextView // 편집 버튼
    private lateinit var calendarIcon: ImageView // 캘린더 아이콘
    private var isEditMode: Boolean = false // 편집 모드 여부를 저장하는 플래그 변수
    private var selectedDate: String = "" // 선택된 날짜를 저장하는 변수

    private val tasksMap = mutableMapOf<String, MutableList<Task>>() // 날짜별 할 일(Task 객체) 저장


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 상태바 숨기기
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN
        actionBar?.hide()
        // 전체 화면 모드 설정
        window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_FULLSCREEN or
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                )
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        setContentView(R.layout.layout) // XML 레이아웃 파일 설정

        // 하단 아이콘 연결
        val timerIcon = findViewById<ImageView>(R.id.timerIcon) // 타이머 아이콘
        val checklistIcon = findViewById<ImageView>(R.id.checklistIcon) // 체크리스트 아이콘
        val leaderBoardIcon = findViewById<ImageView>(R.id.calendarViewIcon) // 기록 보기 아이콘

        // 타이머 화면으로 이동 (추후 구현)
        timerIcon.setOnClickListener {
            Log.d("ICON_CLICK", "타이머 아이콘 클릭됨, TimerActivity로 이동 준비")
            // TimerActivity가 준비되지 않았으므로 주석 처리
            // val intent = Intent(this, TimerActivity::class.java)
            // startActivity(intent)
            Log.d("ICON_CLICK", "TimerActivity는 아직 구현되지 않았습니다.")
        }

        // 체크리스트 화면 (현재 이 화면) - 클릭 시 동작 없음
        checklistIcon.setOnClickListener {
            Log.d("ICON_CLICK", "체크리스트 아이콘 클릭됨, 현재 화면 유지")
        }

        // 기록 보기 화면으로 이동 (CalendarActivity)
        leaderBoardIcon.setOnClickListener {
            Log.d("ICON_CLICK", "기록 보기 아이콘 클릭됨, CalendarActivity로 이동")
            val intent = Intent(this, CalendarActivity::class.java)
            startActivity(intent)
        }

        // 레이아웃의 뷰 참조
        addTaskButton = findViewById(R.id.addText) // + 버튼 참조
        taskContainer = findViewById(R.id.taskContainer) // 동적으로 할 일 추가할 LinearLayout 참조
        editButton = findViewById(R.id.editButton) // 편집 버튼 참조
        calendarIcon = findViewById(R.id.calendarIcon) // 캘린더 아이콘 참조

        // 편집 버튼 클릭 시 편집 모드 토글
        editButton.setOnClickListener {
            toggleEditMode() // 편집 모드 전환 함수 호출
        }

        // + 버튼 클릭 시 할 일 추가 기능
        addTaskButton.setOnClickListener {
            showAddTaskDialog() // 버튼 클릭 시 새로운 할 일 추가
        }

        // 캘린더 아이콘 클릭 시 날짜 선택 다이얼로그 표시
        calendarIcon.setOnClickListener {
            showDatePickerDialog()
            Log.d("DATE_SELECTION", "showDatePickerDialog 함수 호출됨")
        }
        // 새로운 Task 데이터를 데이터베이스에 삽입 (추가된 부분)
        insertTask()

        // 데이터베이스 및 DAO 초기화
        val database = AppDatabase.getDatabase(this) // AppDatabase 인스턴스 가져오기
        taskDao = database.taskDao() // TaskDao 초기화

        val calendar = Calendar.getInstance()
        selectedDate = "${calendar.get(Calendar.YEAR)}-${calendar.get(Calendar.MONTH) + 1}-${calendar.get(Calendar.DAY_OF_MONTH)}"
        loadPlannerData(selectedDate) // 현재 날짜에 맞는 할 일만 로드
        Log.d("DATE_SELECTION", "앱 시작 시 기본 날짜 설정: $selectedDate")

        // 데이터 로드 버튼을 클릭했을 때 할 일 목록 조회
        //loadTasksFromDatabase()  // 앱 시작 시 데이터 조회를 위한 함수 호출

        // 또는, + 버튼 클릭 시 데이터 저장 후 loadTasksFromDatabase 호출
        addTaskButton.setOnClickListener {
            showAddTaskDialog() // 버튼 클릭 시 새로운 할 일 추가
        }
    }

    private fun loadTasksFromDatabase() {
        taskContainer.removeAllViews() // 기존 항목 초기화하여 중복 추가 방지
        lifecycleScope.launch {
            val tasks = taskDao.getAllTasks() // 데이터베이스에서 할 일 목록 가져오기
            tasks.forEach { task ->
                Log.d("DB_TEST", "조회된 할 일: ${task.title}, 날짜: ${task.date}, 상태: ${task.status}")
                addNewTask(task) // 각 할 일을 UI에 추가
            }
        }
    }

    private fun insertTask() {
        // 새 할 일 데이터 생성
        val newTask = Task(date = "2024-11-10", title = "예시 할 일", status = 0)
        Log.d("DB_TEST", "데이터베이스에 데이터가 저장되었습니다: 테스트 할 일")
        // Room DB에 데이터 삽입 (비동기)
        lifecycleScope.launch {
            MyApplication.database.taskDao().insertTask(newTask)
        }
    }

    // 날짜 선택 다이얼로그 표시 함수
    private fun showDatePickerDialog() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(this, { _, selectedYear, selectedMonth, selectedDay ->
            selectedDate = "$selectedYear-${selectedMonth + 1}-$selectedDay"
            loadPlannerData(selectedDate) // 선택된 날짜의 할 일 로드
            Log.d("DATE_SELECTION", "사용자가 선택한 날짜: $selectedDate")
        }, year, month, day)

        datePickerDialog.show()
    }

    // 날짜별 할 일을 로드하여 화면에 표시하는 함수
    private fun loadPlannerData(selectedDate: String) {
        taskContainer.removeAllViews() // 기존 항목 초기화
        lifecycleScope.launch {
            val tasks = taskDao.getTasksByDate(selectedDate) // 선택된 날짜의 할 일 목록 가져오기
            tasks.forEach { task ->
                addNewTask(task) // UI에 각 할 일을 추가
                Log.d("DATE_SELECTION", "로딩된 할 일: ${task.title}, 날짜: ${task.date}")
            }
        }
    }

    // 할 일과 함께 심볼을 표시할 TextView를 추가하는 함수
    private fun addNewTask(task: Task) {
        // 새로운 할 일 레이아웃 생성
        val taskLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(8, 8, 8, 8)
            gravity = android.view.Gravity.START // 왼쪽 정렬
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 16, 0, 0) // 상단 마진을 추가하여 공간 확보
            }
            tag=task
        }

        // 체크 표시 대신 기본 빈 동그라미 아이콘을 표시할 TextView 생성
        val symbolTextView = TextView(this).apply {
            val drawable = ContextCompat.getDrawable(context, R.drawable.ic_circle_empty) // 기본 빈 동그라미 아이콘
            drawable?.setBounds(0, 0, 65, 65) // 아이콘 크기 설정
            setCompoundDrawables(drawable, null, null, null)
            setPadding(8, 16, 8, 8)

            setOnClickListener {
                showSymbolSelectionDialog(this, task) // 클릭 시 심볼 선택 팝업 표시
            }
        }

        // Task의 현재 상태에 따라 심볼을 설정 (기존 상태 표시를 유지)
        updateSymbolDisplay(symbolTextView, task.status) // 현재 상태에 맞는 심볼로 업데이트

        // 할 일 이름을 표시할 TextView 생성
        val taskTextView = TextView(this).apply {
            text = task.title
            textSize = 18f
            setPadding(8, 8, 8, 8)
            setTextColor(ContextCompat.getColor(context, android.R.color.black))
            isClickable = true // 텍스트뷰를 클릭 가능하게 설정
            isFocusable = true // 포커스 가능하게 설정
        }

        // 수정 기능을 위한 클릭 리스너 추가
        taskTextView.setOnClickListener {
            if (isEditMode) {
                showEditTaskDialog(taskTextView, task) // 편집 모드일 때만 수정 다이얼로그 표시
            }
        }

        // 레이아웃에 심볼 TextView와 할 일 이름 TextView 추가
        taskLayout.addView(symbolTextView)
        taskLayout.addView(taskTextView)

        // 편집 모드일 때는 할 일 추가 시 바로 삭제 버튼도 함께 추가
        if (isEditMode) {
            val deleteButton = Button(this).apply {
                text = "삭제"
                tag = "deleteButton"

                setOnClickListener {
                    taskContainer.removeView(taskLayout) // 할 일 삭제
                    deleteTask(task)
                }
            }
            taskLayout.addView(deleteButton, 0) // 왼쪽에 추가
        }

        taskContainer.addView(taskLayout) // 전체 할 일 목록에 추가
    }

    // Task의 상태에 따라 심볼을 업데이트하는 함수
    private fun updateSymbolDisplay(symbolTextView: TextView, status: Int) {
        val drawableId = when (status) {
            1 -> R.drawable.ic_circle_filled // 상태 1일 때의 아이콘
            2 -> R.drawable.ic_triangle      // 상태 2일 때의 아이콘
            3 -> R.drawable.ic_cross         // 상태 3일 때의 아이콘
            else -> R.drawable.ic_circle_empty // 기본 빈 아이콘 (상태가 없을 때)
        }
        val drawable = ContextCompat.getDrawable(this, drawableId)
        drawable?.setBounds(0, 0, 65, 65) // 아이콘 크기 설정
        symbolTextView.setCompoundDrawables(drawable, null, null, null)
    }

    // 데이터베이스에서 Task 삭제 함수
    private fun deleteTask(task: Task) {
        Log.d("DB_TEST", "deleteTask 함수가 호출되었습니다.")
        lifecycleScope.launch {
            taskDao.deleteTask(task) // Room 데이터베이스에서 Task 삭제
            Log.d("DB_TEST", "데이터베이스에서 삭제되었습니다: ${task.title}")
        }
    }

    // 편집 모드 전환 함수
    private fun toggleEditMode() {
        isEditMode = !isEditMode

        // 편집 모드 상태에 따라 텍스트 변경
        if (isEditMode) {
            editButton.text = "완료"
        } else {
            editButton.text = "편집"
        }

        // taskContainer의 자식 뷰들을 반복하며 각 항목에 삭제 버튼 추가 또는 제거
        for (i in 0 until taskContainer.childCount) {
            val taskLayout = taskContainer.getChildAt(i) as LinearLayout
            val deleteButton = taskLayout.findViewWithTag<Button>("deleteButton")

            if (isEditMode) {
                // 편집 모드일 때 삭제 버튼이 없으면 추가
                if (deleteButton == null) {
                    val deleteButton = Button(this).apply {
                        text = "삭제"
                        tag = "deleteButton"
                        setOnClickListener {
                            Log.d("DB_TEST", "삭제 버튼이 클릭되었습니다.")
                            taskContainer.removeView(taskLayout) // 할 일 삭제
                            deleteTask(taskLayout.tag as Task) // 데이터베이스에서도 삭제
                        }
                    }
                    taskLayout.addView(deleteButton, 0) // 왼쪽에 추가
                }
            } else {
                // 편집 모드가 아닐 때는 삭제 버튼을 숨김
                deleteButton?.let {
                    Log.d("DB_TEST", "삭제 버튼이 제거되었습니다.")
                    taskLayout.removeView(it)
                }
            }
        }
    }

    // 할 일 추가 다이얼로그
    private fun showAddTaskDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_task, null)
        val taskEditText = dialogView.findViewById<EditText>(R.id.editTaskName)

        // 선택된 날짜가 비어있지 않은지 확인
        if (selectedDate.isBlank()) {
            val calendar = Calendar.getInstance()
            selectedDate = "${calendar.get(Calendar.YEAR)}-${calendar.get(Calendar.MONTH) + 1}-${calendar.get(Calendar.DAY_OF_MONTH)}"
        }

        // AlertDialog 생성 및 설정
        AlertDialog.Builder(this)
            .setTitle("새 할 일 추가")
            .setView(dialogView) // 커스텀 레이아웃 설정
            .setPositiveButton("확인") { dialog, _ ->
                val taskName = taskEditText.text.toString()
                if (taskName.isNotBlank()) {
                    val newTask = Task(date = selectedDate, title = taskName, status = 0)

                    lifecycleScope.launch {
                        taskDao.insertTask(newTask)
                        Log.d("DB_TEST", "데이터베이스에 데이터가 저장되었습니다: ${newTask.title}, 날짜: ${newTask.date}")
                        loadPlannerData(selectedDate) // 저장 후 해당 날짜의 할 일을 로드
                    }
                }
                dialog.dismiss()
            }
            .setNegativeButton("취소") { dialog, _ ->
                dialog.cancel()
            }
            .show()
    }

    // 할 일 제목 수정 다이얼로그 표시 함수
    private fun showEditTaskDialog(taskTextView: TextView, task: Task) { // (수정) task 파라미터 추가
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_task, null)
        val taskEditText = dialogView.findViewById<EditText>(R.id.editTaskName)
        taskEditText.setText(task.title) // (수정) 기존 할 일 제목을 task.title에서 가져옴

        // AlertDialog 생성 및 설정
        AlertDialog.Builder(this)
            .setTitle("할 일 수정")
            .setView(dialogView) // 커스텀 레이아웃 설정
            .setPositiveButton("확인") { dialog, _ ->
                val updatedTaskName = taskEditText.text.toString()
                if (updatedTaskName.isNotBlank()) {
                    task.title = updatedTaskName // (추가) Task 객체의 title 업데이트
                    taskTextView.text = updatedTaskName // UI의 TextView 텍스트 업데이트

                    // (추가) DB에 업데이트된 내용을 반영
                    lifecycleScope.launch {
                        taskDao.updateTask(task) // Task 객체 업데이트
                        Log.d("DB_TEST", "데이터베이스에서 제목이 업데이트되었습니다: ${task.title}")
                    }
                }
                dialog.dismiss()
            }
            .setNegativeButton("취소") { dialog, _ ->
                dialog.cancel()
            }
            .show()
    }

    private fun showSymbolSelectionDialog(symbolTextView: TextView, task: Task) {
        val symbols = arrayOf(
            R.drawable.ic_circle_filled, // 채워진 동그라미
            R.drawable.ic_triangle, // 세모
            R.drawable.ic_cross // 엑스
        )

        // 다이얼로그에 사용할 커스텀 레이아웃을 생성합니다.
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_symbol_selection, null)
        val symbolListView = dialogView.findViewById<LinearLayout>(R.id.symbol_list)

        // AlertDialog를 미리 생성해 둡니다.
        val dialog = AlertDialog.Builder(this)
            .setTitle("상태 선택")
            .setView(dialogView)
            .create()

        // 각 이미지를 클릭할 수 있는 ImageView로 추가합니다.
        symbols.forEachIndexed {index, drawableId ->
            val imageView = ImageView(this).apply {
                setImageResource(drawableId)
                layoutParams = LinearLayout.LayoutParams(150, 150).apply { // 다이얼로그 내 아이콘 크기 설정
                    setMargins(8, 8, 8, 8)
                }
                setOnClickListener {
                    // 선택한 상태를 Task 객체에 업데이트
                    task.status = index + 1 // 상태를 업데이트 (index + 1로 상태 구분)

                    // Task 상태에 맞는 심볼을 화면에 업데이트
                    updateSymbolDisplay(symbolTextView, task.status) // 상태에 따라 심볼 표시
                    dialog.dismiss() // 다이얼로그 닫기

                    // 변경된 상태를 데이터베이스에 저장
                    lifecycleScope.launch {
                        taskDao.updateTask(task) // Task 업데이트
                        Log.d("DB_TEST", "데이터베이스에 상태가 업데이트되었습니다: ${task.title}, 상태: ${task.status}")
                    }
                }
            }
            symbolListView.addView(imageView)
        }

        // 다이얼로그를 보여줍니다.
        dialog.show()
    }
}