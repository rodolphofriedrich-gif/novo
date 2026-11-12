package com.robsonmartins.androidmidisynth

import android.content.Context
import jp.kshoji.blemidi.central.BleMidiCentralProvider
import jp.kshoji.blemidi.device.MidiInputDevice as BleMidiDevice
import jp.kshoji.blemidi.listener.OnMidiInputEventListener as BleListener
import jp.kshoji.blemidi.listener.OnMidiDeviceAttachedListener
import jp.kshoji.driver.midi.device.MidiInputDevice as UsbMidiDevice
import jp.kshoji.driver.midi.device.MidiOutputDevice as UsbMidiOutputDevice
import jp.kshoji.driver.midi.util.UsbMidiDriver

class MidiManager(
    private val context: Context,
    private val onLog: (String) -> Unit,
    private val onMidiEvent: (status: Int, data1: Int, data2: Int) -> Unit
) {
    private var usbMidiDriver: UsbMidiDriver? = null
    private var bleMidiProvider: BleMidiCentralProvider? = null

    // --- Listener BLE completo (métodos obrigatórios da interface) ---
    private val bleListener = object : BleListener {
        override fun onMidiNoteOn(sender: BleMidiDevice, channel: Int, note: Int, velocity: Int) {
            onMidiEvent(0x90 or (channel and 0x0F), note, velocity)
        }
        override fun onMidiNoteOff(sender: BleMidiDevice, channel: Int, note: Int, velocity: Int) {
            onMidiEvent(0x80 or (channel and 0x0F), note, velocity)
        }
        override fun onMidiControlChange(sender: BleMidiDevice, channel: Int, function: Int, value: Int) {
            onMidiEvent(0xB0 or (channel and 0x0F), function, value)
        }

        override fun onMidiMiscellaneousFunctionCodes(sender: BleMidiDevice, cable: Int, byte1: Int, byte2: Int, byte3: Int) {}
        override fun onMidiCableEvents(sender: BleMidiDevice, cable: Int, byte1: Int, byte2: Int, byte3: Int) {}
        override fun onMidiSystemCommonMessage(sender: BleMidiDevice, cable: Int, bytes: ByteArray?) {}
        override fun onMidiSystemExclusive(sender: BleMidiDevice, systemExclusive: ByteArray) {}
        override fun onMidiSingleByte(sender: BleMidiDevice, byte1: Int) {}
        override fun onMidiPolyphonicAftertouch(sender: BleMidiDevice, cable: Int, channel: Int, note: Int, pressure: Int) {}
        override fun onMidiProgramChange(sender: BleMidiDevice, cable: Int, channel: Int, program: Int) {}
        override fun onMidiChannelAftertouch(sender: BleMidiDevice, cable: Int, channel: Int, pressure: Int) {}
        override fun onMidiPitchWheel(sender: BleMidiDevice, cable: Int, channel: Int, amount: Int) {}
        override fun onMidiSongSelect(sender: BleMidiDevice, cable: Int, song: Int) {}
        override fun onMidiSongPositionPointer(sender: BleMidiDevice, cable: Int, position: Int) {}
        override fun onMidiTuneRequest(sender: BleMidiDevice, cable: Int) {}
        override fun onMidiTimingClock(sender: BleMidiDevice, cable: Int) {}
        override fun onMidiStart(sender: BleMidiDevice, cable: Int) {}
        override fun onMidiContinue(sender: BleMidiDevice, cable: Int) {}
        override fun onMidiStop(sender: BleMidiDevice, cable: Int) {}
        override fun onMidiActiveSensing(sender: BleMidiDevice, cable: Int) {}
        override fun onMidiReset(sender: BleMidiDevice, cable: Int) {}
        override fun onMidiRPNReceived(sender: BleMidiDevice, cable: Int, channel: Int, function: Int, valueMSB: Int, valueLSB: Int) {}
        override fun onMidiNRPNReceived(sender: BleMidiDevice, cable: Int, channel: Int, function: Int, valueMSB: Int, valueLSB: Int) {}
        override fun onMidiRPNReceived(sender: BleMidiDevice, cable: Int, channel: Int, function: Int, value: Int) {}
        override fun onMidiNRPNReceived(sender: BleMidiDevice, cable: Int, channel: Int, function: Int, value: Int) {}
    }

    fun start() {
        // --- 1. Inicialização USB ---
        try {
            usbMidiDriver = object : UsbMidiDriver(context) {
                override fun onMidiInputDeviceAttached(midiInputDevice: UsbMidiDevice) {
                    onLog("USB conectado: ${midiInputDevice.usbDevice.deviceName}")
                }
                override fun onMidiInputDeviceDetached(midiInputDevice: UsbMidiDevice) {
                    onLog("USB desconectado")
                }
                override fun onMidiOutputDeviceAttached(midiOutputDevice: UsbMidiOutputDevice) {}
                override fun onMidiOutputDeviceDetached(midiOutputDevice: UsbMidiOutputDevice) {}

                // Eventos MIDI do teclado USB
                override fun onMidiNoteOn(sender: UsbMidiDevice, cable: Int, channel: Int, note: Int, velocity: Int) {
                    onMidiEvent(0x90 or (channel and 0x0F), note, velocity)
                }
                override fun onMidiNoteOff(sender: UsbMidiDevice, cable: Int, channel: Int, note: Int, velocity: Int) {
                    onMidiEvent(0x80 or (channel and 0x0F), note, velocity)
                }
                override fun onMidiControlChange(sender: UsbMidiDevice, cable: Int, channel: Int, function: Int, value: Int) {
                    onMidiEvent(0xB0 or (channel and 0x0F), function, value)
                }

                // Métodos obrigatórios da interface
                override fun onMidiMiscellaneousFunctionCodes(sender: UsbMidiDevice, cable: Int, byte1: Int, byte2: Int, byte3: Int) {}
                override fun onMidiCableEvents(sender: UsbMidiDevice, cable: Int, byte1: Int, byte2: Int, byte3: Int) {}
                override fun onMidiSystemCommonMessage(sender: UsbMidiDevice, cable: Int, bytes: ByteArray?) {}
                override fun onMidiSystemExclusive(sender: UsbMidiDevice, cable: Int, systemExclusive: ByteArray?) {}
                override fun onMidiPolyphonicAftertouch(sender: UsbMidiDevice, cable: Int, channel: Int, note: Int, pressure: Int) {}
                override fun onMidiProgramChange(sender: UsbMidiDevice, cable: Int, channel: Int, program: Int) {}
                override fun onMidiChannelAftertouch(sender: UsbMidiDevice, cable: Int, channel: Int, pressure: Int) {}
                override fun onMidiPitchWheel(sender: UsbMidiDevice, cable: Int, channel: Int, amount: Int) {}
                override fun onMidiSongSelect(sender: UsbMidiDevice, cable: Int, song: Int) {}
                override fun onMidiSongPositionPointer(sender: UsbMidiDevice, cable: Int, position: Int) {}
                override fun onMidiTuneRequest(sender: UsbMidiDevice, cable: Int) {}
                override fun onMidiTimingClock(sender: UsbMidiDevice, cable: Int) {}
                override fun onMidiStart(sender: UsbMidiDevice, cable: Int) {}
                override fun onMidiContinue(sender: UsbMidiDevice, cable: Int) {}
                override fun onMidiStop(sender: UsbMidiDevice, cable: Int) {}
                override fun onMidiActiveSensing(sender: UsbMidiDevice, cable: Int) {}
                override fun onMidiReset(sender: UsbMidiDevice, cable: Int) {}
                override fun onMidiRPNReceived(sender: UsbMidiDevice, cable: Int, channel: Int, function: Int, valueMSB: Int, valueLSB: Int) {}
                override fun onMidiNRPNReceived(sender: UsbMidiDevice, cable: Int, channel: Int, function: Int, valueMSB: Int, valueLSB: Int) {}
                override fun onMidiRPNReceived(sender: UsbMidiDevice, cable: Int, channel: Int, function: Int, value: Int) {}
                override fun onMidiNRPNReceived(sender: UsbMidiDevice, cable: Int, channel: Int, function: Int, value: Int) {}
            }.apply { open() }
            onLog("USB MIDI driver iniciado")
        } catch (e: Exception) {
            onLog("Erro USB: ${e.message}")
        }

        // --- 2. Inicialização BLE ---
        try {
            bleMidiProvider = BleMidiCentralProvider(context).apply {
                setOnMidiDeviceAttachedListener(object : OnMidiDeviceAttachedListener {
                    override fun onMidiInputDeviceAttached(midiInputDevice: BleMidiDevice) {
                        midiInputDevice.setMidiEventListener(bleListener)
                        onLog("BLE conectado: ${midiInputDevice.deviceName}")
                    }
                    override fun onMidiOutputDeviceAttached(midiOutputDevice: jp.kshoji.blemidi.device.MidiOutputDevice) {}
                })
                startScanDevice()
            }
            onLog("BLE MIDI iniciado (scan)")
        } catch (e: Exception) {
            onLog("Erro BLE: ${e.message}")
        }
    }

    fun stop() {
        usbMidiDriver?.close()
        bleMidiProvider?.stopScanDevice()
    }
}
