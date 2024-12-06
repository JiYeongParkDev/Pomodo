package com.example.pomodo_remake

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity


class OnboardingActivity : AppCompatActivity() {
    private var currentScreen = 1 // 현재 온보딩 화면을 추적하는 변수
    private lateinit var sharedPreferences: SharedPreferences // SharedPreferences 선언

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // SharedPreferences 초기화
        sharedPreferences = getSharedPreferences("OnboardingPrefs", MODE_PRIVATE)
        val isFirstLaunch = sharedPreferences.getBoolean("isFirstLaunch", true)

        // 첫 실행이 아니면 바로 MainActivity로 이동
        if (!isFirstLaunch) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        // 첫 번째 온보딩 화면 표시
        setContentView(R.layout.onboarding_timer_screen)

        // 화면 터치 이벤트 처리
        findViewById<View>(android.R.id.content).setOnClickListener {
            when (currentScreen) {
                1 -> showSecondScreen() // 첫 번째 화면 -> 두 번째 화면
                2 -> showThirdScreen()  // 두 번째 화면 -> 세 번째 화면
                3 -> showFourthScreen() // 세 번째 화면 -> 네 번째 화면
                4 -> showFifthScreen()  // 네 번째 화면 -> 메인 화면
                5 -> showSixthScreen()  // 네 번째 화면 -> 메인 화면
                6 -> goToMainScreen()  // 네 번째 화면 -> 메인 화면
            }
        }
    }

    private fun showSecondScreen() {
        setContentView(R.layout.onboarding_timer_screen2)
        currentScreen = 2 // 현재 화면 번호 갱신
    }

    private fun showThirdScreen() {
        setContentView(R.layout.onboarding_planner_screen)
        currentScreen = 3 // 현재 화면 번호 갱신
    }

    private fun showFourthScreen() {
        setContentView(R.layout.onboarding_planner_screen2)
        currentScreen = 4 // 현재 화면 번호 갱신
    }

    private fun showFifthScreen() {
        setContentView(R.layout.onboarding_calendar_screen)
        currentScreen = 5 // 현재 화면 번호 갱신
    }

    private fun showSixthScreen() {
        setContentView(R.layout.onboarding_calendar_screen2)
        currentScreen = 6 // 현재 화면 번호 갱신
    }

    private fun goToMainScreen() {
        // 온보딩 완료 상태를 SharedPreferences에 저장
        sharedPreferences.edit().putBoolean("isFirstLaunch", false).apply()

        // 메인 화면으로 이동
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
