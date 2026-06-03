package com.swordfish.lemuroid.app.mobile.feature.paywall

import android.app.Activity
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.swordfish.lemuroid.app.mobile.shared.compose.ui.AppTheme
import com.swordfish.lemuroid.ext.feature.access.AccessManager
import com.swordfish.lemuroid.lib.android.RetrogradeComponentActivity
import javax.inject.Inject

class PaywallActivity : RetrogradeComponentActivity() {
    @Inject
    lateinit var accessManager: AccessManager

    private val viewModel by viewModels<PaywallViewModel> {
        PaywallViewModel.Factory(accessManager, applicationContext)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppTheme {
                PaywallScreen(
                    viewModel = viewModel,
                    onClose = { finish() },
                    activity = this
                )
            }
        }
    }
}
