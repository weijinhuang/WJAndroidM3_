package com.wj.androidm3.business.ui.test

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.content.pm.PackageManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.recyclerview.widget.LinearLayoutManager
import com.polidea.rxandroidble3.LogConstants
import com.polidea.rxandroidble3.LogOptions
import com.polidea.rxandroidble3.RxBleClient
import com.polidea.rxandroidble3.scan.IsConnectable
import com.polidea.rxandroidble3.scan.ScanResult
import com.polidea.rxandroidble3.scan.ScanSettings
import com.wj.androidm3.BuildConfig
import com.wj.androidm3.R
import com.wj.androidm3.databinding.FragmentBleTestBinding
import com.wj.basecomponent.ui.BaseMVVMFragment
import com.wj.basecomponent.util.log.WJLog
import com.wj.basecomponent.vm.BaseViewModel
import io.reactivex.rxjava3.disposables.Disposable


class BLETestFragment : BaseMVVMFragment<BaseViewModel, FragmentBleTestBinding>() {

    lateinit var mRxBleClient: RxBleClient

    private var mScanDisposable: Disposable? = null

    val mAdapter = BLEDeviceListAdapter()

    val mBLEResultList = mutableListOf<ScanResult>()

    private val mRequestBluetoothEnableContract =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissionResultMap ->
//            var hasPermission = true
//            permissionResultMap.values.forEach { granted ->
//                if (!granted) {
//                    hasPermission = false
//                    return@forEach
//                }
//            }
//            if (hasPermission) {
                val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                val REQUEST_ENABLE_BT = 1
                requireActivity().startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
//            }
        }

    override fun firstCreateView() {
        mRxBleClient = RxBleClient.create(requireContext())
        RxBleClient.updateLogOptions(
            LogOptions.Builder()
                .setLogLevel(if (BuildConfig.DEBUG) LogConstants.VERBOSE else LogConstants.NONE)
                .setMacAddressLogSetting(LogConstants.MAC_ADDRESS_FULL)
                .setUuidsLogSetting(LogConstants.UUIDS_FULL)
                .setShouldLogAttributeValues(true)
                .build()
        )


        mAdapter.setOnClickListener { scanResult: ScanResult ->
            val bleDevice = mRxBleClient.getBleDevice(scanResult.bleDevice.macAddress)
            val establishConnection = bleDevice.establishConnection(false)
            establishConnection.subscribe(
                { rxBleConnection ->

                    WJLog.e("连接${scanResult.bleDevice.name}成功")
                },
                { error ->
                    WJLog.e("连接蓝牙失败：$error")
                })

        }
        enableBle()
        mViewBinding?.run {
            btnStartScan.setOnClickListener {
                if (isBleEnable() && supportBLE()) {

                    stopBleScan()

                    mRxBleClient?.let {
                        mScanDisposable = it.scanBleDevices(
                            ScanSettings.Builder()
                                .setScanMode(ScanSettings.SCAN_MODE_BALANCED)
                                .setCallbackType(ScanSettings.CALLBACK_TYPE_FIRST_MATCH)
                                .build()
                        )
                            .subscribe({ scanResult ->
                                if (scanResult.isConnectable == IsConnectable.CONNECTABLE) {
                                    var hasDevice = false
                                    mBLEResultList.forEach { bleDeviceResult ->
                                        if (bleDeviceResult.bleDevice.macAddress == scanResult.bleDevice.macAddress) {
                                            hasDevice = true
                                            return@forEach
                                        }
                                    }
                                    if (!hasDevice) {
                                        mBLEResultList.add(scanResult)
                                        val newList = mBLEResultList.toMutableList()
                                        mAdapter.submitList(newList)
                                    }

                                }

                            }, {
                                WJLog.e("搜索蓝牙设备报错：${it.message}")
                            })

                    }

                } else {
                    enableBle()
                }
            }

            btnStopScan.setOnClickListener {
                stopBleScan()
            }

            mViewBinding?.recyclerView?.let { recyclerView ->
                recyclerView.adapter = mAdapter
                recyclerView.layoutManager = LinearLayoutManager(requireActivity(), LinearLayoutManager.VERTICAL, false)
                recyclerView.setHasFixedSize(true)
            }
        }

    }

    override fun onDestroy() {
        super.onDestroy()
        stopBleScan()
    }

    private fun stopBleScan() {
        mScanDisposable?.let {
            if (!it.isDisposed) {
                it.dispose()
            }
        }
    }


    private fun enableBle() {
        mRequestBluetoothEnableContract.launch(
            arrayOf(
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )

    }

    override fun getLayoutId(): Int {
        return R.layout.fragment_ble_test
    }

    fun supportBLE(): Boolean {
        val packageManager = requireActivity().packageManager
        // Check to see if the Bluetooth classic feature is available.
        val bluetoothAvailable = packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH)

// Check to see if the BLE feature is available.
        val bluetoothLEAvailable = packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)
        WJLog.d("bluetoothAvailable:$bluetoothAvailable  bluetoothLEAvailable:$bluetoothLEAvailable")

        return bluetoothAvailable && bluetoothLEAvailable
    }

    /***
     * 判断蓝牙是否开启
     */
    fun isBleEnable(): Boolean {
        val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
        if (bluetoothAdapter != null) {
            WJLog.d("address:${bluetoothAdapter.address}")
            return bluetoothAdapter.isEnabled
        }
        return false
    }
}