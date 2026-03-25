package com.marcfradera.shooterranking

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import com.marcfradera.shooterranking.data.FirebaseProvider
import com.marcfradera.shooterranking.databinding.ActivityMainBinding
import com.marcfradera.shooterranking.ui.vm.AuthViewModel

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val authViewModel by viewModels<AuthViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        FirebaseProvider.initialize(applicationContext)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment

        val navController = navHostFragment.navController
        val navGraph = navController.navInflater.inflate(R.navigation.main_nav_graph)

        val startDestination = when {
            authViewModel.isLoggedIn && authViewModel.emailConfirmed ->
                R.id.temporadesFragment

            authViewModel.isLoggedIn && !authViewModel.emailConfirmed ->
                R.id.verifyFragment

            else ->
                R.id.welcomeFragment
        }

        navGraph.setStartDestination(startDestination)
        navController.setGraph(navGraph, null)
    }
}