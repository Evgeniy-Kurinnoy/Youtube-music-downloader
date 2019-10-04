package com.example.youtubemusicdownloader.musicDownload

import android.Manifest
import android.animation.ObjectAnimator
import android.app.AlertDialog
import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_main.*
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder

import android.widget.Toast
import android.transition.TransitionManager
import android.view.View
import android.view.View.*
import androidx.constraintlayout.widget.ConstraintSet

import androidx.lifecycle.ViewModelProviders
import com.jakewharton.rxbinding3.view.clicks
import com.jakewharton.rxbinding3.widget.textChanges
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import com.example.youtubemusicdownloader.BaseActivity
import com.example.youtubemusicdownloader.R
import com.example.youtubemusicdownloader.SettingsActivity
import com.example.youtubemusicdownloader.downloadService.DownloadBinder
import com.example.youtubemusicdownloader.downloadService.DownloadService
import com.example.youtubemusicdownloader.utils.log
import retrofit2.HttpException


class MusicDownloadActivity : BaseActivity() {
    enum class State {
        default, preload, loading, completed
    }

    private var disposables = CompositeDisposable()
    private lateinit var viewModel: MusicDownloadViewModel

    private var serviceConnected: Boolean = false
        set(value){
            field = value
            log("service connected: $value")
            val url = intent.extras?.getString(Intent.EXTRA_TEXT)
            if (url != null){
                urlField.setText(url)
                downloadButton.callOnClick()
            }
        }

    private val mConnection = object: ServiceConnection {
        override fun onServiceDisconnected(name: ComponentName?) {
            serviceConnected = false
        }

        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            bind(service as DownloadBinder)
            serviceConnected = true
        }
    }



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        viewModel = ViewModelProviders.of(this).get(MusicDownloadViewModel::class.java)

        connectToService()

        settingsButton.setOnClickListener(::openSettings)
    }


    private fun connectToService(){
        val intent = Intent(this, DownloadService::class.java)
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE)
    }


    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        log("new intent")
        when (val url = intent?.extras?.getString(Intent.EXTRA_TEXT)) {
            null -> return
            else -> {
                urlField.setText(url)
                when(viewModel.currentState){
                    State.loading, State.preload -> Toast.makeText(this, getString(R.string.wait_for_download), Toast.LENGTH_LONG).show()

                    State.completed -> {
                        downloadButton.callOnClick()
                        downloadButton.callOnClick()
                    }
                    State.default -> downloadButton.callOnClick()
                }
            }
        }
    }

    private fun bind(binder: DownloadBinder){
        val input = MusicDownloadViewModel.Input(
                urlField.textChanges().map { it.toString() },
                downloadButton.clicks()
                    .doOnNext { log("button on next") }
                    .flatMap {
                        checkPermissions(listOf(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE))
                            .toObservable()
                            .doOnNext {
                                if (!it) {
                                    performError(SecurityException())
                                }
                            }
                            .filter { it }
                            .map { }
                    }
        )
        val output = viewModel.transform(input, binder)

        output.progress
            .map { "$it%" }
            .subscribe(progressView::setText)
            .addTo(disposables)

        output.error
            .subscribe {
                performError(it)
            }.addTo(disposables)

        output.startLoading
            .map { it.title }
            .subscribe(musicNameField::setText)
            .addTo(disposables)

        output.state.subscribe(::performState).addTo(disposables)
    }

    private fun setButtonEnabled(enabled: Boolean){
        downloadButton.isEnabled = enabled
        downloadButton.alpha = if (enabled) 1.0f else 0.5f
    }

    private fun performState(state: State){
        setButtonEnabled(true)
        when (state){
            State.default -> toDefaultState()
            State.loading -> toLoadingState()
            State.completed -> toCompeteState()
            State.preload -> setButtonEnabled(false)

        }
    }

    private fun performError(t: Throwable){
        val builder = AlertDialog.Builder(this, R.style.MyAlertDialogStyle)

        when {
            t is SecurityException -> builder
                .setTitle(getString(R.string.permission_denied_title))
                .setMessage(getString(R.string.permission_denied_message))

            (t as? HttpException)?.code() == 403 -> builder
                .setTitle(getString(R.string.forbidden_title))
                .setMessage(getString(R.string.forbidden_message))

            else ->  builder
                .setTitle(getString(R.string.error_title))
                .setMessage(t.message)
        }
            .setCancelable(true)
            .setNegativeButton(getString(R.string.ok)) { dialog, id ->
                dialog.cancel()
            }
            .create()
            .show()
    }


    private fun toLoadingState(){
        val constraintSet = ConstraintSet()
        constraintSet.clone(rootView)
        constraintSet.setVisibility(loadingGroup.id, VISIBLE)
        constraintSet.setVisibility(progressBar1.id, VISIBLE)
        constraintSet.setVisibility(completedView.id, GONE)

        TransitionManager.beginDelayedTransition(rootView)
        constraintSet.applyTo(rootView)

        downloadButton.text = getString(R.string.cancel)

        val anim = ObjectAnimator.ofFloat(urlField, SCALE_X, 1f, 0f)
        anim.duration = 200
        anim.start()

        urlField.isEnabled = false
        musicNameField.isSelected = true


    }

    private fun toCompeteState(){
        val constraintSet = ConstraintSet()
        constraintSet.clone(rootView)
        constraintSet.setVisibility(progressBar1.id, GONE)
        constraintSet.setVisibility(completedView.id, VISIBLE)
        TransitionManager.beginDelayedTransition(rootView)
        constraintSet.applyTo(rootView)

        downloadButton.text = getString(R.string.back)
        urlField.isEnabled = false

    }

    private fun toDefaultState(){
        urlField.text.clear()
        val anim = ObjectAnimator.ofFloat(urlField, SCALE_X, 0f, 1f)
        anim.startDelay = 300
        anim.start()

        val constraintSet = ConstraintSet()
        constraintSet.clone(rootView)
        constraintSet.setVisibility(loadingGroup.id, GONE)
        constraintSet.setVisibility(progressBar1.id, GONE)
        constraintSet.setVisibility(completedView.id, GONE)

        TransitionManager.beginDelayedTransition(rootView)
        constraintSet.applyTo(rootView)

        downloadButton.text = getString(R.string.download)


        urlField.isEnabled = true
    }

    private fun openSettings(view: View){
        val intent = Intent(this, SettingsActivity::class.java)
        startActivity(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        disposables.dispose()
        if (serviceConnected) {
            unbindService(mConnection)
            serviceConnected = false
        }
    }

}

