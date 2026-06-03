package com.swordfish.lemuroid.lib.updates

data class AppUpdateNotice(
    val marketUrl: String = DEFAULT_MARKET_URL,
    val webUrl: String = DEFAULT_WEB_URL,
) {
    companion object {
        const val DEFAULT_MARKET_URL = "market://details?id=com.rolemiaster.emulaitor"
        const val DEFAULT_WEB_URL = "https://play.google.com/store/apps/details?id=com.rolemiaster.emulaitor"
    }
}
