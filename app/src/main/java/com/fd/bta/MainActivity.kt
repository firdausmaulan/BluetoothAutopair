package com.fd.bta

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.fd.bta.databinding.ActivityMainBinding

@SuppressLint("MissingPermission")
class MainActivity : AppCompatActivity() {

    private val binding: ActivityMainBinding by lazy { ActivityMainBinding.inflate(layoutInflater) }
    private val permissionHelper = PermissionHelper()
    private var bluetoothAdapter: BluetoothAdapter? = null
    private var deviceList: MutableList<BluetoothDevice> = mutableListOf()
    private var deviceNameList: MutableList<String> = mutableListOf()
    private lateinit var deviceListAdapter: ArrayAdapter<String>

    private var enableBluetoothLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                registerDiscoveryReceiver()
                startDiscovery()
            }
        }

    // Create a BroadcastReceiver for ACTION_FOUND
    private val bluetoothDiscoveryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    // Discovery has found a device. Get the BluetoothDevice object and its info.
                    val device: BluetoothDevice? = intent.parcelable(BluetoothDevice.EXTRA_DEVICE)
                    device?.let {
                        if (!deviceList.contains(device)) {
                            deviceList.add(device)
                            deviceNameList.add(device.name ?: "Unknown Device")
                            deviceListAdapter.notifyDataSetChanged()
                        }
                    }
                }
                BluetoothAdapter.ACTION_DISCOVERY_STARTED -> {
                    // You might want to show a progress indicator here
                    Toast.makeText(this@MainActivity, "Discovery started", Toast.LENGTH_SHORT).show()
                }
                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                    // You might want to hide a progress indicator here
                    Toast.makeText(this@MainActivity, "Discovery finished", Toast.LENGTH_SHORT).show()
                    unregisterReceiver(this) // Unregister the receiver as we got the result
                    if (deviceList.isNotEmpty()) registerBoundStateReceiver()
                }
            }
        }
    }

    private val bondStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (BluetoothDevice.ACTION_BOND_STATE_CHANGED == action) {
                val device: BluetoothDevice? = intent.parcelable(BluetoothDevice.EXTRA_DEVICE)
                when (device?.bondState) {
                    BluetoothDevice.BOND_BONDING -> {
                        Toast.makeText(context, "Pairing with ${device.name}...", Toast.LENGTH_SHORT).show()
                    }
                    BluetoothDevice.BOND_BONDED -> {
                        Toast.makeText(context, "Paired with ${device.name}.", Toast.LENGTH_SHORT).show()
                        // Perform actions after successful pairing
                        unregisterReceiver(this) // Unregister the receiver as we got the result
                    }
                    BluetoothDevice.BOND_NONE -> {
                        Toast.makeText(context, "Failed to pair with ${device.name}.", Toast.LENGTH_SHORT).show()
                        unregisterReceiver(this) // Unregister the receiver
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        setupListView()

        if (isBluetoothEnable()) {
            registerDiscoveryReceiver()
            startDiscovery()
        }
    }

    private fun setupListView() {
        deviceListAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, deviceNameList)
        binding.deviceListView.adapter = deviceListAdapter
        binding.deviceListView.setOnItemClickListener { _, _, position, _ ->
            val selectedDevice : BluetoothDevice = deviceList[position]
            Toast.makeText(this, "Clicked: ${selectedDevice.name}", Toast.LENGTH_SHORT).show()

            if (selectedDevice.bondState == BluetoothDevice.BOND_BONDED) {
                Toast.makeText(this, "${selectedDevice.name} is already paired.", Toast.LENGTH_SHORT).show()
                return@setOnItemClickListener
            }

            try {
                val method = selectedDevice.javaClass.getMethod("createBond")
                val isBonded = method.invoke(selectedDevice) as Boolean
                if (isBonded) {
                    Toast.makeText(this, "Initiating pairing with ${selectedDevice.name}...", Toast.LENGTH_SHORT).show()
                    // The bondStateReceiver will handle the pairing status updates
                } else {
                    Toast.makeText(this, "Failed to initiate pairing with ${selectedDevice.name}.", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this, "Error during pairing: ${e.message}", Toast.LENGTH_SHORT).show()
                e.printStackTrace()
            }
        }
    }

    private fun getBluetoothAdapter(): BluetoothAdapter? {
        if (bluetoothAdapter == null) {
            bluetoothAdapter = (getSystemService(BLUETOOTH_SERVICE) as BluetoothManager).adapter
        }
        return bluetoothAdapter
    }

    private fun isBluetoothEnable(): Boolean {
        if (permissionHelper.isBluetoothPermissionGranted(this)) {
            return getBluetoothAdapter()?.isEnabled == true
        }
        return false
    }

    private fun registerDiscoveryReceiver() {
        // Register for multiple bluetooth actions
        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_FOUND)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(bluetoothDiscoveryReceiver, filter, RECEIVER_EXPORTED)
        } else {
            registerReceiver(bluetoothDiscoveryReceiver, filter)
        }
    }

    private fun registerBoundStateReceiver() {
        val filter = IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(bondStateReceiver, filter, RECEIVER_EXPORTED)
        } else {
            registerReceiver(bondStateReceiver, filter)
        }
    }

    private fun enableBluetooth() {
        enableBluetoothLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String?>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (getBluetoothAdapter()?.isEnabled == true) {
            registerDiscoveryReceiver()
            startDiscovery()
        } else {
            enableBluetooth()
        }
    }

    private fun startDiscovery() {
        deviceList.clear()
        deviceListAdapter.notifyDataSetChanged()
        getBluetoothAdapter()?.startDiscovery()
    }

    private fun showPairedDevices() {
        val pairedDevices: Set<BluetoothDevice>? = getBluetoothAdapter()?.bondedDevices
        pairedDevices?.forEach { device ->
            deviceList.add(device)
            deviceNameList.add(device.name)
        }
        deviceListAdapter.notifyDataSetChanged()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Make sure we're no longer discovering
        getBluetoothAdapter()?.cancelDiscovery()
        // Unregister broadcast listener
        unregisterReceiver(bluetoothDiscoveryReceiver)
        unregisterReceiver(bondStateReceiver)
    }
}