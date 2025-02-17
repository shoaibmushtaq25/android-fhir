/*
 * Copyright 2021 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.fhir.datacapture.contrib.views.barcode.mlkit.md.settings

import android.content.Context
import android.graphics.RectF
import android.preference.PreferenceManager
import androidx.annotation.StringRes
import com.google.android.fhir.datacapture.R
import com.google.android.fhir.datacapture.contrib.views.barcode.mlkit.md.camera.CameraSizePair
import com.google.android.fhir.datacapture.contrib.views.barcode.mlkit.md.camera.GraphicOverlay
import com.google.android.gms.common.images.Size
import com.google.mlkit.vision.barcode.Barcode

/** Utility class to retrieve shared preferences. */
object PreferenceUtils {

  fun saveStringPreference(context: Context, @StringRes prefKeyId: Int, value: String?) {
    PreferenceManager.getDefaultSharedPreferences(context)
      .edit()
      .putString(context.getString(prefKeyId), value)
      .apply()
  }

  fun getProgressToMeetBarcodeSizeRequirement(overlay: GraphicOverlay, barcode: Barcode): Float {
    val context = overlay.context
    return if (getBooleanPref(context, R.string.pref_key_enable_barcode_size_check, false)) {
      val reticleBoxWidth = getBarcodeReticleBox(overlay).width()
      val barcodeWidth = overlay.translateX(barcode.boundingBox?.width()?.toFloat() ?: 0f)
      val requiredWidth =
        reticleBoxWidth * getIntPref(context, R.string.pref_key_minimum_barcode_width, 50) / 100
      (barcodeWidth / requiredWidth).coerceAtMost(1f)
    } else {
      1f
    }
  }

  fun getBarcodeReticleBox(overlay: GraphicOverlay): RectF {
    val context = overlay.context
    val overlayWidth = overlay.width.toFloat()
    val overlayHeight = overlay.height.toFloat()
    val boxWidth =
      overlayWidth * getIntPref(context, R.string.pref_key_barcode_reticle_width, 80) / 100
    val boxHeight =
      overlayHeight * getIntPref(context, R.string.pref_key_barcode_reticle_height, 35) / 100
    val cx = overlayWidth / 2
    val cy = overlayHeight / 2
    return RectF(cx - boxWidth / 2, cy - boxHeight / 2, cx + boxWidth / 2, cy + boxHeight / 2)
  }

  fun shouldDelayLoadingBarcodeResult(context: Context): Boolean =
    getBooleanPref(context, R.string.pref_key_delay_loading_barcode_result, true)

  private fun getIntPref(context: Context, @StringRes prefKeyId: Int, defaultValue: Int): Int {
    val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
    val prefKey = context.getString(prefKeyId)
    return sharedPreferences.getInt(prefKey, defaultValue)
  }

  fun getUserSpecifiedPreviewSize(context: Context): CameraSizePair? {
    return try {
      val previewSizePrefKey = context.getString(R.string.pref_key_rear_camera_preview_size)
      val pictureSizePrefKey = context.getString(R.string.pref_key_rear_camera_picture_size)
      val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
      CameraSizePair(
        Size.parseSize(sharedPreferences.getString(previewSizePrefKey, null)),
        Size.parseSize(sharedPreferences.getString(pictureSizePrefKey, null))
      )
    } catch (e: Exception) {
      null
    }
  }

  private fun getBooleanPref(
    context: Context,
    @StringRes prefKeyId: Int,
    defaultValue: Boolean
  ): Boolean =
    PreferenceManager.getDefaultSharedPreferences(context)
      .getBoolean(context.getString(prefKeyId), defaultValue)
}
