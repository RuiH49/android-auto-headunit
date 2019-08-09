package info.anodsplace.headunit.main

import android.app.Application
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import info.anodsplace.headunit.App
import info.anodsplace.headunit.connection.UsbDeviceCompat
import info.anodsplace.headunit.connection.UsbReceiver
import info.anodsplace.headunit.utils.Settings

class MainViewModel(application: Application): AndroidViewModel(application), UsbReceiver.Listener {

    val usbDevices = MutableLiveData<List<UsbDeviceCompat>>()

    private val app: App
        get() = getApplication()
    private val settings = Settings(application)
    private val usbReceiver = UsbReceiver(this)

    fun register() {
        app.registerReceiver(usbReceiver, UsbReceiver.createFilter())
        usbDevices.value = createDeviceList(settings.allowedDevices)
    }

    override fun onCleared() {
        app.unregisterReceiver(usbReceiver)
        super.onCleared()
    }

    override fun onUsbDetach(device: android.hardware.usb.UsbDevice) {
        usbDevices.value = createDeviceList(settings.allowedDevices)
    }

    override fun onUsbAttach(device: android.hardware.usb.UsbDevice) {
        usbDevices.value = createDeviceList(settings.allowedDevices)
    }

    override fun onUsbPermission(granted: Boolean, connect: Boolean, device: UsbDevice) {
        usbDevices.value = createDeviceList(settings.allowedDevices)
    }

    private fun createDeviceList(allowDevices: Set<String>): List<UsbDeviceCompat> {
        val manager = app.getSystemService(android.content.Context.USB_SERVICE) as UsbManager
        return manager.deviceList.entries
                .map { (_, device) -> UsbDeviceCompat(device) }
                .sortedWith(Comparator { lhs, rhs ->
                    if (lhs.isInAccessoryMode) {
                        return@Comparator -1
                    }
                    if (rhs.isInAccessoryMode) {
                        return@Comparator 1
                    }
                    if (allowDevices.contains(lhs.uniqueName)) {
                        return@Comparator -1
                    }
                    if (allowDevices.contains(rhs.uniqueName)) {
                        return@Comparator 1
                    }
                    lhs.uniqueName.compareTo(rhs.uniqueName)
                })
    }
}
