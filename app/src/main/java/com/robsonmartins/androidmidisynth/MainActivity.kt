package com.robsonmartins.androidmidisynth

import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ArrayAdapter
import android.widget.SeekBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import java.io.File
import java.io.IOException

class MainActivity : AppCompatActivity() {

    companion object {
        init {
            System.loadLibrary("synth-lib")
        }
    }

    private val soundFontPicker =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) importSoundFont(uri)
        }

    private lateinit var synthManager: SynthManager
    private lateinit var txtSoundFontName: TextView
    private lateinit var txtSoundFontDetails: TextView
    private lateinit var txtStatus: TextView
    private lateinit var txtOctave: TextView
    private lateinit var txtVolumeValue: TextView
    private lateinit var txtReverbValue: TextView
    private lateinit var spinnerPresets: Spinner
    private lateinit var pianoKeyboard: PianoKeyboardView
    private lateinit var btnLoadSoundFont: MaterialButton
    private lateinit var btnStop: MaterialButton
    private lateinit var btnOctaveDown: MaterialButton
    private lateinit var btnOctaveUp: MaterialButton
    private lateinit var seekVolume: SeekBar
    private lateinit var seekReverb: SeekBar

    private var presets: List<SoundFontPreset> = emptyList()
    private var soundFontLoaded = false
    private var synthReady = false
    private var octaveShift = 0
    private var loadedSoundFontFile: File? = null
    private val synthLock = Any()
    @Volatile private var activityDestroyed = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContentView(R.layout.activity_main)

        bindViews()
        configureControls()

        txtStatus.text = "Abra um arquivo SF2 ou SF3 para começar."
    }

    private fun bindViews() {
        txtSoundFontName = findViewById(R.id.txtSoundFontName)
        txtSoundFontDetails = findViewById(R.id.txtSoundFontDetails)
        txtStatus = findViewById(R.id.txtStatus)
        txtOctave = findViewById(R.id.txtOctave)
        txtVolumeValue = findViewById(R.id.txtVolumeValue)
        txtReverbValue = findViewById(R.id.txtReverbValue)
        spinnerPresets = findViewById(R.id.spinnerPresets)
        pianoKeyboard = findViewById(R.id.pianoKeyboard)
        btnLoadSoundFont = findViewById(R.id.btnLoadSoundFont)
        btnStop = findViewById(R.id.btnStop)
        btnOctaveDown = findViewById(R.id.btnOctaveDown)
        btnOctaveUp = findViewById(R.id.btnOctaveUp)
        seekVolume = findViewById(R.id.seekVolume)
        seekReverb = findViewById(R.id.seekReverb)
    }

    private fun configureControls() {
        btnLoadSoundFont.setOnClickListener {
            soundFontPicker.launch(arrayOf("*/*"))
        }

        btnStop.setOnClickListener {
            if (soundFontLoaded) {
                pianoKeyboard.clearPressedKeys()
                synchronized(synthLock) { synthManager.allSoundsOff() }
                txtStatus.text = "Todas as notas foram interrompidas."
            }
        }

        btnOctaveDown.setOnClickListener { changeOctave(-1) }
        btnOctaveUp.setOnClickListener { changeOctave(1) }

        pianoKeyboard.onNoteOn = { note, velocity ->
            if (soundFontLoaded) synchronized(synthLock) { synthManager.noteOn(0, note, velocity) }
        }
        pianoKeyboard.onNoteOff = { note ->
            if (soundFontLoaded) synchronized(synthLock) { synthManager.noteOff(0, note) }
        }

        spinnerPresets.isEnabled = false
        spinnerPresets.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: android.widget.AdapterView<*>?,
                view: android.view.View?,
                position: Int,
                id: Long
            ) {
                if (soundFontLoaded && position in presets.indices) {
                    synchronized(synthLock) { synthManager.selectPreset(presets[position]) }
                    txtStatus.text = "Instrumento: ${presets[position].name}"
                }
            }

            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) = Unit
        }

        seekVolume.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                txtVolumeValue.text = progress.toString()
                if (synthReady) synchronized(synthLock) { synthManager.setVolume(progress) }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit
            override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
        })

        seekReverb.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                txtReverbValue.text = progress.toString()
                if (synthReady) synchronized(synthLock) { synthManager.setReverb(progress) }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit
            override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
        })

        txtVolumeValue.text = seekVolume.progress.toString()
        txtReverbValue.text = seekReverb.progress.toString()
        setPlayerControlsEnabled(false)
    }

    private fun changeOctave(delta: Int) {
        octaveShift = (octaveShift + delta).coerceIn(-2, 2)
        val baseNote = 48 + octaveShift * 12
        pianoKeyboard.setBaseNote(baseNote)
        val octave = (baseNote / 12) - 1
        txtOctave.text = "C${octave} – B${octave + 1}"
    }

    private fun importSoundFont(uri: Uri) {
        btnLoadSoundFont.isEnabled = false
        setPlayerControlsEnabled(false)
        pianoKeyboard.clearPressedKeys()
        txtStatus.text = "Copiando e carregando SoundFont…"
        val previousFile = loadedSoundFontFile

        Thread {
            var importedFile: File? = null
            try {
                val displayName = getDisplayName(uri)
                val file = copySoundFontToCache(uri, displayName)
                importedFile = file
                val loadedPresets = synchronized(synthLock) {
                    if (activityDestroyed) throw IOException("A tela do aplicativo foi encerrada.")
                    if (!synthReady) {
                        synthManager = SynthManager()
                        synthReady = true
                    }
                    synthManager.setVolume(seekVolume.progress)
                    synthManager.setReverb(seekReverb.progress)
                    synthManager.allSoundsOff()
                    synthManager.loadSoundFont(file)
                }
                if (loadedPresets.isEmpty()) {
                    throw IOException("O arquivo não contém instrumentos reconhecidos.")
                }
                synchronized(synthLock) { synthManager.selectPreset(loadedPresets.first()) }
                if (activityDestroyed) {
                    file.delete()
                    return@Thread
                }

                runOnUiThread {
                    if (activityDestroyed) {
                        file.delete()
                        return@runOnUiThread
                    }
                    loadedSoundFontFile = file
                    previousFile?.takeIf { it != file }?.delete()
                    presets = loadedPresets
                    soundFontLoaded = true
                    txtSoundFontName.text = displayName
                    txtSoundFontDetails.text =
                        "${formatFileSize(file.length())}  •  ${presets.size} instrumentos encontrados"

                    val adapter = object : ArrayAdapter<String>(
                        this,
                        android.R.layout.simple_spinner_item,
                        presets.map { it.toString() }
                    ) {
                        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                            return super.getView(position, convertView, parent).also {
                                (it as? TextView)?.setTextColor(Color.WHITE)
                            }
                        }

                        override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                            return super.getDropDownView(position, convertView, parent).also {
                                it.setBackgroundColor(Color.rgb(17, 26, 43))
                                (it as? TextView)?.setTextColor(Color.WHITE)
                            }
                        }
                    }
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    spinnerPresets.adapter = adapter
                    spinnerPresets.isEnabled = true
                    pianoKeyboard.isEnabled = true
                    btnStop.isEnabled = true
                    btnOctaveDown.isEnabled = true
                    btnOctaveUp.isEnabled = true
                    spinnerPresets.setSelection(0)
                    setPlayerControlsEnabled(true)
                    btnLoadSoundFont.isEnabled = true
                    txtStatus.text = "SoundFont carregado. Toque nas teclas."
                }
            } catch (error: Exception) {
                importedFile?.delete()
                if (!soundFontLoaded && synthReady) {
                    synchronized(synthLock) {
                        synthManager.release()
                        synthReady = false
                    }
                }
                if (activityDestroyed) return@Thread
                runOnUiThread {
                    if (activityDestroyed) return@runOnUiThread
                    btnLoadSoundFont.isEnabled = true
                    setPlayerControlsEnabled(soundFontLoaded)
                    txtStatus.text = error.message ?: "Não foi possível abrir o SoundFont."
                    Toast.makeText(
                        this,
                        error.message ?: "Falha ao carregar SoundFont",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }.start()
    }

    private fun getDisplayName(uri: Uri): String {
        contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val column = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (column >= 0) return cursor.getString(column)?.takeIf { it.isNotBlank() } ?: "SoundFont.sf2"
            }
        }
        return "SoundFont.sf2"
    }

    private fun copySoundFontToCache(uri: Uri, displayName: String): File {
        val extension = displayName.substringAfterLast('.', "").lowercase()
        if (extension != "sf2" && extension != "sf3") {
            throw IOException("Formato não suportado. Selecione um arquivo .sf2 ou .sf3.")
        }

        val safeName = displayName.replace(Regex("[^A-Za-z0-9._-]"), "_")
        val directory = File(cacheDir, "soundfonts")
        if (!directory.exists() && !directory.mkdirs()) {
            throw IOException("Não foi possível criar a pasta interna dos SoundFonts.")
        }
        val destination = File(directory, "${System.currentTimeMillis()}_$safeName")

        try {
            val input = contentResolver.openInputStream(uri)
                ?: throw IOException("Não foi possível ler o arquivo selecionado.")
            input.use { source ->
                destination.outputStream().buffered().use { output ->
                    source.copyTo(output, bufferSize = 64 * 1024)
                }
            }
            return destination
        } catch (error: Exception) {
            destination.delete()
            throw error
        }
    }

    private fun formatFileSize(bytes: Long): String {
        val megabytes = bytes / (1024.0 * 1024.0)
        return if (megabytes >= 1.0) {
            String.format(java.util.Locale.getDefault(), "%.1f MB", megabytes)
        } else {
            "${(bytes / 1024).coerceAtLeast(1)} KB"
        }
    }

    private fun setPlayerControlsEnabled(enabled: Boolean) {
        val available = enabled && soundFontLoaded
        spinnerPresets.isEnabled = available
        pianoKeyboard.isEnabled = available
        btnStop.isEnabled = available
        btnOctaveDown.isEnabled = available
        btnOctaveUp.isEnabled = available
        seekVolume.isEnabled = enabled
        seekReverb.isEnabled = enabled
    }

    override fun onDestroy() {
        activityDestroyed = true
        if (synthReady) {
            synchronized(synthLock) {
                pianoKeyboard.clearPressedKeys()
                synthManager.release()
                synthReady = false
            }
        }
        loadedSoundFontFile?.delete()
        super.onDestroy()
    }
}
