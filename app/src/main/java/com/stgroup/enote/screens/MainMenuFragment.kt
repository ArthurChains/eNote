package com.stgroup.enote.screens

import androidx.fragment.app.Fragment
import com.stgroup.enote.utilities.APP_ACTIVITY

class MainMenuFragment : Fragment() {

    override fun onResume() {
        super.onResume()
        APP_ACTIVITY.mDrawer.enableDrawer()
    }
}