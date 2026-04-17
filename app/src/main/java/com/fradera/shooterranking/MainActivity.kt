package com.marcfradera.shooterranking

import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.marcfradera.shooterranking.data.FirebaseProvider
import com.marcfradera.shooterranking.databinding.ActivityMainBinding
import com.marcfradera.shooterranking.ui.vm.AuthViewModel

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val authViewModel by viewModels<AuthViewModel>()

    private var adView: AdView? = null
    private var adsInitialized = false
    private var currentDestinationId: Int? = null
    private var lastLoadedBannerDestinationId: Int? = null

    private fun shouldEnableAds(): Boolean = true

    private val bannerLoadRunnable = Runnable {
        if (::binding.isInitialized.not()) return@Runnable
        if (isFinishing || isDestroyed) return@Runnable
        if (!binding.adContainer.isAttachedToWindow) return@Runnable
        if (binding.adContainer.visibility != View.VISIBLE) return@Runnable
        if (!adsInitialized) return@Runnable

        loadBanner()
    }

    companion object {
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

        if (shouldEnableAds() && canUseGooglePlayServices()) {
            initAds()
        } else {
            adsInitialized = false
            binding.adContainer.visibility = View.GONE
        }

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

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val navigatedBack = navController.popBackStack()

                if (!navigatedBack) {
                    moveTaskToBack(true)
                }
            }
        })
    }

    private fun canUseGooglePlayServices(): Boolean {
        val status = GoogleApiAvailability.getInstance()
            .isGooglePlayServicesAvailable(this)
        return status == ConnectionResult.SUCCESS
    }

    private fun initAds() {
        try {
            MobileAds.initialize(this) {
                adsInitialized = true
                refreshBannerForDestination(currentDestinationId)
            }
        } catch (_: Exception) {
            adsInitialized = false
            if (::binding.isInitialized) {
                binding.adContainer.visibility = View.GONE
                binding.adContainer.removeCallbacks(bannerLoadRunnable)
            }
        }
    }

    private fun refreshBannerForDestination(destinationId: Int?) {
        if (!::binding.isInitialized) return

        binding.adContainer.removeCallbacks(bannerLoadRunnable)

        if (destinationId == null) return

        if (destinationId in NO_AD_DESTINATIONS) {
            hideBanner()
            lastLoadedBannerDestinationId = null
            return
        }

        if (!shouldEnableAds() || !adsInitialized || !canUseGooglePlayServices()) {
            binding.adContainer.visibility = View.GONE
            return
        }

        if (isFinishing || isDestroyed) return

        binding.adContainer.visibility = View.VISIBLE

        val bannerAlreadyLoadedForThisDestination =
            adView != null &&
                    lastLoadedBannerDestinationId == destinationId &&
                    binding.adContainer.childCount > 0

        if (bannerAlreadyLoadedForThisDestination) return

        binding.adContainer.post(bannerLoadRunnable)
    }

    private fun loadBanner() {
        if (!::binding.isInitialized) return
        if (isFinishing || isDestroyed) return
        if (!binding.adContainer.isAttachedToWindow) return

        if (!shouldEnableAds() || !canUseGooglePlayServices()) {
            hideBanner()
            return
        }

        val adWidth = calculateAdWidth()
        if (adWidth <= 0) {
            binding.adContainer.removeCallbacks(bannerLoadRunnable)
            binding.adContainer.post(bannerLoadRunnable)
            return
        }

        try {
            destroyBanner()

            val newAdView = AdView(this).apply {
                adUnitId = TEST_BANNER_AD_UNIT_ID
                setAdSize(
                    AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(
                        this@MainActivity,
                        adWidth
                    )
                )
            }

            adView = newAdView

            binding.adContainer.removeAllViews()
            binding.adContainer.addView(newAdView)

            newAdView.loadAd(AdRequest.Builder().build())
            lastLoadedBannerDestinationId = currentDestinationId
        } catch (_: Exception) {
            hideBanner()
        }
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
        if (!::binding.isInitialized) return

        binding.adContainer.removeCallbacks(bannerLoadRunnable)
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
        if (::binding.isInitialized) {
            binding.adContainer.removeCallbacks(bannerLoadRunnable)
        }
        adView?.pause()
        super.onPause()
    }

    override fun onDestroy() {
        if (::binding.isInitialized) {
            binding.adContainer.removeCallbacks(bannerLoadRunnable)
        }
        destroyBanner()
        super.onDestroy()
    }
}