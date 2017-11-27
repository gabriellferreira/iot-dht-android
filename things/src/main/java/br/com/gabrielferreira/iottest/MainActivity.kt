package br.com.gabrielferreira.iottest

import android.app.Activity
import android.content.ContentValues.TAG
import android.content.Context
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Bundle
import android.util.Log
import br.com.gabrielferreira.R
import com.felhr.usbserial.UsbSerialDevice
import com.felhr.usbserial.UsbSerialInterface
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_main.*
import java.nio.charset.Charset
import java.util.concurrent.TimeUnit

/*
	Name: Gabriel Lucas Ferreira
	Email: gabriel@vrp.com.br
	Date: 26/11/17 19:46
*/

class MainActivity : Activity() {

    private val compositeDisposable = CompositeDisposable()
    private var serial: UsbSerialDevice? = null

    private val database by lazy { FirebaseDatabase.getInstance() }
    private val relayToggleRef by lazy { database.getReference("relay_toggle") }
    private val tempToggleRef by lazy { database.getReference("temp_toggle") }
    private val tempRef by lazy { database.getReference("temp") }
    private val humRef by lazy { database.getReference("hum") }

    private val tempDisposable by lazy {
        Observable.interval(1, TimeUnit.SECONDS, Schedulers.io())
                .subscribe {
                    serial?.write("T#".toByteArray())
                }
    }

    private val mCallback: UsbSerialInterface.UsbReadCallback =
            UsbSerialInterface.UsbReadCallback { msg ->
                var dataStr = String(msg, Charset.forName("UTF-8")).trim()
                Log.d("MainActivity", "Data received: " + dataStr)

                if (dataStr.isNotEmpty()
                        && dataStr.first() == '$'
                        && dataStr.last() == '#') {
                    dataStr = dataStr.removePrefix("$").removeSuffix("#")
                    val listParameters = dataStr.split("-")
                    if (listParameters.size < 2) return@UsbReadCallback
                    when (listParameters.first()) {
                        "TH" -> runOnUiThread {
                            val params = listParameters[1].split("/")
                            tempRef.setValue(params[0])
                            humRef.setValue(params[1])
                        }
                        "R" -> {
                            if (listParameters[1] == "T") {
                                if (!relay_toggle.isEnabled) {
                                    relayToggleRef.setValue(true)
                                }
                            } else {
                                if (relay_toggle.isEnabled) {
                                    relayToggleRef.setValue(false)
                                }
                            }
                        }
                    }
                }
            }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val usbManager = getSystemService(Context.USB_SERVICE) as UsbManager
        val connectedDevices = usbManager.deviceList
        for (device in connectedDevices.values) {
            if (device.vendorId == 9025 && device.productId == 67) {
                Log.i(TAG, "Device found: " + device.deviceName)
                startSerialConnection(usbManager, device)
                break
            }
        }
        initListeners()
    }

    private fun initListeners() {
        temp_hum_toggle.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                compositeDisposable.add(tempDisposable)
            } else {
                compositeDisposable.remove(tempDisposable)
            }
            tempToggleRef.setValue(isChecked)
        }
        relay_toggle.setOnCheckedChangeListener { _, isChecked ->
            relayToggleRef.setValue(isChecked)
        }
        tempRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (!dataSnapshot.exists()) return
                val value = dataSnapshot.getValue(String::class.java) ?: "Temp: -"
                temp.text = value
            }

            override fun onCancelled(error: DatabaseError) {
                // Failed to read value
                Log.w(TAG, "Failed to read value.", error.toException())
            }
        })
        humRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (!dataSnapshot.exists()) return
                val value = dataSnapshot.getValue(String::class.java) ?: "Hum: -"
                hum.text = value
            }

            override fun onCancelled(error: DatabaseError) {
                // Failed to read value
                Log.w(TAG, "Failed to read value.", error.toException())
            }
        })
        tempToggleRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                // This method is called once with the initial value and again
                // whenever data at this location is updated.
                if (!dataSnapshot.exists()) return
                val value = dataSnapshot.getValue(Boolean::class.java) ?: false
                if (temp_hum_toggle.isChecked == value) return
                temp_hum_toggle.isChecked = value
            }

            override fun onCancelled(error: DatabaseError) {
                // Failed to read value
                Log.w(TAG, "Failed to read value.", error.toException())
            }
        })
        relayToggleRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                // This method is called once with the initial value and again
                // whenever data at this location is updated.
                if (!dataSnapshot.exists()) return
                val value = dataSnapshot.getValue(Boolean::class.java) ?: false
                if (relay_toggle.isChecked == value) return
                relay_toggle.isChecked = value
                if (value) {
                    serial?.write("R-T#".toByteArray())
                } else {
                    serial?.write("R-F#".toByteArray())
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Failed to read value
                Log.w(TAG, "Failed to read value.", error.toException())
            }
        })
    }

    private fun startSerialConnection(usbManager: UsbManager, device: UsbDevice) {
        val connection = usbManager.openDevice(device)
        serial = UsbSerialDevice.createUsbSerialDevice(device, connection)

        if (serial != null && serial!!.open()) {
            serial!!.setBaudRate(115200)
            serial!!.setDataBits(UsbSerialInterface.DATA_BITS_8)
            serial!!.setStopBits(UsbSerialInterface.STOP_BITS_1)
            serial!!.setParity(UsbSerialInterface.PARITY_NONE)
//            serial!!.setFlowControl(UsbSerialInterface.FLOW_CONTROL_XON_XOFF)
            serial!!.read(mCallback)
        }
    }
}
