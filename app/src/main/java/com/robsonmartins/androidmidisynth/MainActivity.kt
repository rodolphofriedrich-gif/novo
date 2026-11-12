package com.robsonmartins.androidmidisynth

import android.os.Bundle
import android.view.WindowManager
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.ViewModel

class MainViewModel : ViewModel() {
    var textContent: String = ""
}

class MainActivity : AppCompatActivity() {

    companion object {
        init { System.loadLibrary("synth-lib") }
    }

    private val viewModel: MainViewModel by viewModels()
    private lateinit var synthManager: SynthManager
    private lateinit var midiManager: MidiManager
    private lateinit var txtLog: TextView
    private lateinit var scrollView: ScrollView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        scrollView = findViewById(R.id.scrollView)
        txtLog = findViewById(R.id.txtLog)
        txtLog.text = viewModel.textContent
        txtLog.addTextChangedListener {
            viewModel.textContent = it.toString()
            scrollView.post { scrollView.fullScroll(ScrollView.FOCUS_DOWN) }
        }

        // Síntese
        synthManager = SynthManager(this)
        synthManager.loadSF("KawaiStereoGrand.sf3")
        synthManager.setVolume(127)

        // ⬇️ NOVO: MidiManager com drivers kshoji
        midiManager = MidiManager(
            context = this,
            onLog = ::onMidiMessageReceived,
            onMidiEvent = { status, data1, data2 ->
                synthManager.sendMidiMessage(status, data1, data2)
            }
        )
        midiManager.start()
    }

    override fun onDestroy() {
        midiManager.stop() // ⬅️ substitui o finalize() antigo
        synthManager.finalize()
        super.onDestroy()
    }

    private fun onMidiMessageReceived(message: String) {
        runOnUiThread { txtLog.append(message + "\n") }
    }
}
