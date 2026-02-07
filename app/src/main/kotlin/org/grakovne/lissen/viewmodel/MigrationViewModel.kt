package org.grakovne.lissen.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.grakovne.lissen.content.cache.persistent.dao.CachedBookDao
import org.grakovne.lissen.persistence.preferences.LissenSharedPreferences
import javax.inject.Inject

@HiltViewModel
class MigrationViewModel
  @Inject
  constructor(
    private val bookDao: CachedBookDao,
    private val preferences: LissenSharedPreferences,
  ) : ViewModel() {
    private val _migrationState = MutableLiveData<MigrationState>(MigrationState.Idle)
    val migrationState: LiveData<MigrationState> = _migrationState

    fun startMigration() {
      if (_migrationState.value != MigrationState.Idle) {
        return
      }

      executeMigration()
    }

    fun retryMigration() {
      if (_migrationState.value == MigrationState.Running) {
        return
      }

      executeMigration()
    }

    private fun executeMigration() {
      viewModelScope.launch {
        _migrationState.value = MigrationState.Running

        // Artificial delay for a better UX if migration is target-fast
        delay(1500)
        try {
          withContext(Dispatchers.IO) {
            // Trigger DB initialization and migration by performing a simple query
            bookDao.countCachedBooks(null, "", "")
          }

          preferences.setDatabaseVersion(CURRENT_DATABASE_VERSION)
          _migrationState.value = MigrationState.Completed
        } catch (e: Exception) {
          if (e is kotlinx.coroutines.CancellationException) {
            throw e
          }

          timber.log.Timber.e(e, "Migration failed")
          _migrationState.value = MigrationState.Error(e)
        }
      }
    }

    companion object {
      const val CURRENT_DATABASE_VERSION = 19
    }
  }

sealed class MigrationState {
  data object Idle : MigrationState()

  data object Running : MigrationState()

  data object Completed : MigrationState()

  data class Error(
    val throwable: Throwable,
  ) : MigrationState()
}
