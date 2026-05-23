package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

// Exercise model representing an active physical movement
data class Exercise(
    val name: String,
    val description: String,
    val durationSeconds: Int,
    val targetMuscles: String,
    val imageType: String, // Type of custom animation/art to display in Canvas
    val difficulty: String = "مبتدئ"
) : Serializable

// Standard Workout Program Table
data class WorkoutTable(
    val id: String,
    val title: String,
    val description: String,
    val totalMinutes: Int,
    val difficulty: String = "مبتدئ",
    val equipments: String = "بدون أدوات (منزلي)",
    val exercises: List<Exercise>
) : Serializable

// Primary Room Entity to store finished routines and build daily streaks
@Entity(tableName = "workout_history")
data class WorkoutHistoryItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val workoutId: String,
    val workoutTitle: String,
    val timestamp: Long = System.currentTimeMillis(),
    val durationSeconds: Int,
    val exercisesCompleted: Int,
    val caloriesBurned: Int
)
