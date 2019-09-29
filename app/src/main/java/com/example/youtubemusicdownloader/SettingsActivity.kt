package com.example.youtubemusicdownloader

import android.app.Activity
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.settings_activity.*
import android.content.Intent
import android.os.Environment
import android.view.MenuItem
import com.example.youtubemusicdownloader.data.Defaults
import com.nononsenseapps.filepicker.FilePickerActivity
import com.nononsenseapps.filepicker.Utils


class SettingsActivity: AppCompatActivity() {
    private val READ_REQUEST_CODE = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_activity)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.settings)

        chooseFolderItem.setOnClickListener(::chooseFolder)
        pathView.text = Defaults.savePath
    }

    private fun chooseFolder(view: View){
        val intent = Intent(this, FilePickerActivity::class.java)
        intent.putExtra(FilePickerActivity.EXTRA_ALLOW_MULTIPLE, false)
        intent.putExtra(FilePickerActivity.EXTRA_ALLOW_CREATE_DIR, false)
        intent.putExtra(FilePickerActivity.EXTRA_MODE, FilePickerActivity.MODE_DIR)
        intent.putExtra(FilePickerActivity.EXTRA_START_PATH, Defaults.savePath)

        startActivityForResult(intent, READ_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == READ_REQUEST_CODE && resultCode == Activity.RESULT_OK && data != null){

            val urls = Utils.getSelectedFilesFromResult(data)
            if (urls.isNotEmpty()){
                val segments = urls.first().pathSegments
                val path = segments.subList(1, segments.size).joinToString("/")
                Defaults.savePath = path
                pathView.text = path
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when(item?.itemId){
            android.R.id.home -> onBackPressed()
        }

        return super.onOptionsItemSelected(item)
    }
}