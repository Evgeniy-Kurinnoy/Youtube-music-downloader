package com.example.youtubemusicdownloader.musicDownload

import android.animation.ObjectAnimator
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_main.*
import android.content.Intent

import android.widget.Toast
import android.transition.TransitionManager
import android.view.View
import android.view.View.*
import androidx.constraintlayout.widget.ConstraintSet
import com.jakewharton.rxbinding3.view.clicks
import com.jakewharton.rxbinding3.widget.textChanges
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import com.example.youtubemusicdownloader.BaseActivity
import com.example.youtubemusicdownloader.R
import com.example.youtubemusicdownloader.SettingsActivity


class MusicDownloadActivity : BaseActivity() {
    enum class State {
        default, loading, completed
    }

    private var disposables = CompositeDisposable()
    private lateinit var viewModel: MusicDownloadViewModel


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        viewModel = MusicDownloadViewModel(this)

        bind()

        settingsButton.setOnClickListener(::openSettings)

        val url = intent.extras?.getString(Intent.EXTRA_TEXT)
        if (url != null){
            urlField.setText(url)
            downloadButton.callOnClick()
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)

        when (val url = intent?.extras?.getString(Intent.EXTRA_TEXT)) {
            null -> return
            else -> {
                urlField.setText(url)
                when(viewModel.currentState){
                    State.loading -> Toast.makeText(this, getString(R.string.wait_for_download), Toast.LENGTH_LONG).show()
                    State.completed -> {
                        downloadButton.callOnClick()
                        downloadButton.callOnClick()
                    }
                    State.default -> downloadButton.callOnClick()

                }
            }
        }
    }

    private fun bind(){
        val input = MusicDownloadViewModel.Input(
                urlField.textChanges().map { it.toString() },
                downloadButton.clicks()
        )
        val output = viewModel.transform(input)

        output.progress
            .map { "$it%" }
            .subscribe(progressView::setText)
            .addTo(disposables)

        output.error
            .subscribe {
                Toast.makeText(this, it.toString(), Toast.LENGTH_LONG).show()
            }.addTo(disposables)

        output.startLoading
            .map { it.title }
            .subscribe {
                musicNameField.text = it
            }
            .addTo(disposables)

        output.state.subscribe(::performState).addTo(disposables)
    }

    private fun performState(state: State){
        when (state){
            State.default -> toDefaultState()
            State.loading -> toLoadingState()
            State.completed -> toCompeteState()
        }
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
    }

}
