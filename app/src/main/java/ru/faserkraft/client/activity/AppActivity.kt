package ru.faserkraft.client.activity

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.navOptions
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import ru.faserkraft.client.R
import ru.faserkraft.client.databinding.AppActivityBinding
import ru.faserkraft.client.model.UserData
import ru.faserkraft.client.model.VersionInfo
import ru.faserkraft.client.update.AppUpdateManager
import ru.faserkraft.client.utils.getToday
import ru.faserkraft.client.viewmodel.ScannerViewModel
import ru.faserkraft.client.viewmodel.UpdateViewModel

@AndroidEntryPoint
class AppActivity : AppCompatActivity() {

    private lateinit var navController: NavController

    private val viewModel: ScannerViewModel by viewModels()
    private val updateViewModel: UpdateViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = AppActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        binding.bottomNav.setupWithNavController(navController)

        binding.bottomNav.setOnItemSelectedListener { item ->
            val options = navOptions {
                popUpTo(navController.graph.startDestinationId) {
                    inclusive = true
                }
                launchSingleTop = true
            }

            when (item.itemId) {
                R.id.registrationFragment -> {
                    navController.navigate(R.id.registrationFragment, null, options)
                    true
                }

                R.id.scannerFragment -> {
                    navController.navigate(R.id.scannerFragment, null, options)
                    true
                }

                R.id.dayPlanFragment -> {
                    viewModel.getDayPlans(getToday())
                    val currentId = navController.currentDestination?.id
                    if (currentId != R.id.dayPlanFragment) {
                        navController.navigate(R.id.dayPlanFragment, null, options)
                    }
                    true
                }

                R.id.inventoryFragment -> {
                    val currentId = navController.currentDestination?.id
                    if (currentId != R.id.productsInventoryFragment) {
                        navController.navigate(R.id.productsInventoryFragment, null, options)
                    }
                    true
                }

                R.id.storageFragment -> {
                    val currentId = navController.currentDestination?.id
                    if (currentId != R.id.storageContainerFragment) {
                        navController.navigate(R.id.storageContainerFragment, null, options)
                    }
                    true
                }

                else -> false
            }
        }

        viewModel.userData.observe(this) { user ->
            if (user != null) {
                checkForUpdates(user)
            }
        }

    }

    private fun checkForUpdates(user: UserData) {
        updateViewModel.checkForUpdates(user)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                updateViewModel.updateAvailable.collect { version ->
                    version ?: return@collect
                    showUpdateDialog(version)
                }
            }
        }
    }


    private fun showUpdateDialog(version: VersionInfo) {
        val status = updateViewModel.status.value
        if (status is AppUpdateManager.UpdateStatus.Downloading ||
            status is AppUpdateManager.UpdateStatus.Installing
        ) return

        MaterialAlertDialogBuilder(this)
            .setTitle("Доступно обновление ${version.versionName}")
            .setMessage(version.changelog)
            .setCancelable(!version.forceUpdate)
            .setPositiveButton("Обновить") { _, _ ->
                updateViewModel.startUpdate(version)
            }
            .apply {
                if (!version.forceUpdate) {
                    setNegativeButton("Позже") { dialog, _ -> dialog.dismiss() }
                }
            }
            .show()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        navController.navigate(R.id.scannerFragment)
    }
}