package ru.faserkraft.client.activity

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.navOptions
import androidx.navigation.ui.setupWithNavController
import dagger.hilt.android.AndroidEntryPoint
import ru.faserkraft.client.R
import ru.faserkraft.client.databinding.AppActivityBinding
import ru.faserkraft.client.viewmodel.ScannerViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@AndroidEntryPoint
class AppActivity : AppCompatActivity() {

    private lateinit var navController: NavController

    private val viewModel: ScannerViewModel by viewModels()

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
                    inclusive = true   // очищаем весь стек до стартового dest
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
                    val apiFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    val datePlan = apiFormat.format(Date())
                    viewModel.getDayPlans(datePlan)
                    val currentId = navController.currentDestination?.id
                    if (currentId != R.id.dayPlanFragment) {
                        navController.navigate(R.id.dayPlanFragment, null, options)
                    }
                    true
                }

                else -> false
            }
        }

    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Вернуться на ScannerFragment при новом интенте
        navController.navigate(R.id.scannerFragment)
    }
}
