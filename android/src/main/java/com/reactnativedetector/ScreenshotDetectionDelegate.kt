package com.reactnativedetector

import android.Manifest
import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.Build
import android.provider.MediaStore
import android.content.pm.PackageManager
import android.annotation.SuppressLint
import android.app.Activity
import androidx.core.content.ContextCompat
import com.facebook.react.bridge.ReactContext
import java.lang.Exception


class ScreenshotDetectionDelegate(val context: ReactContext, val listener: ScreenshotDetectionListener) {
  lateinit var contentObserver: ContentObserver

  var isListening = false
  var previousPath = ""

  private val screenCaptureCallback = Activity.ScreenCaptureCallback {
    onScreenCaptured("")
  }

  @SuppressLint("MissingPermission")
  fun startScreenshotDetection() {
    val contentObserver: ContentObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
      override fun onChange(selfChange: Boolean, uri: Uri?) {

        super.onChange(selfChange, uri)
        if (!isReadExternalStoragePermissionGranted()) {
          onScreenCapturedWithDeniedPermission()
        }

        if (uri != null) {
          val path = getFilePathFromContentResolver(context, uri)
          if (path != null && isScreenshotPath(path)) {
            previousPath = path.toLowerCase().substring(path.toLowerCase().lastIndexOf("screenshot"))
            onScreenCaptured(path!!)
          }
        }
      }
    }

    context.contentResolver.registerContentObserver(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
      true,
      contentObserver)

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
      context.runOnUiQueueThread {
        kotlin.run {
          context.currentActivity?.registerScreenCaptureCallback(context.mainExecutor, screenCaptureCallback)
        }
      }
    }

    isListening = true
  }

  @SuppressLint("MissingPermission")
  fun stopScreenshotDetection() {
    val contentObserver: ContentObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
      override fun onChange(selfChange: Boolean, uri: Uri?) {

        super.onChange(selfChange, uri)
        if (!isReadExternalStoragePermissionGranted()) {
          onScreenCapturedWithDeniedPermission()
        }

        if (uri != null) {
          val path = getFilePathFromContentResolver(context, uri)
          if (path != null && isScreenshotPath(path)) {
            previousPath = path.toLowerCase().substring(path.toLowerCase().lastIndexOf("screenshot"))
            onScreenCaptured(path!!)
          }
        }
      }
    }

    context.getContentResolver().unregisterContentObserver(contentObserver)

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
      context.runOnUiQueueThread {
        kotlin.run {
          context.currentActivity?.unregisterScreenCaptureCallback(screenCaptureCallback)
        }
      }
    }

    isListening = false
  }

  private fun onScreenCaptured(path: String) {
    listener.onScreenCaptured(path)
  }

  private fun onScreenCapturedWithDeniedPermission() {
    listener.onScreenCapturedWithDeniedPermission()
  }

  private fun isScreenshotPath(path: String?): Boolean {
    return path != null && path.toLowerCase().contains("screenshots") && (previousPath == "" || !path.toLowerCase().contains(previousPath))
  }

  @SuppressLint("Range")
  private fun getFilePathFromContentResolver(context: Context, uri: Uri): String? {
    try {

      val cursor = context.contentResolver.query(uri, arrayOf(MediaStore.Images.Media.DISPLAY_NAME, MediaStore.Images.Media.DATA), null, null, null)
      if (cursor != null && cursor.moveToFirst()) {
        val path = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA))
        cursor.close()
        return path
      }
    } catch (e: Exception) {

    }
    return null
  }

  private fun isReadExternalStoragePermissionGranted(): Boolean {
    val isAndroid13OrAbove = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

    if (isAndroid13OrAbove) {
      return ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED
    }
    return ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
  }
}

interface ScreenshotDetectionListener {
  fun onScreenCaptured(path: String)
  fun onScreenCapturedWithDeniedPermission()
}
