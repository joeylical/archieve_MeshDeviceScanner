package com.ediacaranstudio.meshdevicescanner

import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.*
import android.bluetooth.le.AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.MobileAds
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.network_item.view.*
import kotlinx.android.synthetic.main.network_item.view.item_mac
import kotlinx.android.synthetic.main.network_item.view.item_name
import kotlinx.android.synthetic.main.network_item.view.item_rssi
import kotlinx.android.synthetic.main.unprovisioned_item.view.*
import java.nio.ByteBuffer
import java.util.*

class MainActivity : AppCompatActivity() {

    data class MeshProvisionedDevice(var name:String?, val mac:String, var rssi:Int, var keyRefresh:Boolean?, var ivUpdate:Boolean?, var ivIndex:Int?, var networkId:String?)
    data class MeshUnProvisionedDevice(var name:String?, val mac:String, var rssi:Int, var uuid:UUID?)

    val devList = mutableListOf<String>()
    val devInfo = mutableMapOf<String, Any>()

    lateinit var mBluetoothAdapter:BluetoothAdapter
    lateinit var scanner:BluetoothLeScanner
    lateinit var advister:BluetoothLeAdvertiser

    fun hexToString(b:ByteArray): String {
        val sb = StringBuilder()
        for(c in b) {
            sb.append(String.format("%02X", c))
        }

        return sb.toString()
    }

    fun hexToUUID(b:ByteArray): UUID {
        val bb = ByteBuffer.wrap(b)
        val high = bb.getLong()
        val low = bb.getLong()
        return UUID(high, low)
    }

    fun startScan() {
        devList.clear()
        devInfo.clear()
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        scanner = mBluetoothAdapter.bluetoothLeScanner
        advister = mBluetoothAdapter.bluetoothLeAdvertiser

        val bleScanFilters = listOf<ScanFilter>(
        )

        val scanSetting = ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build()

//        devList.add("00:11:22:33:44:55")
//        devInfo["00:11:22:33:44:55"] = MeshProvisionedDevice("Ble Mesh Light", "00:11:22:33:44:55", -60, true, false, 35, "3156CFA8F47D3126")
//
//        devList.add("55:44:33:22:11:00")
//        devInfo["55:44:33:22:11:00"] = MeshUnProvisionedDevice("Ble Mesh Light", "55:44:33:22:11:00", -90, UUID.randomUUID())

        val mAdapter = object: BaseAdapter() {
            override fun getCount(): Int {
                return devList.size
            }

            override fun getItem(position: Int): Any {
                return position
            }

            override fun getItemId(position: Int): Long {
                return position.toLong()
            }

            override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
                val inflater = this@MainActivity.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

                val addr = devList[position]

                if(devInfo[addr] is MeshUnProvisionedDevice) {
                    val temp = devInfo[addr] as MeshUnProvisionedDevice
                    val t = inflater.inflate(R.layout.unprovisioned_item, null)
                    t.item_mac.text = addr
                    t.item_name.text = temp?.name ?: "null"
                    if(temp?.name == null) {
                        t.item_name.typeface = Typeface.defaultFromStyle(Typeface.ITALIC)
                    }else{
                        t.item_name.typeface = Typeface.DEFAULT_BOLD
                    }
                    t.item_rssi.text = temp!!.rssi.toString() + " dBm"
                    t.item_uuid.text = temp!!.uuid.toString()
                    return t
                }else if(devInfo[addr] is MeshProvisionedDevice) {
                    val temp = devInfo[addr] as MeshProvisionedDevice
                    val t = inflater.inflate(R.layout.network_item, null)
                    t.item_mac.text = addr
                    t.item_name.text = temp?.name ?: "null"
                    if(temp?.name == null) {
                        t.item_name.typeface = Typeface.defaultFromStyle(Typeface.ITALIC)
                    }else{
                        t.item_name.typeface = Typeface.DEFAULT_BOLD
                    }
                    t.item_rssi.text = temp!!.rssi.toString() + " dBm"
                    t.iv_index.text = "IV Index:" + temp?.ivIndex.toString()
                    t.iv_update.isChecked = temp?.ivUpdate ?: false
                    t.key_refresh.isChecked = temp?.keyRefresh ?: false
                    t.network_id.text = "Network ID: " + temp?.networkId ?: ""
                    t.btn_offline.setOnTouchListener { v, event ->
                        Toast.makeText(this@MainActivity, "离网", Toast.LENGTH_LONG).show()
//                        Thread{
                        val settings = AdvertiseSettings.Builder()
                            .setAdvertiseMode(ADVERTISE_MODE_LOW_LATENCY)
                            .setConnectable(false)
                            .setTimeout(1000)
                            .build()

                        val tt = mutableListOf<Byte>()

                        for(i in 0..5) {
                            val h = addr.subSequence(i*3,i*3+2).toString()
                            val b = Integer.parseInt(h, 16).toByte()
                            tt.add(b)
                        }

//                            val t = MacAddress.fromString(addr)
                        val prefix = byteArrayOf(77.toByte(), 79.toByte(), 86.toByte(), 69.toByte())
                        val advdata = AdvertiseData.Builder()
                            .addManufacturerData(0x4552, prefix + tt)
                            .build()

                        Log.i("debug", hexToString(prefix + tt))

                        advister.startAdvertising(settings, advdata, object:AdvertiseCallback(){})
                        true
                    }
                    t.btn_offline.setOnClickListener{
                        //                        Toast.makeText(this@MainActivity, "离网", Toast.LENGTH_LONG).show()
////                        Thread{
//                            val advister = mBluetoothAdapter.bluetoothLeAdvertiser
//                            val settings = AdvertiseSettings.Builder()
//                                .setAdvertiseMode(ADVERTISE_MODE_LOW_LATENCY)
//                                .setConnectable(false)
//                                .setTimeout(1000)
//                                .build()
//
//                            val tt = mutableListOf<Byte>()
//
//                            for(i in 0..5) {
//                                val h = addr.subSequence(i*3,i*3+2).toString()
//                                val b = Integer.parseInt(h, 16).toByte()
//                                tt.add(b)
//                            }
//
////                            val t = MacAddress.fromString(addr)
//                            val prefix = byteArrayOf(77.toByte(), 79.toByte(), 86.toByte(), 69.toByte())
//                            val advdata = AdvertiseData.Builder()
//                                .addManufacturerData(0x4552, prefix + tt)
//                                .build()
//
//                            Log.i("debug", hexToString(prefix + tt))
//
//                        advister.startAdvertising(settings, advdata, object:AdvertiseCallback(){})
                    }
                    return t
                }else{
                    return TextView(this@MainActivity)
                }
            }
        }

        result_listview.adapter = mAdapter

        val scanCallback = object: ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult?) {
                super.onScanResult(callbackType, result)
                if(result == null)
                    return
                val dev = result.device
                val addr = dev.address.toUpperCase()
                val rssi = result.rssi
                val adv_array = result.scanRecord.bytes

                var name:String? = null
                var unprovisioned = false
                var provisioned = true
                var networkId:String? = null
                var uuid:UUID? = null
                var oob:Int? = null
                var keyRefresh:Boolean? = null
                var ivUpdate:Boolean? = null
                var ivIndex:Int? = null

                var i = 0
                while(i < adv_array.size) {
                    val len = adv_array[i].toInt()
                    if(len == 0)
                        break
                    val type = adv_array[i+1].toInt()
                    val data = adv_array.sliceArray(IntRange(i+2, i+len))


                    when(type) {
                        0x09 -> {
                            name = String(data)
                        }
                        0x16 -> {
                            val serviceUUID = data[1].toInt()*256 + data[0]
                            if(serviceUUID == 0x1827) {
                                unprovisioned = true
                                uuid = hexToUUID(data.sliceArray(IntRange(2, 18)))
                                oob = data[19]*256+data[18]
                            }else if(serviceUUID == 0x1828) {
                                if(data[2].toInt() == 0) {
                                    provisioned = true
                                    networkId = hexToString(data.sliceArray(3..10))
                                }
                            }
                        }
                        0x2b -> {
                            if(data[0].toInt() == 0) {
                                // Unprovisioned
                                unprovisioned = true
                                uuid = hexToUUID(data.sliceArray(1..16))
                                oob = data[18]*256+data[17]
                            }else if(data[0].toInt() == 1) {
                                // Provisioned
                                provisioned = true
                                val flag = data[1].toInt()
                                keyRefresh = (flag and 0x01 == 0x01)
                                ivUpdate = (flag and 0x02 == 0x02)
                                networkId = hexToString(data.sliceArray(2..9))
                                val temp = data.sliceArray(10..13)
                                ivIndex = temp[0]*256*256*256+temp[1]*256*256+temp[2]*256+temp[3]
                            }
                        }

                    }

                    i += len +1
                }

                if(unprovisioned && uuid != null) {
                    if(devInfo.containsKey(addr)) {
                        val dev = devInfo[addr] as MeshUnProvisionedDevice
                        if(name != null)
                            dev.name = name
                        dev.rssi = rssi
                        if(uuid != null)
                            dev.uuid = uuid
                    }else{
                        devList.add(addr)
                        devInfo[addr] = MeshUnProvisionedDevice(name, addr, rssi, uuid)
                    }
                    mAdapter.notifyDataSetChanged()
                }else if(provisioned && networkId != null) {
                    if(devInfo.containsKey(addr)) {
                        val dev = devInfo[addr] as MeshProvisionedDevice
                        if(name != null)
                            dev.name = name
                        dev.rssi = rssi
                        if(networkId != null)
                            dev.networkId = networkId
                        if(ivIndex != null)
                            dev.ivIndex = ivIndex
                        if(ivUpdate != null)
                            dev.ivUpdate = ivUpdate
                        if(keyRefresh != null)
                            dev.keyRefresh = keyRefresh
                    }else{
                        devList.add(addr)
                        devInfo[addr] = MeshProvisionedDevice(name, addr, rssi, keyRefresh, ivUpdate, ivIndex, networkId)
                    }
                    mAdapter.notifyDataSetChanged()
                }
            }
        }

        scanner.startScan(bleScanFilters, scanSetting, scanCallback)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        MobileAds.initialize(this, "ca-app-pub-3940256099942544~3347511713")

        adView.loadAd(AdRequest.Builder().build())

        if ((ContextCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED) ||
            (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)) {
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.BLUETOOTH_ADMIN, android.Manifest.permission.ACCESS_COARSE_LOCATION), 0)
        }else{
            startScan()
        }
    }

    override fun onPause() {
        super.onPause()
        advister?.stopAdvertising(object:AdvertiseCallback(){})
        scanner?.stopScan(object:ScanCallback(){})
    }

    override fun onResume() {
        super.onResume()
        if ((ContextCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED) ||
            (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)) {
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.BLUETOOTH_ADMIN, android.Manifest.permission.ACCESS_COARSE_LOCATION), 0)
        }else{
            startScan()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if(grantResults.all { it == PackageManager.PERMISSION_GRANTED })
            startScan()
    }
}
