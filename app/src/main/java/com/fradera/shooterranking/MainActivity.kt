package com.marcfradera.shooterranking

import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import com.marcfradera.shooterranking.data.FirebaseProvider
import com.marcfradera.shooterranking.databinding.ActivityMainBinding
import com.marcfradera.shooterranking.ui.vm.AuthViewModel

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val authViewModel by viewModels<AuthViewModel>()

    private var adView: AdView? = null
    private var adsInitialized = false
    private var currentDestinationId: Int? = null

    companion object {
        // Usa este ID SOLO para pruebas.
        private const val TEST_BANNER_AD_UNIT_ID =
            "ca-app-pub-3940256099942544/9214589741"

        private val NO_AD_DESTINATIONS = setOf(
            R.id.loginFragment,
            R.id.signupFragment,
            R.id.verifyFragment
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        FirebaseProvider.initialize(applicationContext)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initAds()

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

        navController.addOnDestinationChangedListener { _, destination, _ ->
            currentDestinationId = destination.id
            refreshBannerForDestination(destination.id)
        }

        currentDestinationId = navController.currentDestination?.id
        refreshBannerForDestination(currentDestinationId)
    }

    private fun initAds() {
        MobileAds.initialize(this) {
            adsInitialized = true
            refreshBannerForDestination(currentDestinationId)
        }
    }

    private fun refreshBannerForDestination(destinationId: Int?) {
        if (destinationId == null) return

        if (destinationId in NO_AD_DESTINATIONS) {
            hideBanner()
            return
        }

        if (!adsInitialized) {
            binding.adContainer.visibility = View.GONE
            return
        }

        binding.adContainer.visibility = View.VISIBLE

        binding.adContainer.post {
            loadBanner()
        }
    }

    private fun loadBanner() {
        destroyBanner()

        val adWidth = calculateAdWidth()

        adView = AdView(this).apply {
            adUnitId = TEST_BANNER_AD_UNIT_ID
            setAdSize(
                AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(
                    this@MainActivity,
                    adWidth
                )
            )
            loadAd(AdRequest.Builder().build())
        }

        binding.adContainer.removeAllViews()
        binding.adContainer.addView(adView)
    }

    private fun calculateAdWidth(): Int {
        val displayMetrics = resources.displayMetrics
        val density = displayMetrics.density

        val adWidthPixels = if (binding.adContainer.width > 0) {
            binding.adContainer.width
        } else {
            displayMetrics.widthPixels
        }

        return (adWidthPixels / density).toInt()
    }

    private fun hideBanner() {
        binding.adContainer.visibility = View.GONE
        destroyBanner()
        binding.adContainer.removeAllViews()
    }

    private fun destroyBanner() {
        adView?.destroy()
        adView = null
    }

    override fun onResume() {
        super.onResume()
        adView?.resume()
    }

    override fun onPause() {
        adView?.pause()
        super.onPause()
    }

    override fun onDestroy() {
        destroyBanner()
        super.onDestroy()
    }
}