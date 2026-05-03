package ru.faserkraft.client.activity

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import ru.faserkraft.client.R
import ru.faserkraft.client.databinding.AppActivityBinding
import ru.faserkraft.client.model.VersionInfo
import ru.faserkraft.client.viewmodel.UpdateViewModel

@AndroidEntryPoint
class AppActivity : AppCompatActivity() {

    private lateinit var navController: NavController
    private val updateViewModel: UpdateViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = AppActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        setupBottomNav(binding.bottomNav)
        observeUpdateViewModel()
    }

    private fun setupBottomNav(bottomNav: com.google.android.material.bottomnavigation.BottomNavigationView) {
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

    private fun observeUpdateViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                updateViewModel.updateAvailable.collect { versionInfo ->
                    versionInfo ?: return@collect
                    showUpdateDialog(versionInfo)
                }
            }
        }
    }

    private fun showUpdateDialog(versionInfo: VersionInfo) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Доступно обновление")
            .setMessage("Версия ${versionInfo.versionName}")
            .setPositiveButton("Обновить") { _, _ ->
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    data = versionInfo.apkFile.toUri()
                }
                startActivity(intent)
            }
            .setNegativeButton("Позже", null)
            .show()
    }
}