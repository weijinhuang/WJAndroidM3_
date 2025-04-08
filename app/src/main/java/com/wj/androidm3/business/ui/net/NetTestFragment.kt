package com.wj.androidm3.business.ui.net

import android.os.Build
import androidx.lifecycle.lifecycleScope
import com.wj.androidm3.R
import com.wj.androidm3.databinding.FragmentNetTestBinding
import com.wj.basecomponent.ui.BaseMVVMFragment
import com.wj.basecomponent.util.log.WJLog
import com.wj.basecomponent.vm.BaseViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.Socket
import java.util.Date

class NetTestFragment : BaseMVVMFragment<BaseViewModel, FragmentNetTestBinding>() {
    var socket: Socket? = null

    private fun initSocket(callback: (Socket) -> Unit) {
        if (null == socket) {
            WJLog.d("创建socket：192.168.3.188:7891")
            socket = Socket("192.168.3.188", 7891).apply {
                keepAlive = true
            }
        }
        callback.invoke(socket!!)
    }

    override fun firstCreateView() {
        lifecycleScope.launch(Dispatchers.IO) {
            initSocket {
                WJLog.i("tcp连接已建立")
                listenServer(it)
            }
        }

        mViewBinding?.run {
            sendTcpPacket.setOnClickListener {
                lifecycleScope.launch(Dispatchers.IO) {
                    try {
                        initSocket { socket ->
                            if (socket.isInputShutdown) {
                                WJLog.d("socket :${socket.inetAddress}  isInputShutdown")
                                return@initSocket
                            }
                            if (socket.isOutputShutdown) {
                                WJLog.d("socket :${socket.inetAddress} is isOutputShutdown")
                                return@initSocket
                            }
                            if (socket.isClosed) {
                                WJLog.d("socket :${socket.inetAddress} is closed")
                                return@initSocket
                            }
                            if (!socket.isConnected) {
                                WJLog.d("socket :${socket.inetAddress} is disconnect")
                                return@initSocket
                            }
                            WJLog.d("socket.isBound: ${socket.isBound}")
                            socket.getOutputStream().let { os ->
                                val msg = "I am phone(${Build.BRAND})：${Date()}\n"
                                WJLog.d("Sent：$msg")
                                os.write(msg.toByteArray())
                                os.flush()
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }


            }
        }
    }

    private fun listenServer(socket: Socket) {
        WJLog.d("listenServer")
        lifecycleScope.launch(Dispatchers.IO) {
            socket.getInputStream().use { intputStream ->
                InputStreamReader(intputStream).use { isr ->
                    BufferedReader(isr).use { reader ->
                        while (socket.isConnected && !socket.isClosed) {
                            var line: String? = null

                            do {
                                line = reader.readLine()

                                WJLog.d("接收到数据：$line")

                                if (line == "EOF") {
                                    WJLog.d("客户端请求关闭socket")
                                    break
                                }
                            } while (line != null)
                        }
                    }
                }

            }
        }
    }

    override fun getLayoutId(): Int {
        return R.layout.fragment_net_test
    }

    override fun onDestroy() {
        super.onDestroy()
        socket?.close()
    }
}