package com.worklog.quickrecord

import android.app.Application

class QuickRecordApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
