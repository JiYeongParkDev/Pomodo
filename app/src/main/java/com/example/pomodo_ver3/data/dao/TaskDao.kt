package com.example.pomodo_ver3.data.dao

import androidx.room.*
import com.example.pomodo_ver3.data.entity.Task

@Dao
interface TaskDao {

    // 할 일 추가
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: Task)

    // 할 일 수정
    @Update
    suspend fun updateTask(task: Task)

    // 할 일 삭제
    @Delete
    suspend fun deleteTask(task: Task)

    // 특정 날짜의 할 일 목록 가져오기
    @Query("SELECT * FROM tasks WHERE date = :date")
    suspend fun getTasksByDate(date: String): List<Task>

    // 모든 할 일 가져오기 (날짜 내림차순 정렬)
    @Query("SELECT * FROM tasks ORDER BY date DESC")
    suspend fun getAllTasks(): List<Task>
}