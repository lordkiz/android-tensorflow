package com.example.androidtensorflow

import android.os.Handler
import android.os.HandlerThread

internal fun MainActivity.startBackgroundThread() {
    backgroundThread = HandlerThread(MainActivity.HANDLER_THREAD_NAME)
    backgroundThread.start()
    backgroundHandler = Handler(backgroundThread.looper)
    synchronized(lock) {
        runClassifier = true
    }
}

internal fun MainActivity.stopBackgroundThread() {
    try {
        backgroundThread.quit()
        synchronized(lock) {
            runClassifier = false;
        }
    } catch (e: InterruptedException) {
        e.printStackTrace()
    }
}