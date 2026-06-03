package com.swordfish.lemuroid.lib.updates

import android.app.Activity
import android.content.Intent
import android.net.Uri

fun Activity.openUpdateStore(notice: AppUpdateNotice) {
    val marketIntent = Intent(Intent.ACTION_VIEW, Uri.parse(notice.marketUrl)).apply {
        setPackage("com.android.vending")
    }
    val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse(notice.webUrl))

    runCatching {
        startActivity(marketIntent)
    }.recoverCatching {
        startActivity(webIntent)
    }
}
