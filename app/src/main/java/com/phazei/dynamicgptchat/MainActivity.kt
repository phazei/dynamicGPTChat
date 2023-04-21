package com.phazei.dynamicgptchat

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.menu.MenuBuilder
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.navigation.NavigationView
import com.phazei.dynamicgptchat.chatnodes.ChatNodeViewModel
import com.phazei.dynamicgptchat.databinding.ActivityMainBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    private val sharedViewModel: SharedViewModel by viewModels()
    //this needs to be bound to activity so it will stay active when switching fragments
    private val chatNodeViewModel: ChatNodeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //sharedViewModel must be in onCreate or it will not be created in time for the fragment
        //leaving sharedViewModelFactory in case it's every needed in activityViewModels() in a fragment
        sharedViewModel
        chatNodeViewModel

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
                R.id.ChatTreeListFragment,
                R.id.prompts_graph,
                R.id.app_settings_fragment
            ), drawerLayout
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        setupFABPageChangeFixes(navController)

        // navController.addOnDestinationChangedListener { _, destination, _ ->
        //     printNavBackStack(navController)
        // }

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

    private fun setupFABPageChangeFixes(navController: NavController) {
        val fab = binding.appBarMain.floatingActionButton

        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.ChatTreeListFragment -> {
                    fab.show()
                    updateAppBarScrollFlags(true)
                }
                else -> {
                    fab.hide()
                    updateAppBarScrollFlags(false)
                }
            }
        }
        fab.setOnClickListener {
            //this enables the FAB to be clicked and trigger a method that can be assigned in any Fragment
            sharedViewModel.onFabClick.value?.invoke()
        }
    }

    @SuppressLint("RestrictedApi")
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        if (menu is MenuBuilder) {
            //by default it will hide any icons in the menu dropdown
            menu.setOptionalIconsVisible(true)
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.app_settings_fragment -> true
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    /**
     * Scroll flags break all pages but ChatTrees, so only enable it on that page
     */
    private fun updateAppBarScrollFlags(isScrollEnabled: Boolean) {
        val toolbar = binding.appBarMain.toolbar
        val params = toolbar.layoutParams as AppBarLayout.LayoutParams
        if (isScrollEnabled) {
            params.scrollFlags = (AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL
                    or AppBarLayout.LayoutParams.SCROLL_FLAG_ENTER_ALWAYS
                    or AppBarLayout.LayoutParams.SCROLL_FLAG_SNAP)
        } else {
            params.scrollFlags = 0
        }
        toolbar.layoutParams = params
    }
}

