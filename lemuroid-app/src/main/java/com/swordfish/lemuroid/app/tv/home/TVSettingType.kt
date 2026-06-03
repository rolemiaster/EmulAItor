package com.swordfish.lemuroid.app.tv.home

import com.swordfish.lemuroid.R

enum class TVSettingType(val icon: Int, val text: Int) {
    METADATA_ENRICHMENT(R.drawable.ic_cloud_sync_64dp, R.string.library_metadata_enrichment_in_progress),
    HD_MODE_ON(R.drawable.ic_hd_white_64dp, R.string.home_hd_mode_enabled),
    HD_MODE_OFF(R.drawable.ic_hd_off_white_64dp, R.string.home_hd_mode_disabled),
    STOP_RESCAN(R.drawable.ic_stop_white_64dp, R.string.stop),
    RESCAN(R.drawable.ic_refresh_white_64dp, R.string.rescan),
    SHOW_ALL_FAVORITES(R.drawable.ic_more_games, R.string.show_all),
    CHOOSE_DIRECTORY(R.drawable.ic_folder_white_64dp, R.string.directory),
    SETTINGS(R.drawable.ic_settings_white_64dp, R.string.settings),
    SAVE_SYNC(R.drawable.ic_cloud_sync_64dp, R.string.save_sync),
    CATALOG(R.drawable.ic_cloud_sync_64dp, R.string.title_catalog),
    MANUAL(R.drawable.ic_book_white_64dp, R.string.settings_title_manual),
    ABOUT(R.drawable.ic_help, R.string.settings_title_about),
}
