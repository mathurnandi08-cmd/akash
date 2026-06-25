package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.EngagementEvent
import com.example.data.EventRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.random.Random

class AnalyticsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: EventRepository

    // Filter states
    private val _selectedPlatform = MutableStateFlow("All")
    val selectedPlatform = _selectedPlatform.asStateFlow()

    private val _selectedEventType = MutableStateFlow("All")
    val selectedEventType = _selectedEventType.asStateFlow()

    // Simulation states
    private val _isSimulating = MutableStateFlow(true)
    val isSimulating = _isSimulating.asStateFlow()

    private val _simulationSpeedMultiplier = MutableStateFlow(1.0) // 1x, 2x, 5x
    val simulationSpeedMultiplier = _simulationSpeedMultiplier.asStateFlow()

    // Selected event for detail dialog
    private val _selectedEvent = MutableStateFlow<EngagementEvent?>(null)
    val selectedEvent = _selectedEvent.asStateFlow()

    // Total events in memory / database
    val totalEventCount: StateFlow<Int>

    // All events, filtered reactively in memory for real-time reactivity
    val filteredEvents: StateFlow<List<EngagementEvent>>

    // Simulated user list to maintain consistent session profiles
    private val simulatedUsers = List(150) { index ->
        SimulatedUserProfile(
            userId = "USR_${1000 + index}",
            platform = when (index % 3) {
                0 -> "Android"
                1 -> "iOS"
                else -> "Web"
            },
            country = when (index % 7) {
                0 -> "US"
                1 -> "IN"
                2 -> "DE"
                3 -> "JP"
                4 -> "UK"
                5 -> "CA"
                else -> "FR"
            }
        )
    }

    private var simulationJob: Job? = null

    init {
        val database = AppDatabase.getDatabase(application)
        repository = EventRepository(database.eventDao())

        totalEventCount = repository.totalEventCount.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

        // Combine database events flow with user filters
        filteredEvents = combine(
            repository.allEvents,
            _selectedPlatform,
            _selectedEventType
        ) { events, platform, eventType ->
            events.filter { event ->
                val platformMatch = platform == "All" || event.platform.equals(platform, ignoreCase = true)
                val typeMatch = eventType == "All" || event.eventType.equals(eventType, ignoreCase = true)
                platformMatch && typeMatch
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        // Seed some initial historical events so the dashboard isn't empty on launch
        seedHistoricalData()

        // Start real-time simulation background coroutine
        startSimulation()
    }

    private fun seedHistoricalData() {
        viewModelScope.launch {
            val count = totalEventCount.value
            if (count < 50) {
                val initialEvents = mutableListOf<EngagementEvent>()
                val now = System.currentTimeMillis()
                // Seed 200 events over the past 10 minutes
                for (i in 0 until 200) {
                    val ageMs = Random.nextLong(10 * 60 * 1000) // up to 10 mins ago
                    initialEvents.add(createRandomEvent(now - ageMs))
                }
                repository.insertMultiple(initialEvents)
            }
        }
    }

    private fun startSimulation() {
        simulationJob?.cancel()
        simulationJob = viewModelScope.launch {
            while (true) {
                if (_isSimulating.value) {
                    val delayMs = (1000 / _simulationSpeedMultiplier.value).toLong()
                    delay(delayMs)

                    // Insert 1 to 3 random events per simulation tick to make it lively
                    val batchSize = Random.nextInt(1, 4)
                    val newEvents = List(batchSize) {
                        createRandomEvent(System.currentTimeMillis())
                    }
                    repository.insertMultiple(newEvents)

                    // Prune database to last 1000 events to prevent growth
                    repository.prune(1000)
                } else {
                    delay(500)
                }
            }
        }
    }

    private fun createRandomEvent(timestamp: Long): EngagementEvent {
        val user = simulatedUsers.random()
        val eventType = when (Random.nextInt(100)) {
            in 0..15 -> "session_start"
            in 16..55 -> "view"
            in 56..85 -> "click"
            in 86..94 -> "purchase"
            else -> "error"
        }

        val (element, value) = when (eventType) {
            "session_start" -> Pair("AppLaunch", 0.0)
            "view" -> {
                val screens = listOf("Home", "Catalog", "ProductDetail", "Cart", "Checkout", "Settings")
                Pair(screens.random(), 0.0)
            }
            "click" -> {
                val buttons = listOf("AddToCart", "ApplyPromo", "SearchIcon", "FilterToggle", "ShareButton", "HelpDesk")
                Pair(buttons.random(), 0.0)
            }
            "purchase" -> {
                Pair("PaymentGateway", Random.nextDouble(10.0, 249.99))
            }
            "error" -> {
                val errorDetails = listOf("NetworkTimeout", "PaymentDeclined", "AssetLoadFailed", "SessionExpired")
                Pair(errorDetails.random(), 500.0) // 500 status code as value
            }
            else -> Pair("Unknown", 0.0)
        }

        return EngagementEvent(
            userId = user.userId,
            eventType = eventType,
            elementName = element,
            platform = user.platform,
            country = user.country,
            timestamp = timestamp,
            value = value
        )
    }

    // Public actions
    fun setPlatformFilter(platform: String) {
        _selectedPlatform.value = platform
    }

    fun setEventTypeFilter(eventType: String) {
        _selectedEventType.value = eventType
    }

    fun toggleSimulation() {
        _isSimulating.value = !_isSimulating.value
    }

    fun setSimulationSpeed(multiplier: Double) {
        _simulationSpeedMultiplier.value = multiplier
    }

    fun setSelectedEvent(event: EngagementEvent?) {
        _selectedEvent.value = event
    }

    fun injectTrafficSpike() {
        viewModelScope.launch {
            val spikeEvents = List(45) {
                // Generate high volume of purchases/clicks over the last 15 seconds
                createRandomEvent(System.currentTimeMillis() - Random.nextLong(15000))
            }
            repository.insertMultiple(spikeEvents)
        }
    }

    fun clearLogs() {
        viewModelScope.launch {
            repository.clear()
        }
    }

    // Track actual clicks from the user in this dashboard app
    fun logDashboardEvent(eventType: String, elementName: String) {
        viewModelScope.launch {
            val realEvent = EngagementEvent(
                userId = "YOU_CURRENT_USER",
                eventType = eventType,
                elementName = elementName,
                platform = "Android",
                country = "US", // Local representation
                timestamp = System.currentTimeMillis(),
                value = 1.0 // Signifies actual user engagement
            )
            repository.insert(realEvent)
        }
    }

    override fun onCleared() {
        super.onCleared()
        simulationJob?.cancel()
    }

    private data class SimulatedUserProfile(
        val userId: String,
        val platform: String,
        val country: String
    )
}
