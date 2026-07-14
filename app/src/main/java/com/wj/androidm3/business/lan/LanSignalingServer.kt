package com.wj.androidm3.business.lan

import com.wj.basecomponent.util.log.WJLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException
import java.nio.charset.StandardCharsets

/**
 * 一个极简局域网信令通道。
 *
 * 这里故意不传音视频帧，只传 WebRTC 建连前后的控制消息。这样做有两个好处：
 * 1. 控制消息很少，TCP 足够简单可靠，丢了就重发/报错。
 * 2. 音视频实时性要求高，交给 WebRTC 自己通过 ICE 选择 UDP/SRTP 路径。
 *
 * 当前实现采用“每条信令一个 TCP 短连接”：发送方连上对方端口，写入一帧 JSON 后关闭。
 * 对新手来说这比维护一条长连接更容易理解，也避免了长连接断线重连、心跳保活等额外问题。
 */
class LanSignalingServer(
    private val port: Int = DEFAULT_PORT
) {
    private var serverSocket: ServerSocket? = null
    private var serverJob: Job? = null

    fun start(
        scope: CoroutineScope,
        onMessage: (LanSignalEnvelope) -> Unit,
        onError: (Throwable) -> Unit
    ) {
        // Activity 进入页面后会启动监听。重复调用时直接返回，避免端口被自己重复绑定。
        if (serverJob != null) {
            return
        }
        serverJob = scope.launch(Dispatchers.IO) {
            try {
                ServerSocket().use { server ->
                    // reuseAddress 让页面快速关闭再打开时更容易重新绑定端口。
                    server.reuseAddress = true
                    server.bind(InetSocketAddress(port))
                    serverSocket = server
                    WJLog.d("LAN signaling server started on port $port")
                    while (isActive) {
                        // accept() 是阻塞调用，必须放在 IO 线程。
                        // 每个 socket 只读一条消息，读完自动关闭。
                        val socket = server.accept()
                        launch {
                            readOneMessage(socket, onMessage, onError)
                        }
                    }
                }
            } catch (e: SocketException) {
                if (isActive) {
                    onError(e)
                }
            } catch (t: Throwable) {
                if (isActive) {
                    onError(t)
                }
            } finally {
                serverSocket = null
            }
        }
    }

    suspend fun send(host: String, message: LanSignalMessage) {
        withContext(Dispatchers.IO) {
            Socket().use { socket ->
                // 信令消息很小，希望尽快发出；关闭 Nagle 可以减少小包等待。
                socket.tcpNoDelay = true
                socket.connect(InetSocketAddress(host, port), CONNECT_TIMEOUT_MS)
                socket.soTimeout = READ_TIMEOUT_MS
                val bytes = message.toJsonString().toByteArray(StandardCharsets.UTF_8)
                if (bytes.size > MAX_FRAME_BYTES) {
                    throw IOException("Signal frame too large: ${bytes.size}")
                }
                DataOutputStream(BufferedOutputStream(socket.getOutputStream())).use { output ->
                    // TCP 是字节流，没有“消息边界”。
                    // 先写 4 字节长度，再写 JSON 内容，接收端就知道一条消息该读多少字节。
                    output.writeInt(bytes.size)
                    output.write(bytes)
                    output.flush()
                }
            }
        }
    }

    suspend fun stop() {
        val job = serverJob
        close()
        // stop() 给需要“等监听线程完全退出”的调用方使用。
        job?.cancelAndJoin()
    }

    fun close() {
        // close() 给 Activity.onDestroy 使用：同步关闭 socket，立即让 accept() 退出。
        serverJob?.cancel()
        serverJob = null
        serverSocket?.close()
        serverSocket = null
    }

    private fun readOneMessage(
        socket: Socket,
        onMessage: (LanSignalEnvelope) -> Unit,
        onError: (Throwable) -> Unit
    ) {
        socket.use {
            try {
                it.tcpNoDelay = true
                it.soTimeout = READ_TIMEOUT_MS
                // remoteIp 来自 TCP 连接本身，后续回复 accept/answer/ice 都发回这个地址。
                val remoteIp = it.inetAddress.hostAddress ?: it.inetAddress.hostName
                val input = DataInputStream(BufferedInputStream(it.getInputStream()))
                val length = input.readInt()
                if (length <= 0 || length > MAX_FRAME_BYTES) {
                    throw IOException("Invalid signal frame length: $length")
                }
                val bytes = ByteArray(length)
                // readFully 会一直读到指定长度，解决 TCP 分片导致一次 read 读不全的问题。
                input.readFully(bytes)
                val json = String(bytes, StandardCharsets.UTF_8)
                onMessage(LanSignalEnvelope(remoteIp, LanSignalMessage.parse(json)))
            } catch (t: Throwable) {
                onError(t)
            }
        }
    }

    companion object {
        // 双方约定好的局域网信令端口。两台手机都进入页面后都会监听这个端口。
        const val DEFAULT_PORT = 39400

        // SDP 可能比较长，但仍远小于 1MB；限制长度可以防止异常输入占用太多内存。
        private const val MAX_FRAME_BYTES = 1024 * 1024
        private const val CONNECT_TIMEOUT_MS = 5000
        private const val READ_TIMEOUT_MS = 5000
    }
}
