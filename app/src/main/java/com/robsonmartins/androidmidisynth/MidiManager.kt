package com.robsonmartins.androidmidisynth

import android.content.Context
import android.hardware.usb.UsbDevice
import jp.kshoji.blemidi.central.BleMidiCentralProvider
import jp.kshoji.blemidi.device.MidiInputDevice as BleMidiDevice
import jp.kshoji.blemidi.listener.OnMidiDeviceAttachedListener as BleDeviceAttachedListener
import jp.kshoji.blemidi.listener.OnMidiInputEventListener as BleMidiListener
import jp.kshoji.driver.midi.device.MidiInputDevice as UsbMidiDevice
import jp.kshoji.driver.midi.device.MidiOutputDevice as UsbMidiOutputDevice
import jp.kshoji.driver.midi.util.UsbMidiDriver

/**
 * Recebe eventos MIDI de dispositivos USB e BLE e os encaminha ao motor de áudio.
 *
 * As implementações seguem as assinaturas das versões fixadas no catálogo Gradle:
 * midi-driver 0.1.12 e ble-midi 0.0.19.
 */
class MidiManager(
    private val context: Context,
    private val onLog: (String) -> Unit,
    private val onMidiEvent: (status: Int, data1: Int, data2: Int) -> Unit
) {
    private var usbMidiDriver: UsbMidiDriver? = null
    private var bleMidiProvider: BleMidiCentralProvider? = null

    /**
     * A API BLE 0.0.19 não recebe o número do cabo MIDI nos callbacks.
     * O listener é instalado diretamente no MidiInputDevice quando ele conecta.
     */
    private val bleListener = object : BleMidiListener {
        override fun onMidiNoteOn(sender: BleMidiDevice, channel: Int, note: Int, velocity: Int) {
            onMidiEvent(0x90 or (channel and 0x0F), note, velocity)
        }

        override fun onMidiNoteOff(sender: BleMidiDevice, channel: Int, note: Int, velocity: Int) {
            onMidiEvent(0x80 or (channel and 0x0F), note, velocity)
        }

        override fun onMidiControlChange(sender: BleMidiDevice, channel: Int, function: Int, value: Int) {
            onMidiEvent(0xB0 or (channel and 0x0F), function, value)
        }

        override fun onMidiSystemExclusive(sender: BleMidiDevice, systemExclusive: ByteArray) {}
        override fun onMidiPolyphonicAftertouch(sender: BleMidiDevice, channel: Int, note: Int, pressure: Int) {}
        override fun onMidiProgramChange(sender: BleMidiDevice, channel: Int, program: Int) {
            onMidiEvent(0xC0 or (channel and 0x0F), program, 0)
        }
        override fun onMidiChannelAftertouch(sender: BleMidiDevice, channel: Int, pressure: Int) {}
        override fun onMidiPitchWheel(sender: BleMidiDevice, channel: Int, amount: Int) {
            onMidiEvent(0xE0 or (channel and 0x0F), amount and 0x7F, (amount shr 7) and 0x7F)
        }
        override fun onMidiTimeCodeQuarterFrame(sender: BleMidiDevice, timing: Int) {}
        override fun onMidiSongSelect(sender: BleMidiDevice, song: Int) {}
        override fun onMidiSongPositionPointer(sender: BleMidiDevice, position: Int) {}
        override fun onMidiTuneRequest(sender: BleMidiDevice) {}
        override fun onMidiTimingClock(sender: BleMidiDevice) {}
        override fun onMidiStart(sender: BleMidiDevice) {}
        override fun onMidiContinue(sender: BleMidiDevice) {}
        override fun onMidiStop(sender: BleMidiDevice) {}
        override fun onMidiActiveSensing(sender: BleMidiDevice) {}
        override fun onMidiReset(sender: BleMidiDevice) {}
        override fun onRPNMessage(sender: BleMidiDevice, channel: Int, function: Int, value: Int) {}
        override fun onNRPNMessage(sender: BleMidiDevice, channel: Int, function: Int, value: Int) {}
    }

    fun startUsb() {
        // USB MIDI 0.1.12: UsbMidiDriver implementa os próprios callbacks de entrada.
        try {
            usbMidiDriver = object : UsbMidiDriver(context) {
                override fun onDeviceAttached(usbDevice: UsbDevice) {
                    onLog("Dispositivo USB conectado: ${usbDevice.deviceName}")
                }

                override fun onDeviceDetached(usbDevice: UsbDevice) {
                    onLog("Dispositivo USB desconectado")
                }

                override fun onMidiInputDeviceAttached(midiInputDevice: UsbMidiDevice) {
                    onLog("USB MIDI conectado: ${midiInputDevice.usbDevice.deviceName}")
                }

                override fun onMidiInputDeviceDetached(midiInputDevice: UsbMidiDevice) {
                    onLog("USB MIDI desconectado")
                }

                override fun onMidiOutputDeviceAttached(midiOutputDevice: UsbMidiOutputDevice) {}
                override fun onMidiOutputDeviceDetached(midiOutputDevice: UsbMidiOutputDevice) {}

                // Eventos MIDI encaminhados pelo próprio UsbMidiDriver.
                override fun onMidiNoteOn(sender: UsbMidiDevice, cable: Int, channel: Int, note: Int, velocity: Int) {
                    onMidiEvent(0x90 or (channel and 0x0F), note, velocity)
                }

                override fun onMidiNoteOff(sender: UsbMidiDevice, cable: Int, channel: Int, note: Int, velocity: Int) {
                    onMidiEvent(0x80 or (channel and 0x0F), note, velocity)
                }

                override fun onMidiControlChange(sender: UsbMidiDevice, cable: Int, channel: Int, function: Int, value: Int) {
                    onMidiEvent(0xB0 or (channel and 0x0F), function, value)
                }

                // Demais callbacks obrigatórios da interface USB MIDI.
                override fun onMidiMiscellaneousFunctionCodes(sender: UsbMidiDevice, cable: Int, byte1: Int, byte2: Int, byte3: Int) {}
                override fun onMidiCableEvents(sender: UsbMidiDevice, cable: Int, byte1: Int, byte2: Int, byte3: Int) {}
                override fun onMidiSystemCommonMessage(sender: UsbMidiDevice, cable: Int, bytes: ByteArray) {}
                override fun onMidiSystemExclusive(sender: UsbMidiDevice, cable: Int, systemExclusive: ByteArray) {}
                override fun onMidiPolyphonicAftertouch(sender: UsbMidiDevice, cable: Int, channel: Int, note: Int, pressure: Int) {}
                override fun onMidiProgramChange(sender: UsbMidiDevice, cable: Int, channel: Int, program: Int) {
                    onMidiEvent(0xC0 or (channel and 0x0F), program, 0)
                }
                override fun onMidiChannelAftertouch(sender: UsbMidiDevice, cable: Int, channel: Int, pressure: Int) {}
                override fun onMidiPitchWheel(sender: UsbMidiDevice, cable: Int, channel: Int, amount: Int) {
                    onMidiEvent(0xE0 or (channel and 0x0F), amount and 0x7F, (amount shr 7) and 0x7F)
                }
                override fun onMidiSingleByte(sender: UsbMidiDevice, cable: Int, byte1: Int) {}
                override fun onMidiTimeCodeQuarterFrame(sender: UsbMidiDevice, cable: Int, timing: Int) {}
                override fun onMidiSongSelect(sender: UsbMidiDevice, cable: Int, song: Int) {}
                override fun onMidiSongPositionPointer(sender: UsbMidiDevice, cable: Int, position: Int) {}
                override fun onMidiTuneRequest(sender: UsbMidiDevice, cable: Int) {}
                override fun onMidiTimingClock(sender: UsbMidiDevice, cable: Int) {}
                override fun onMidiStart(sender: UsbMidiDevice, cable: Int) {}
                override fun onMidiContinue(sender: UsbMidiDevice, cable: Int) {}
                override fun onMidiStop(sender: UsbMidiDevice, cable: Int) {}
                override fun onMidiActiveSensing(sender: UsbMidiDevice, cable: Int) {}
                override fun onMidiReset(sender: UsbMidiDevice, cable: Int) {}
            }.apply {
                open()
            }
            onLog("USB MIDI iniciado")
        } catch (e: Exception) {
            onLog("Erro USB: ${e.message}")
        }
    }

    fun startBle() {
        // BLE MIDI 0.0.19: o listener é associado ao dispositivo de entrada.
        try {
            bleMidiProvider = BleMidiCentralProvider(context).apply {
                setOnMidiDeviceAttachedListener(object : BleDeviceAttachedListener {
                    override fun onMidiInputDeviceAttached(midiInputDevice: BleMidiDevice) {
                        midiInputDevice.setOnMidiInputEventListener(bleListener)
                        onLog("BLE MIDI conectado: ${midiInputDevice.deviceName}")
                    }

                    override fun onMidiOutputDeviceAttached(midiOutputDevice: jp.kshoji.blemidi.device.MidiOutputDevice) {
                        onLog("Dispositivo BLE MIDI de saída conectado")
                    }
                })
                startScanDevice(0)
            }
            onLog("BLE MIDI iniciado (busca contínua)")
        } catch (e: Exception) {
            onLog("Erro BLE: ${e.message}")
        }
    }

    fun stop() {
        try {
            usbMidiDriver?.close()
        } catch (e: Exception) {
            onLog("Erro ao encerrar USB MIDI: ${e.message}")
        }

        try {
            bleMidiProvider?.stopScanDevice()
        } catch (e: Exception) {
            onLog("Erro ao encerrar busca BLE MIDI: ${e.message}")
        }
    }
}
