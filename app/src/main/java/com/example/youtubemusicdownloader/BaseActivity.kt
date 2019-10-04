package com.example.youtubemusicdownloader

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import io.reactivex.Single
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.SingleSubject
import android.view.WindowManager
import android.os.Build



open class BaseActivity: AppCompatActivity() {
    private var requestedPermission: String? = null
    private var permissionSubject: SingleSubject<Boolean>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            val w = window
            w.setFlags(
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
            )
        }
    }

    fun checkPermissions(permissions: List<String>): Single<Boolean> {
       // requestedPermission = permission
        permissionSubject = SingleSubject.create()
        val notGrantedPermissions = permissions.filter {
            checkCallingOrSelfPermission(it) != PackageManager.PERMISSION_GRANTED
        }

        if (notGrantedPermissions.isEmpty()) {
            return Single.just(true)
        } else {
            ActivityCompat.requestPermissions(this, notGrantedPermissions.toTypedArray(), 1)
        }
        return permissionSubject!!
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
       // val permission = requestedPermission ?: return

        if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            permissionSubject?.onSuccess(true)
        } else {
            permissionSubject?.onSuccess(false)
        }

    }


}