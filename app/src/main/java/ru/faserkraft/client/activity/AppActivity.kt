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
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import ru.faserkraft.client.R
import ru.faserkraft.client.databinding.AppActivityBinding
import ru.faserkraft.client.model.VersionInfo
import ru.faserkraft.client.presentation.order.OrderViewModel
import ru.faserkraft.client.presentation.packaging.PackagingViewModel
import ru.faserkraft.client.presentation.plan.PlanViewModel
import ru.faserkraft.client.presentation.product.ProductViewModel
import ru.faserkraft.client.presentation.scanner.ScannerViewModel
import ru.faserkraft.client.viewmodel.UpdateViewModel

@AndroidEntryPoint
class AppActivity : AppCompatActivity() {

    private lateinit var navController: NavController

    // Новые feature-ViewModels — инстанцируются на уровне Activity,
    // чтобы фрагменты могли получить их через activityViewModels()
    val scannerViewModel: ScannerViewModel by viewModels()
    val productViewModel: ProductViewModel by viewModels()
    val packagingViewModel: PackagingViewModel by viewModels()
    val orderViewModel: OrderViewModel by viewModels()
    val planViewModel: PlanViewModel by viewModels()

    private val updateViewModel: UpdateViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = AppActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        binding.bottomNav.setupWithNavController(navController)

        observeUpdateViewModel()
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