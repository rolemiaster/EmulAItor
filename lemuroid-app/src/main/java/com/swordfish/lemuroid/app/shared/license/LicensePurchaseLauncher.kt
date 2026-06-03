package com.swordfish.lemuroid.app.shared.license

import android.app.Activity
import android.content.Intent
import android.net.Uri

fun Activity.openLicensePurchasePageAndFinish() {
    val marketIntent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.rolemiaster.emulaitor")).apply {
        setPackage("com.android.vending")
    }
    val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.rolemiaster.emulaitor"))

    runCatching {
        startActivity(marketIntent)
    }.recoverCatching {
        startActivity(webIntent)
    }

    finish()
}
