package ru.faserkraft.client.activity

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import ru.faserkraft.client.R
import ru.faserkraft.client.data.update.UpdateStatus
import ru.faserkraft.client.databinding.AppActivityBinding
import ru.faserkraft.client.domain.model.VersionInfo
import ru.faserkraft.client.presentation.app.AppViewModel
import ru.faserkraft.client.presentation.update.UpdateUiEvent
import ru.faserkraft.client.presentation.update.UpdateViewModel

@AndroidEntryPoint
class AppActivity : AppCompatActivity() {

    private lateinit var navController: NavController
    private val appViewModel: AppViewModel by viewModels()
    private val updateViewModel: UpdateViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = AppActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        setupBottomNav(binding.bottomNav)
        triggerUpdateCheckOnce()
        observeUpdateEvents()
        observeUpdateStatus()
    }

    private fun setupBottomNav(bottomNav: BottomNavigationView) {
        bottomNav.setOnItemSelectedListener { item ->
            val navOptions = NavOptions.Builder()
                .setLaunchSingleTop(true)
                .setPopUpTo(item.itemId, inclusive = true)
                .build()
            navController.navigate(item.itemId, null, navOptions)
            true
        }

        navController.addOnDestinationChangedListener { _, destination, _ ->
            val menuItemIds = setOf(
                R.id.registrationFragment,
                R.id.dayPlanFragment,
                R.id.scannerFragment,
                R.id.productsInventoryFragment,
                R.id.storageContainerFragment
            )
            if (destination.id in menuItemIds) {
                bottomNav.menu.findItem(destination.id)?.isChecked = true
            }
        }
    }

    private fun triggerUpdateCheckOnce() {
        lifecycleScope.launch {
            val user = appViewModel.userData.value
                ?: appViewModel.userData.firstOrNull { it != null }
                ?: return@launch

            updateViewModel.checkForUpdates(user)
        }
    }

    private fun observeUpdateEvents() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                updateViewModel.events.collect { event ->
                    when (event) {
                        is UpdateUiEvent.ShowUpdateDialog -> showUpdateDialog(event.version)
                        is UpdateUiEvent.ShowError -> showUpdateErrorDialog(event.message)
                    }
                }
            }
        }
    }

    private fun observeUpdateStatus() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                updateViewModel.status.collect { status ->
                    if (status is UpdateStatus.Error) {
                        showUpdateErrorDialog(status.message)
                    }
                }
            }
        }
    }

    private fun showUpdateDialog(versionInfo: VersionInfo) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Доступно обновление")
            .setMessage(
                buildString {
                    append("Версия ${versionInfo.versionName}")
                    if (versionInfo.changelog.isNotBlank()) {
                        append("\n\n")
                        append(versionInfo.changelog)
                    }
                }
            )
            .setPositiveButton("Обновить") { _, _ ->
                updateViewModel.startUpdate(versionInfo)
            }
            .setNegativeButton("Позже", null)
            .show()
    }

    private fun showUpdateErrorDialog(message: String) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Ошибка обновления")
            .setMessage(message)
            .setPositiveButton("ОК", null)
            .show()
    }
}