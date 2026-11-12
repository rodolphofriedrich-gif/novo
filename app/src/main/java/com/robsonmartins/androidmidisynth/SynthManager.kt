package com.robsonmartins.androidmidisynth

import android.content.Context
import androidx.appcompat.app.AppCompatActivity.MODE_PRIVATE
import java.io.IOException

class SynthManager(private val context: Context) {

    private var soundFontPath: String? = null
    private var released = false

    init { fluidsynthInit() }

    fun release() {
        if (!released) {
            fluidsynthFree()
            released = true
        }
    }

    fun loadSF(filename: String, program: Int = 0) {
        try {
            soundFontPath = copyAssetToTmpFile(filename)
            if (fluidsynthLoadSF(soundFontPath, program) < 0) {
                throw IOException("Error loading $filename")
            }
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
    }

    fun setVolume(volume: Int) {
        for (channel in 0 until 16) {
            fluidsynthCC(channel, 7, volume.coerceIn(0, 127))
        }
    }

    /**
     * Envia um evento MIDI bruto (status, data1, data2) para o FluidSynth.
     * O status byte deve vir com o canal na parte baixa (ex: 0x90 | channel).
     */
    fun sendMidiMessage(status: Int, data1: Int, data2: Int) {
        when (status and 0xF0) {
            0x90 -> if (data2 > 0) fluidsynthNoteOn(status and 0x0F, data1, data2) else fluidsynthNoteOff(status and 0x0F, data1)
            0x80 -> fluidsynthNoteOff(status and 0x0F, data1)
            0xB0 -> fluidsynthCC(status and 0x0F, data1, data2)
            0xC0 -> fluidsynthProgramChange(status and 0x0F, data1)
            0xE0 -> fluidsynthPitchBend(status and 0x0F, (data2 shl 7) or data1)
        }
    }

    @Throws(IOException::class)
    private fun copyAssetToTmpFile(filename: String): String {
        context.assets.open(filename).use { `is` ->
            val tempFilename = "tmp_$filename"
            context.openFileOutput(tempFilename, MODE_PRIVATE).use { fos ->
                var bytesRead: Int
                val buffer = ByteArray(4096)
                while ((`is`.read(buffer).also { bytesRead = it }) != -1) {
                    fos.write(buffer, 0, bytesRead)
                }
            }
            val filesDir = context.filesDir
            return "$filesDir/$tempFilename"
        }
    }

    // ⬇️ MÉTODOS AGORA PÚBLICOS (eram private)
    external fun fluidsynthInit()
    external fun fluidsynthLoadSF(soundfontPath: String?, program: Int): Int
    external fun fluidsynthFree()
    external fun fluidsynthNoteOn(channel: Int, note: Int, velocity: Int)
    external fun fluidsynthNoteOff(channel: Int, note: Int)
    external fun fluidsynthCC(channel: Int, controller: Int, value: Int)
    external fun fluidsynthProgramChange(channel: Int, program: Int)
    external fun fluidsynthPitchBend(channel: Int, value: Int)
    external fun fluidsynthReverb(level: Int)
}
