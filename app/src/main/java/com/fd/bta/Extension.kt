package com.fd.bta

import android.content.Intent
import android.os.Build
import androidx.core.content.IntentCompat

inline fun <reified T> Intent.parcelable(name: String): T? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        IntentCompat.getParcelableExtra(this, name,  T::class.java)
    } else {
        getParcelableExtra(name)
    }
}