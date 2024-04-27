package com.wj.androidm3.business.ui.net

import androidx.lifecycle.lifecycleScope
import com.wj.androidm3.R
import com.wj.androidm3.databinding.FragmentNetTestBinding
import com.wj.basecomponent.ui.BaseMVVMFragment
import com.wj.basecomponent.util.log.WJLog
import com.wj.basecomponent.vm.BaseViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.OutputStream
import java.net.Socket
import java.util.Date

class NetTestFragment : BaseMVVMFragment<BaseViewModel, FragmentNetTestBinding>() {
    var socket: Socket? = null

    private fun initSocket(callback: (Socket) -> Unit) {
        if (null == socket) {
            WJLog.d("创建socket：192.168.3.188:7891")
            socket = Socket("192.168.3.188", 7891)
        }
        callback.invoke(socket!!)
    }

    override fun firstCreateView() {
        lifecycleScope.launch(Dispatchers.IO) {
            initSocket {
                WJLog.i("tcp连接已建立")
            }
        }

        mViewBinding?.run {
            sendTcpPacket.setOnClickListener {
                lifecycleScope.launch(Dispatchers.IO) {
                    initSocket { socket ->
                        socket.getOutputStream().let { os ->
                            val msg = "来自手机的消息：${Date()}\n"
                            WJLog.d("发送消息：$msg")
                            os.write(msg.toByteArray())
                            os.flush()
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