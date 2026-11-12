package com.robsonmartins.androidmidisynth

import android.content.Context
import jp.kshoji.blemidi.central.BleMidiCentralProvider
import jp.kshoji.blemidi.device.MidiInputDevice as BleMidiDevice
import jp.kshoji.blemidi.listener.OnMidiInputEventListener as BleListener
import jp.kshoji.driver.midi.device.MidiInputDevice as UsbMidiDevice
import jp.kshoji.driver.midi.listener.OnMidiInputEventListener as UsbListener
import jp.kshoji.driver.midi.util.UsbMidiDriver

/**
 * Recebe MIDI de dispositivos USB e Bluetooth LE via bibliotecas kshoji
 * e repassa os eventos para o SynthManager.
 */
class MidiManager(
    private val context: Context,
    private val onLog: (String) -> Unit,
    private val onMidiEvent: (status: Int, data1: Int, data2: Int) -> Unit
) {
    private var usbMidiDriver: UsbMidiDriver? = null
    private var bleMidiProvider: BleMidiCentralProvider? = null

    // ---- Listener USB ----
    private val usbListener = object : UsbListener {
        override fun onMidiNoteOn(sender: UsbMidiDevice?, channel: Int, note: Int, velocity: Int) {
            onMidiEvent(0x90 or ((channel - 1) and 0x0F), note, velocity)
        }
        override fun onMidiNoteOff(sender: UsbMidiDevice?, channel: Int, note: Int, velocity: Int) {
            onMidiEvent(0x80 or ((channel - 1) and 0x0F), note, velocity)
        }
        override fun onMidiControlChange(sender: UsbMidiDevice?, channel: Int, controller: Int, value: Int) {
            onMidiEvent(0xB0 or ((channel - 1) and 0x0F), controller, value)
        }
        // Outros callbacks (Pitch Bend, Program Change) podem ser adicionados aqui.
    }

    // ---- Listener BLE ----
    private val bleListener = object : BleListener {
        override fun onMidiNoteOn(sender: BleMidiDevice?, channel: Int, note: Int, velocity: Int) {
            onMidiEvent(0x90 or ((channel - 1) and 0x0F), note, velocity)
        }
        override fun onMidiNoteOff(sender: BleMidiDevice?, channel: Int, note: Int, velocity: Int) {
            onMidiEvent(0x80 or ((channel - 1) and 0x0F), note, velocity)
        }
        override fun onMidiControlChange(sender: BleMidiDevice?, channel: Int, controller: Int, value: Int) {
            onMidiEvent(0xB0 or ((channel - 1) and 0x0F), controller, value)
        }
    }

    fun start() {
        // ---- USB ----
        try {
            usbMidiDriver = UsbMidiDriver(context).apply {
                setOnMidiInputEventListener(usbListener)
                open() // método confirmado no sample oficial[reference:2]
            }
            onLog("USB MIDI driver iniciado")
        } catch (e: Exception) {
            onLog("Erro USB: ${e.message}")
        }

        // ---- BLE ----
        try {
            bleMidiProvider = BleMidiCentralProvider(context, bleListener).apply {
                startScanDevice(0) // timeout 0 = escaneia continuamente[reference:3]
            }
            onLog("BLE MIDI iniciado (scan)")
        } catch (e: Exception) {
            onLog("Erro BLE: ${e.message}")
        }
    }

    fun stop() {
        usbMidiDriver?.close() // método confirmado no sample oficial[reference:4]
        bleMidiProvider?.stopScanDevice()
    }
}
