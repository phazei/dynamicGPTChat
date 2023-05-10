package com.phazei.dynamicgptchat

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.Menu
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.menu.MenuBuilder
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.NavOptions
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.navigation.NavigationView
import com.phazei.dynamicgptchat.databinding.ActivityMainBinding


class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)

        setContentView(binding.root)
        setSupportActionBar(binding.appBarMain.toolbar)

        val drawerLayout: DrawerLayout = binding.drawerLayout
        val navView: NavigationView = binding.navView
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment_content_main) as NavHostFragment
        val navController = navHostFragment.navController

        // appBarConfiguration = AppBarConfiguration(navController.graph)
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.PromptsFragment,
                R.id.AppSettingsFragment,
                R.id.AboutFragment
            ), drawerLayout
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        binding.AboutFragment.setOnClickListener {
            // navView.setCheckedItem(R.id.AboutFragment)
            navView.menu.performIdentifierAction(R.id.AboutFragment, Menu.FLAG_ALWAYS_PERFORM_CLOSE)
            navController.navigate(R.id.AboutFragment, null, NavOptions.Builder()
                //to distinguish navigation via this action
                .setEnterAnim(R.anim.spin_in_crazy)
                .setExitAnim(R.anim.spin_out_crazy)
                .setPopEnterAnim(R.anim.spin_in_crazy)
                .setPopExitAnim(R.anim.spin_out_crazy)

                .setLaunchSingleTop(true)
                .setRestoreState(false)
                .setPopUpTo(R.id.AboutFragment, true)
                .build()
            )
        }

        navController.addOnDestinationChangedListener { _, destination, _ ->
            drawerLayout.closeDrawer(GravityCompat.START)
            //manually set active view display for "about" menu
            val typedValue = TypedValue()
            if (destination.id == R.id.AboutFragment) {
                binding.root.context.theme.resolveAttribute(com.google.android.material.R.attr.colorPrimary, typedValue, true)
                binding.AboutFragment.setTextColor(typedValue.data)
                binding.aboutMenuBg.visibility = View.VISIBLE
            } else {
                binding.root.context.theme.resolveAttribute(com.google.android.material.R.attr.colorOnSurface, typedValue, true)
                binding.AboutFragment.setTextColor(typedValue.data)
                binding.aboutMenuBg.visibility = View.GONE
            }
            // printNavBackStack(navController)
        }
    }

    /**
     * Just for testing backStack
     */
    // private fun printNavBackStack(navController: NavController) {
    //     val backStack = navController.backQueue
    //     val backStackEntries = backStack.map { entry ->
    //         val destination = entry.destination
    //         "Destination id: ${destination.id}, Destination label: ${destination.label}"
    //     }.joinToString("\n")
    //     val backStackEntryCount = supportFragmentManager.backStackEntryCount
    //     val fragmentCount = supportFragmentManager.fragments.size
    //     Log.d("TAG", "DestinationListener: Back stack entry count: $backStackEntryCount, Fragment count: $fragmentCount")
    //     Log.d("TAG", "Navigation Back Stack:\n$backStackEntries")
    // }

    @SuppressLint("RestrictedApi")
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        if (menu is MenuBuilder) {
            // by default it will hide any icons in the menu dropdown
            menu.setOptionalIconsVisible(true)
        }
        return true
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

}

