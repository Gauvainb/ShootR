package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.TirTrackerDatabase
import com.example.data.TirTrackerRepository
import com.example.data.model.ShootingSessionEntity
import com.example.data.model.ShotEntity
import com.example.data.model.WeaponEntity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class TirTrackerViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: TirTrackerRepository

    init {
        val db = TirTrackerDatabase.getDatabase(application)
        repository = TirTrackerRepository(db.sessionDao(), db.weaponDao())
    }

    val sessions: StateFlow<List<ShootingSessionEntity>> = repository.allSessions.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val weapons: StateFlow<List<WeaponEntity>> = repository.allWeapons.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val totalSessionsCount: StateFlow<Int> = repository.totalSessionsCount.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        0
    )

    val totalShotsCount: StateFlow<Int> = repository.totalShotsCount.flatMapLatest { count ->
        flowOf(count ?: 0)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    private val _currentSessionId = MutableStateFlow<Long?>(null)
    val currentSessionId: StateFlow<Long?> = _currentSessionId.asStateFlow()

    val currentSession: StateFlow<ShootingSessionEntity?> = _currentSessionId.flatMapLatest { id ->
        if (id != null) repository.getSession(id) else flowOf(null)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val currentShots: StateFlow<List<ShotEntity>> = _currentSessionId.flatMapLatest { id ->
        if (id != null) repository.getShotsForSession(id) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun selectSession(sessionId: Long) {
        _currentSessionId.value = sessionId
    }

    fun createSession(
        title: String,
        targetType: String,
        distanceMeters: Int,
        weapon: WeaponEntity?,
        ammoBrand: String,
        ammoCaliber: String,
        position: String,
        notes: String,
        onCreated: (Long) -> Unit
    ) {
        viewModelScope.launch {
            val newSession = ShootingSessionEntity(
                title = title.ifBlank { "Séance $targetType" },
                targetType = targetType,
                distanceMeters = distanceMeters,
                weaponId = weapon?.id,
                weaponName = weapon?.name ?: "",
                ammoBrand = ammoBrand,
                ammoCaliber = ammoCaliber.ifBlank { weapon?.caliber ?: "" },
                position = position,
                notes = notes
            )
            val newId = repository.saveSession(newSession)
            _currentSessionId.value = newId
            onCreated(newId)
        }
    }

    fun updateSession(session: ShootingSessionEntity) {
        viewModelScope.launch {
            repository.saveSession(session)
        }
    }

    fun deleteSession(session: ShootingSessionEntity) {
        viewModelScope.launch {
            repository.deleteSession(session)
            if (_currentSessionId.value == session.id) {
                _currentSessionId.value = null
            }
        }
    }

    fun addShotToCurrentSession(xRatio: Float, yRatio: Float, targetDiameterMm: Float = 500f) {
        val sessionId = _currentSessionId.value ?: return
        viewModelScope.launch {
            repository.addShot(sessionId, xRatio, yRatio, targetDiameterMm)
        }
    }

    fun removeLastShot() {
        val sessionId = _currentSessionId.value ?: return
        viewModelScope.launch {
            repository.removeLastShot(sessionId)
        }
    }

    fun clearCurrentSessionShots() {
        val sessionId = _currentSessionId.value ?: return
        viewModelScope.launch {
            repository.clearSessionShots(sessionId)
        }
    }

    fun updateSessionPhoto(sessionId: Long, photoUri: String?) {
        viewModelScope.launch {
            val session = currentSession.value
            if (session != null && session.id == sessionId) {
                repository.saveSession(session.copy(photoUri = photoUri))
            }
        }
    }

    fun saveWeapon(
        id: Long = 0,
        name: String,
        type: String,
        caliber: String,
        sights: String,
        serialNumber: String,
        notes: String
    ) {
        viewModelScope.launch {
            val weapon = WeaponEntity(
                id = id,
                name = name,
                type = type,
                caliber = caliber,
                sights = sights,
                serialNumber = serialNumber,
                notes = notes
            )
            repository.saveWeapon(weapon)
        }
    }

    fun deleteWeapon(weapon: WeaponEntity) {
        viewModelScope.launch {
            repository.deleteWeapon(weapon)
        }
    }
}
