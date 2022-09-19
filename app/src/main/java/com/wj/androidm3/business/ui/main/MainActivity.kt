package com.wj.androidm3.business.ui.main

import android.content.Intent
import android.os.Bundle
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import com.wj.androidm3.R
import com.wj.androidm3.business.services.WJFCMService
import com.wj.androidm3.databinding.ActivityMainBinding
import com.wj.basecomponent.util.log.WJLog

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navView: BottomNavigationView = binding.navView
        navView.inflateMenu(R.menu.bottom_nav_menu)
        val currentTimeMillis = System.currentTimeMillis()
        WJLog.d("currentTimeMillis -> $currentTimeMillis")
        if ((currentTimeMillis % 2).toInt() == 0) {
            navView.menu.removeItem(R.id.navigation_dashboard2)
        }
        val navController = findNavController(R.id.nav_host_fragment_activity_main)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_home, R.id.navigation_dashboard, R.id.navigation_notifications
            )
        )
//        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

    }
}