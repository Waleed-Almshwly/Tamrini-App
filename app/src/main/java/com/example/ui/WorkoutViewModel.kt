package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*

class WorkoutViewModel(application: Application) : AndroidViewModel(application) {
    private val database = WorkoutDatabase.getDatabase(application)
    private val repository = WorkoutRepository(database.dao)

    // History Flow
    val history: StateFlow<List<WorkoutHistoryItem>> = repository.history
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Computed Daily Streak
    val streakDays: StateFlow<Int> = history.map { list ->
        calculateStreak(list)
    }.stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5000), initialValue = 0)

    // Computed Total Metrics
    val totalWorkouts: StateFlow<Int> = history.map { it.size }
        .stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5000), initialValue = 0)

    val totalMinutes: StateFlow<Int> = history.map { list ->
        val seconds = list.sumOf { it.durationSeconds }
        (seconds + 59) / 60
    }.stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5000), initialValue = 0)

    val totalCalories: StateFlow<Int> = history.map { list ->
        list.sumOf { it.caloriesBurned }
    }.stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5000), initialValue = 0)


    // --- Interactive Program Player State ---
    private val _activeProgram = MutableStateFlow<WorkoutTable?>(null)
    val activeProgram: StateFlow<WorkoutTable?> = _activeProgram.asStateFlow()

    enum class PlayerState {
        IDLE, PREPARING, WORKING, RESTING, SUMMARY
    }

    private val _playerState = MutableStateFlow(PlayerState.IDLE)
    val playerState: StateFlow<PlayerState> = _playerState.asStateFlow()

    private val _currentExerciseIndex = MutableStateFlow(0)
    val currentExerciseIndex: StateFlow<Int> = _currentExerciseIndex.asStateFlow()

    private val _secondsRemaining = MutableStateFlow(0)
    val secondsRemaining: StateFlow<Int> = _secondsRemaining.asStateFlow()

    private val _timerIsPaused = MutableStateFlow(false)
    val timerIsPaused: StateFlow<Boolean> = _timerIsPaused.asStateFlow()

    private var playerJob: Job? = null
    private var actualTimeSpentInWorkout = 0

    // Sound alert trigger flow (emits event to play a beep sound)
    private val _soundAlertTrigger = MutableSharedFlow<SoundAlertType>(extraBufferCapacity = 1)
    val soundAlertTrigger: SharedFlow<SoundAlertType> = _soundAlertTrigger.asSharedFlow()

    enum class SoundAlertType {
        TICK, START, REST, CONGRATS
    }


    // --- Custom Manual Timer State ---
    private val _customWorkSeconds = MutableStateFlow(30)
    val customWorkSeconds: StateFlow<Int> = _customWorkSeconds.asStateFlow()

    private val _customRestSeconds = MutableStateFlow(15)
    val customRestSeconds: StateFlow<Int> = _customRestSeconds.asStateFlow()

    private val _customTotalRounds = MutableStateFlow(4)
    val customTotalRounds: StateFlow<Int> = _customTotalRounds.asStateFlow()

    private val _customCurrentRound = MutableStateFlow(1)
    val customCurrentRound: StateFlow<Int> = _customCurrentRound.asStateFlow()

    private val _customTimerState = MutableStateFlow(PlayerState.IDLE)
    val customTimerState: StateFlow<PlayerState> = _customTimerState.asStateFlow()

    private val _customSecondsRemaining = MutableStateFlow(0)
    val customSecondsRemaining: StateFlow<Int> = _customSecondsRemaining.asStateFlow()

    private val _customIsPaused = MutableStateFlow(false)
    val customIsPaused: StateFlow<Boolean> = _customIsPaused.asStateFlow()

    private var customTimerJob: Job? = null


    // --- Actions for Program Player ---
    fun selectProgram(program: WorkoutTable) {
        _activeProgram.value = program
        _playerState.value = PlayerState.IDLE
        _currentExerciseIndex.value = 0
        _secondsRemaining.value = 0
        _timerIsPaused.value = false
        stopPlayer()
    }

    fun startProgram() {
        val program = _activeProgram.value ?: return
        _playerState.value = PlayerState.PREPARING
        _currentExerciseIndex.value = 0
        _secondsRemaining.value = 5 // Preparation countdown length
        _timerIsPaused.value = false
        actualTimeSpentInWorkout = 0
        runPlayerLoop()
    }

    fun pauseResumePlayer() {
        _timerIsPaused.value = !_timerIsPaused.value
    }

    fun skipNext() {
        val program = _activeProgram.value ?: return
        val currentIndex = _currentExerciseIndex.value
        val currentState = _playerState.value

        if (currentState == PlayerState.PREPARING || currentState == PlayerState.RESTING) {
            // Jump to active work of the next exercise
            _playerState.value = PlayerState.WORKING
            _secondsRemaining.value = program.exercises[currentIndex].durationSeconds
        } else if (currentState == PlayerState.WORKING) {
            // Move to rest, or complete if it's the last one
            if (currentIndex < program.exercises.size - 1) {
                _playerState.value = PlayerState.RESTING
                _secondsRemaining.value = 15 // Standard rest duration
                _soundAlertTrigger.tryEmit(SoundAlertType.REST)
            } else {
                completeWorkout()
            }
        }
    }

    fun skipPrevious() {
        val currentIndex = _currentExerciseIndex.value
        if (currentIndex > 0) {
            _currentExerciseIndex.value = currentIndex - 1
            val prevExercise = _activeProgram.value?.exercises?.get(currentIndex - 1)
            _playerState.value = PlayerState.WORKING
            _secondsRemaining.value = prevExercise?.durationSeconds ?: 30
        } else {
            // Restart prep
            _playerState.value = PlayerState.PREPARING
            _secondsRemaining.value = 5
        }
    }

    fun exitPlayer() {
        stopPlayer()
        _playerState.value = PlayerState.IDLE
    }

    private fun stopPlayer() {
        playerJob?.cancel()
        playerJob = null
    }

    private fun runPlayerLoop() {
        stopPlayer()
        _soundAlertTrigger.tryEmit(SoundAlertType.START)

        playerJob = viewModelScope.launch {
            while (true) {
                if (_timerIsPaused.value) {
                    delay(100)
                    continue
                }

                delay(1000)
                if (_timerIsPaused.value) continue

                val currentVal = _secondsRemaining.value
                val currentState = _playerState.value
                val program = _activeProgram.value ?: break
                val currentIndex = _currentExerciseIndex.value

                if (currentState == PlayerState.WORKING) {
                    actualTimeSpentInWorkout++
                }

                if (currentVal > 1) {
                    _secondsRemaining.value = currentVal - 1
                    // Beep sound on last 3 ticks
                    if (currentVal <= 4) {
                        _soundAlertTrigger.tryEmit(SoundAlertType.TICK)
                    }
                } else {
                    // State transitioned
                    when (currentState) {
                        PlayerState.PREPARING -> {
                            _playerState.value = PlayerState.WORKING
                            _secondsRemaining.value = program.exercises[currentIndex].durationSeconds
                            _soundAlertTrigger.tryEmit(SoundAlertType.START)
                        }
                        PlayerState.WORKING -> {
                            if (currentIndex < program.exercises.size - 1) {
                                _playerState.value = PlayerState.RESTING
                                _secondsRemaining.value = 15 // Standard rest style
                                _soundAlertTrigger.tryEmit(SoundAlertType.REST)
                            } else {
                                completeWorkout()
                                break
                            }
                        }
                        PlayerState.RESTING -> {
                            _currentExerciseIndex.value = currentIndex + 1
                            _playerState.value = PlayerState.WORKING
                            _secondsRemaining.value = program.exercises[currentIndex + 1].durationSeconds
                            _soundAlertTrigger.tryEmit(SoundAlertType.START)
                        }
                        else -> break
                    }
                }
            }
        }
    }

    private fun completeWorkout() {
        stopPlayer()
        _playerState.value = PlayerState.SUMMARY
        _soundAlertTrigger.tryEmit(SoundAlertType.CONGRATS)

        val program = _activeProgram.value ?: return
        viewModelScope.launch {
            val calories = (actualTimeSpentInWorkout * 0.15).toInt().coerceAtLeast(15)
            repository.saveHistory(
                WorkoutHistoryItem(
                    workoutId = program.id,
                    workoutTitle = program.title,
                    durationSeconds = actualTimeSpentInWorkout.coerceAtLeast(30),
                    exercisesCompleted = program.exercises.size,
                    caloriesBurned = calories
                )
            )
        }
    }


    // --- Actions for Custom Interval Timer ---
    fun updateCustomConfig(work: Int, rest: Int, rounds: Int) {
        _customWorkSeconds.value = work
        _customRestSeconds.value = rest
        _customTotalRounds.value = rounds
    }

    fun startCustomTimer() {
        _customTimerState.value = PlayerState.PREPARING
        _customCurrentRound.value = 1
        _customSecondsRemaining.value = 5
        _customIsPaused.value = false
        runCustomTimerLoop()
    }

    fun pauseResumeCustomTimer() {
        _customIsPaused.value = !_customIsPaused.value
    }

    fun resetCustomTimer() {
        customTimerJob?.cancel()
        customTimerJob = null
        _customTimerState.value = PlayerState.IDLE
        _customIsPaused.value = false
    }

    private fun runCustomTimerLoop() {
        customTimerJob?.cancel()
        _soundAlertTrigger.tryEmit(SoundAlertType.START)

        customTimerJob = viewModelScope.launch {
            while (true) {
                if (_customIsPaused.value) {
                    delay(100)
                    continue
                }

                delay(1000)
                if (_customIsPaused.value) continue

                val currentVal = _customSecondsRemaining.value
                val currentState = _customTimerState.value
                val currentRound = _customCurrentRound.value
                val totalRounds = _customTotalRounds.value

                if (currentVal > 1) {
                    _customSecondsRemaining.value = currentVal - 1
                    if (currentVal <= 4) {
                        _soundAlertTrigger.tryEmit(SoundAlertType.TICK)
                    }
                } else {
                    when (currentState) {
                        PlayerState.PREPARING -> {
                            _customTimerState.value = PlayerState.WORKING
                            _customSecondsRemaining.value = _customWorkSeconds.value
                            _soundAlertTrigger.tryEmit(SoundAlertType.START)
                        }
                        PlayerState.WORKING -> {
                            if (currentRound < totalRounds) {
                                _customTimerState.value = PlayerState.RESTING
                                _customSecondsRemaining.value = _customRestSeconds.value
                                _soundAlertTrigger.tryEmit(SoundAlertType.REST)
                            } else {
                                // Save Custom Workout completion into history!
                                _customTimerState.value = PlayerState.SUMMARY
                                _soundAlertTrigger.tryEmit(SoundAlertType.CONGRATS)
                                saveCustomWorkoutHistory()
                                break
                            }
                        }
                        PlayerState.RESTING -> {
                            _customCurrentRound.value = currentRound + 1
                            _customTimerState.value = PlayerState.WORKING
                            _customSecondsRemaining.value = _customWorkSeconds.value
                            _soundAlertTrigger.tryEmit(SoundAlertType.START)
                        }
                        else -> break
                    }
                }
            }
        }
    }

    private suspend fun saveCustomWorkoutHistory() {
        val totalActiveSeconds = _customTotalRounds.value * _customWorkSeconds.value
        val exercisesCompleted = _customTotalRounds.value
        val calories = (totalActiveSeconds * 0.14).toInt().coerceAtLeast(10)

        repository.saveHistory(
            WorkoutHistoryItem(
                workoutId = "custom_timer_workout",
                workoutTitle = "مؤقت تفاعلي مخصص",
                durationSeconds = totalActiveSeconds,
                exercisesCompleted = exercisesCompleted,
                caloriesBurned = calories
            )
        )
    }

    fun clearAllHistoryData() {
        viewModelScope.launch {
            repository.resetAll()
        }
    }


    // --- Streak Logic ---
    private fun calculateStreak(historyList: List<WorkoutHistoryItem>): Int {
        if (historyList.isEmpty()) return 0

        // Parse list of unique calendar days in descending order
        val tz = TimeZone.getDefault()
        val calendar = Calendar.getInstance(tz)

        val uniqueWorkoutsDays = historyList.map { item ->
            calendar.timeInMillis = item.timestamp
            val year = calendar.get(Calendar.YEAR)
            val dayOfYear = calendar.get(Calendar.DAY_OF_YEAR)
            year * 1000 + dayOfYear
        }.distinct().sortedDescending()

        if (uniqueWorkoutsDays.isEmpty()) return 0

        // Check today and yesterday
        calendar.time = Date()
        val todayCompact = calendar.get(Calendar.YEAR) * 1000 + calendar.get(Calendar.DAY_OF_YEAR)

        calendar.add(Calendar.DAY_OF_YEAR, -1)
        val yesterdayCompact = calendar.get(Calendar.YEAR) * 1000 + calendar.get(Calendar.DAY_OF_YEAR)

        val firstWorkoutDayValue = uniqueWorkoutsDays[0]

        // If the latest workout is older than yesterday, the streak has expired/broken
        if (firstWorkoutDayValue != todayCompact && firstWorkoutDayValue != yesterdayCompact) {
            return 0
        }

        // Count consecutive days
        var streakValue = 1
        var previousDayValue = firstWorkoutDayValue

        val testCalendar = Calendar.getInstance(tz)
        for (i in 1 until uniqueWorkoutsDays.size) {
            val currentDayValue = uniqueWorkoutsDays[i]

            // Convert previousDayValue to dates to check if they are exactly adjacent
            val prevYear = previousDayValue / 1000
            val prevDoy = previousDayValue % 1000
            testCalendar.set(Calendar.YEAR, prevYear)
            testCalendar.set(Calendar.DAY_OF_YEAR, prevDoy)
            testCalendar.add(Calendar.DAY_OF_YEAR, -1) // check consecutive previous day

            val targetDoyCheck = testCalendar.get(Calendar.YEAR) * 1000 + testCalendar.get(Calendar.DAY_OF_YEAR)

            if (currentDayValue == targetDoyCheck) {
                streakValue++
                previousDayValue = currentDayValue
            } else {
                break
            }
        }
        return streakValue
    }
}
