package com.example.mlkit_ws

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.fragment.app.Fragment
import com.google.android.material.navigation.NavigationView
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*

private var currFragment: Fragment = Sample_1()
private var currTitle: String = "Object Detector"

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setSupportActionBar(toolbar)

        val toggle = ActionBarDrawerToggle(
            this,
            drawerLayout,
            toolbar,
            R.string.open,
            R.string.close
        )
        toggle.isDrawerIndicatorEnabled = true
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        nav_menu.setNavigationItemSelectedListener(this)

        changCurrFragment(currTitle, currFragment)
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        drawerLayout.closeDrawer(GravityCompat.START)
        println(item.itemId)

        when(item.itemId) {
            R.id.sample_1 -> {
                changCurrFragment("Object Detector", Sample_1())
            }
            R.id.sample_2 -> {
                changCurrFragment("Which Plant?", Sample_2())
            }
            R.id.sample_3 -> {
                changCurrFragment("Travel Translator", Sample_3())
            }
            R.id.sample_4 -> {
                changCurrFragment("Barcode Scanner", Sample_4())
            }
        }

        return true
    }

    private fun changCurrFragment(title: String, frag: Fragment) {
        println("now")
        currTitle = title
        currFragment = frag

        setFragment(title, frag)
    }

    private fun setFragment(title: String, frag: Fragment) {
        setToolbarTitle(title)
        changeFragment(frag)
    }

    private fun setToolbarTitle(title: String) {
        supportActionBar?.title = title
    }

    private fun changeFragment(frag: Fragment) {
        val fragment = supportFragmentManager.beginTransaction()
        fragment.replace(R.id.fragment_container, frag).commit()
    }
}