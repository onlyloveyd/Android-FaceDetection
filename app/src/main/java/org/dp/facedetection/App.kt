package org.dp.facedetection

import android.app.Application

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        FaceDetect.init()
    }
}