package com.robsonmartins.androidmidisynth

import java.io.File
import java.io.IOException

data class SoundFontPreset(
    val bank: Int,
    val program: Int,
    val name: String
) {
    override fun toString(): String = "$name  •  Banco $bank / Programa $program"
}

/**
 * Interface Kotlin para o sintetizador nativo FluidSynth.
 */
class SynthManager {

    private var released = false

    init {
        fluidsynthInit()
    }

    fun release() {
        if (!released) {
            fluidsynthAllSoundsOff()
            fluidsynthFree()
            released = true
        }
    }

    /**
     * Carrega um SoundFont já copiado para o armazenamento privado do aplicativo.
     * Retorna os bancos e instrumentos realmente encontrados no arquivo.
     */
    fun loadSoundFont(file: File): List<SoundFontPreset> {
        check(!released) { "O sintetizador já foi finalizado." }
        if (!file.exists() || !file.isFile) {
            throw IOException("O arquivo SoundFont não foi encontrado.")
        }
        if (fluidsynthLoadSF(file.absolutePath, 0) < 0) {
            throw IOException("Não foi possível carregar este SoundFont.")
        }

        return fluidsynthGetPresetList()
            .lineSequence()
            .mapNotNull { line ->
                val parts = line.split('|', limit = 3)
                if (parts.size != 3) return@mapNotNull null
                val bank = parts[0].toIntOrNull() ?: return@mapNotNull null
                val program = parts[1].toIntOrNull() ?: return@mapNotNull null
                SoundFontPreset(bank, program, parts[2])
            }
            .sortedWith(compareBy<SoundFontPreset> { it.bank }.thenBy { it.program })
            .toList()
    }

    fun selectPreset(preset: SoundFontPreset) {
        if (!released) fluidsynthSelectPreset(preset.bank, preset.program)
    }

    fun noteOn(channel: Int, note: Int, velocity: Int) {
        if (!released) fluidsynthNoteOn(channel, note, velocity)
    }

    fun noteOff(channel: Int, note: Int) {
        if (!released) fluidsynthNoteOff(channel, note)
    }

    fun allSoundsOff() {
        if (!released) fluidsynthAllSoundsOff()
    }

    fun setVolume(volume: Int) {
        if (released) return
        val safeVolume = volume.coerceIn(0, 127)
        for (channel in 0 until 16) {
            fluidsynthCC(channel, 7, safeVolume)
        }
    }

    fun setReverb(level: Int) {
        if (!released) fluidsynthReverb(level.coerceIn(0, 127))
    }

    /** Mantém suporte para eventos MIDI externos em uma etapa futura. */
    fun sendMidiMessage(status: Int, data1: Int, data2: Int) {
        if (released) return
        when (status and 0xF0) {
            0x90 -> if (data2 > 0) {
                fluidsynthNoteOn(status and 0x0F, data1, data2)
            } else {
                fluidsynthNoteOff(status and 0x0F, data1)
            }
            0x80 -> fluidsynthNoteOff(status and 0x0F, data1)
            0xB0 -> fluidsynthCC(status and 0x0F, data1, data2)
            0xC0 -> fluidsynthProgramChange(status and 0x0F, data1)
            0xE0 -> fluidsynthPitchBend(status and 0x0F, (data2 shl 7) or data1)
        }
    }

    private external fun fluidsynthInit()
    private external fun fluidsynthLoadSF(soundfontPath: String, program: Int): Int
    private external fun fluidsynthFree()
    private external fun fluidsynthNoteOn(channel: Int, note: Int, velocity: Int)
    private external fun fluidsynthNoteOff(channel: Int, note: Int)
    private external fun fluidsynthCC(channel: Int, controller: Int, value: Int)
    private external fun fluidsynthProgramChange(channel: Int, program: Int)
    private external fun fluidsynthPitchBend(channel: Int, value: Int)
    private external fun fluidsynthReverb(level: Int)
    private external fun fluidsynthSelectPreset(bank: Int, program: Int)
    private external fun fluidsynthAllSoundsOff()
    private external fun fluidsynthGetPresetList(): String
}
