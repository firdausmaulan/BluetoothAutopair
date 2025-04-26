package com.fd.bta

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat

class PermissionHelper {

    fun isPermissionBluetoothGranted(activity: Activity): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val isBluetoothScanGranted = isGranted(activity, Manifest.permission.BLUETOOTH_SCAN)
            val isBluetoothConnectGranted = isGranted(activity, Manifest.permission.BLUETOOTH_CONNECT)
            val isFineLocationGranted = isGranted(activity, Manifest.permission.ACCESS_FINE_LOCATION)
            val granted = isBluetoothScanGranted && isBluetoothConnectGranted && isFineLocationGranted
            if (!granted) {
                ActivityCompat.requestPermissions(
                    activity,
                    arrayOf(
                        Manifest.permission.BLUETOOTH_SCAN,
                        Manifest.permission.BLUETOOTH_CONNECT,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ),
                    1
                )
            }
            granted
        } else {
            val isBluetoothGranted = isGranted(activity, Manifest.permission.BLUETOOTH)
            val isFineLocationGranted = isGranted(activity, Manifest.permission.ACCESS_FINE_LOCATION)
            val granted = isBluetoothGranted && isFineLocationGranted
            if (!granted) {
                ActivityCompat.requestPermissions(
                    activity,
                    arrayOf(
                        Manifest.permission.BLUETOOTH,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ),
                    1
                )
            }
            granted
        }
    }

    private fun isGranted(activity: Activity, permission: String): Boolean {
        return ActivityCompat.checkSelfPermission(
            activity,
            permission
        ) == PackageManager.PERMISSION_GRANTED
    }

}