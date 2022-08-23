package com.vadim.a7minutesworkoutapp

import android.app.Dialog
import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.opengl.Visibility
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.CountDownTimer
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.vadim.a7minutesworkoutapp.databinding.ActivityExerciseBinding
import com.vadim.a7minutesworkoutapp.databinding.DialogCustomBackConfirmatonBinding
import java.lang.Exception
import java.util.*
import kotlin.collections.ArrayList

class ExerciseActivity : AppCompatActivity(), TextToSpeech.OnInitListener {
    private var binding: ActivityExerciseBinding? = null
    private var restTimer: CountDownTimer? = null
    private var restProgress = 0

    private var exerciseTimer: CountDownTimer? = null
    private var exerciseProgress = 0

    private val timeRestInMilliseconds: Long = 10000L
    private val intervalRestInMilliseconds: Long = 1000

    private val timeExerciseInMilliseconds: Long = 30000L
    private val intervalExerciseInMilliseconds: Long = 1000

    private var exerciseList: ArrayList<ExerciseModel>? = null
    private var currentExercisePosition = -1

    private var tts: TextToSpeech? = null
    private var player: MediaPlayer? = null

    private var exerciseAdapter: ExerciseStatusAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityExerciseBinding.inflate(layoutInflater)
        setContentView(binding?.root)

        setSupportActionBar(binding?.toolbarExercise)

        if (supportActionBar != null) {
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
        }

        exerciseList = Constants.defaultExerciseList()

        tts = TextToSpeech(this, this)

        binding?.toolbarExercise?.setNavigationOnClickListener {
            customDialogForBackButton()
        }

        setupRestView()
        setupExerciseStatusRecyclerView()


    }

    private fun customDialogForBackButton() {
        val customDialog = Dialog(this)
        val dialogBinding = DialogCustomBackConfirmatonBinding.inflate(layoutInflater)
        customDialog.setContentView(dialogBinding.root)
        customDialog.setCanceledOnTouchOutside(false)
        dialogBinding.btnYes.setOnClickListener{
            this@ExerciseActivity.finish()
            customDialog.dismiss()
        }
        dialogBinding.btnNo.setOnClickListener{
            customDialog.dismiss()
        }

        customDialog.show()
    }

    override fun onBackPressed() {
        customDialogForBackButton()
    }

    private fun setupExerciseStatusRecyclerView() {
        binding?.rvExerciseStatus?.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        exerciseAdapter = ExerciseStatusAdapter(exerciseList!!)
        binding?.rvExerciseStatus?.adapter = exerciseAdapter
    }

    private fun setupRestView() {
        try {
            val soundURI =
                Uri.parse("android.resource://com.vadim.a7minutesworkoutapp/${R.raw.press_start}")
            player = MediaPlayer.create(applicationContext, soundURI)
            player?.isLooping = false
            player?.start()
        } catch (e: Exception) {
            e.printStackTrace()

        }

        binding?.flRestView?.visibility = View.VISIBLE
        binding?.tvTitle?.visibility = View.VISIBLE
        binding?.tvExerciseName?.visibility = View.INVISIBLE
        binding?.flExerciseView?.visibility = View.INVISIBLE
        binding?.ivImage?.visibility = View.INVISIBLE
        binding?.tvUpcomingLabel?.visibility = View.VISIBLE
        binding?.tvUpComingExerciseName?.visibility = View.VISIBLE

        restProgress = stopTheTimer(restTimer, restProgress)

        binding?.tvUpComingExerciseName?.text =
            exerciseList!![currentExercisePosition + 1].getName()

        setRestProgressBar()
    }

    private fun setupExerciseView() {
        binding?.flRestView?.visibility = View.INVISIBLE
        binding?.tvTitle?.visibility = View.INVISIBLE
        binding?.tvExerciseName?.visibility = View.VISIBLE
        binding?.flExerciseView?.visibility = View.VISIBLE
        binding?.ivImage?.visibility = View.VISIBLE
        binding?.tvUpcomingLabel?.visibility = View.INVISIBLE
        binding?.tvUpComingExerciseName?.visibility = View.INVISIBLE
        exerciseProgress = stopTheTimer(exerciseTimer, exerciseProgress)

        speakOut(exerciseList!![currentExercisePosition].getName())

        binding?.ivImage?.setImageResource(exerciseList!![currentExercisePosition].getImage())
        binding?.tvExerciseName?.text = exerciseList!![currentExercisePosition].getName()
        setExerciseProgressBar()
    }

    private fun setRestProgressBar() {
        binding?.progressBar?.progress = restProgress
        restTimer = object : CountDownTimer(timeRestInMilliseconds, intervalRestInMilliseconds) {
            override fun onTick(p0: Long) {
                restProgress++
                binding?.progressBar?.progress =
                    (timeRestInMilliseconds / intervalRestInMilliseconds - restProgress).toInt()
                binding?.tvTimer?.text =
                    (timeRestInMilliseconds / intervalRestInMilliseconds - restProgress).toString()
            }

            override fun onFinish() {
                currentExercisePosition++

                exerciseList!![currentExercisePosition].setIsSelected(true)
                exerciseAdapter!!.notifyDataSetChanged()
                setupExerciseView()
            }
        }.start()
    }

    private fun setExerciseProgressBar() {
        binding?.progressBarExercise?.progress = exerciseProgress
        exerciseTimer =
            object : CountDownTimer(timeExerciseInMilliseconds, intervalExerciseInMilliseconds) {
                override fun onTick(p0: Long) {
                    exerciseProgress++
                    binding?.progressBarExercise?.progress =
                        (timeExerciseInMilliseconds / intervalExerciseInMilliseconds - exerciseProgress).toInt()
                    binding?.tvTimerExercise?.text =
                        (timeExerciseInMilliseconds / intervalExerciseInMilliseconds - exerciseProgress).toString()
                }

                override fun onFinish() {

                    if (currentExercisePosition < exerciseList?.size!! - 1) {
                        exerciseList!![currentExercisePosition].setIsSelected(false)
                        exerciseList!![currentExercisePosition].setIsCompleted(true)
                        exerciseAdapter!!.notifyDataSetChanged()

                        setupRestView()
                    } else {
                        finish()
                        val intent = Intent(this@ExerciseActivity, FinishActivity::class.java)
                        startActivity(intent)
                    }
                }
            }.start()
    }

    private fun stopTheTimer(timer: CountDownTimer?, progress: Int): Int {
        var timerProgress = progress
        if (timer != null) {
            timer?.cancel()
            timerProgress = 0
            return timerProgress
        }
        return progress
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts!!.setLanguage(Locale.US)

            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("TTS", "The Language specified is not supported!")
            } else {
                Log.e("TTS", "Initialization Failed!")
            }
        }
    }

    private fun speakOut(textToSpeak: String) {
        tts!!.speak(textToSpeak, TextToSpeech.QUEUE_FLUSH, null, "")
    }

    override fun onDestroy() {
        super.onDestroy()
        if (tts != null) {
            tts?.stop()
            tts?.shutdown()
        }
        restProgress = stopTheTimer(restTimer, restProgress)
        exerciseProgress = stopTheTimer(exerciseTimer, exerciseProgress)
        if (player != null) {
            player!!.stop()
        }
        binding = null
    }
}