package com.wj.androidm3.business.ui.lanvideo

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.wj.androidm3.R
import com.wj.androidm3.business.lan.LanSignalEnvelope
import com.wj.androidm3.business.lan.LanSignalMessage
import com.wj.androidm3.business.lan.LanSignalingServer
import com.wj.androidm3.business.rtc.RtcCallManager
import com.wj.androidm3.databinding.ActivityLanVideoCallBinding
import com.wj.basecomponent.ui.BaseMVVMActivity
import com.wj.basecomponent.util.log.WJLog
import com.wj.basecomponent.vm.BaseViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.webrtc.IceCandidate
import org.webrtc.SessionDescription
import java.net.Inet4Address
import java.net.NetworkInterface
import java.util.Locale
import java.util.UUID

/**
 * 局域网视频通话页面。
 *
 * 这个页面做“业务编排”，不直接处理视频帧：
 * 1. 显示本机 IP 和对端 IP 输入框。
 * 2. 申请 CAMERA/RECORD_AUDIO 权限。
 * 3. 启动 TCP 信令监听，接收对方的呼叫请求。
 * 4. 根据 call_request/accept/offer/answer/ice 等消息驱动 WebRTC。
 * 5. 管理挂断、页面销毁、扬声器模式和 A/B 画面互换。
 *
 * 音视频采集、编码、传输、解码都在 RtcCallManager/WebRTC 内部完成。
 */
class LanVideoCallActivity : BaseMVVMActivity<BaseViewModel, ActivityLanVideoCallBinding>() {
    private val binding: ActivityLanVideoCallBinding
        get() = requireNotNull(mViewBinding)
    private lateinit var rtcCallManager: RtcCallManager

    // 两台手机进入这个页面后都会监听 39400 端口；谁输入对方 IP，谁先发 call_request。
    private val signalingServer = LanSignalingServer()

    // 当前通话 ID。所有信令都带这个值，避免旧消息或其他设备消息误操作当前页面。
    private var sessionId: String? = null

    // 对方 IP 来自用户输入或 TCP 连接来源，后续发送 accept/offer/answer/ice 都用它。
    private var remoteIp: String? = null

    // 摄像头和麦克风只需要打开一次，重复 offer/answer 时不再重复启动采集。
    private var localMediaStarted = false

    // 页面级通话状态：呼叫中、已接听、媒体连接中都算 inCall，避免再接第二通。
    private var inCall = false

    // false: A=对方，B=自己；true: A=自己，B=对方。
    private var swapped = false

    // 通话时切到 MODE_IN_COMMUNICATION + 外放；挂断后恢复用户之前的音频状态。
    private var previousAudioMode = AudioManager.MODE_NORMAL
    private var previousSpeakerphone = false

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        // 视频聊天必须同时有摄像头和麦克风权限，缺任意一个都无法完整通话。
        val granted = REQUIRED_PERMISSIONS.all { result[it] == true || hasPermission(it) }
        setStatus(if (granted) "Ready" else "Camera and microphone permissions are required")
        binding.callButton.isEnabled = granted
    }

    override fun getLayoutId(): Int {
        return R.layout.activity_lan_video_call
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 通话时保持屏幕常亮，避免锁屏导致摄像头/渲染暂停。
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        rtcCallManager = RtcCallManager(this, rtcListener)
        rtcCallManager.initRenderers(binding.wPrimaryRenderer, binding.wSecondaryRenderer)

        // 显示本机 IP，方便另一台设备输入。
        binding.localIpText.text = "Local IP: ${findLocalIpv4Address() ?: "unknown"}  Port: ${LanSignalingServer.DEFAULT_PORT}"
        binding.callButton.isEnabled = hasRequiredPermissions()
        binding.callButton.setOnClickListener { startOutgoingCall() }
        binding.hangupButton.setOnClickListener { endCall(notifyPeer = true, status = "Call ended") }
        binding.secondaryVideoContainer.setOnClickListener { toggleVideoSwap() }
        startSignalingServer()
        if (!hasRequiredPermissions()) {
            // 进入页面就提前申请权限，避免接到来电时才发现无法打开摄像头。
            permissionLauncher.launch(REQUIRED_PERMISSIONS)
        }
    }

    override fun onDestroy() {
        // 页面退出即释放通话资源。本版是前台页面通话，不做后台来电/后台持续通话。
        endCall(notifyPeer = false, status = "Closed")
        rtcCallManager.release()
        signalingServer.close()
        super.onDestroy()
    }

    private fun startSignalingServer() {
        signalingServer.start(
            scope = lifecycleScope,
            onMessage = { envelope ->
                // socket 线程收到消息后切回主线程，因为后续可能更新 UI 或弹窗。
                lifecycleScope.launch(Dispatchers.Main) {
                    handleSignal(envelope)
                }
            },
            onError = { throwable ->
                WJLog.e("LAN signaling error: ${throwable.message}")
                lifecycleScope.launch(Dispatchers.Main) {
                    setStatus("Signaling error: ${throwable.message ?: throwable.javaClass.simpleName}")
                }
            }
        )
    }

    private fun startOutgoingCall() {
        if (!hasRequiredPermissions()) {
            permissionLauncher.launch(REQUIRED_PERMISSIONS)
            return
        }
        val ip = binding.peerIpEdit.text?.toString()?.trim().orEmpty()
        if (!isValidIpv4(ip)) {
            setStatus("Please enter a valid LAN IPv4 address")
            return
        }
        if (inCall) {
            setStatus("Already in a call")
            return
        }
        // 呼叫方先生成 sessionId，后续所有 SDP/ICE 都归属这一次通话。
        val newSessionId = UUID.randomUUID().toString()
        sessionId = newSessionId
        remoteIp = ip
        inCall = true
        binding.callButton.isEnabled = false
        binding.hangupButton.isEnabled = true
        setStatus("Calling $ip...")
        sendSignal(
            ip,
            LanSignalMessage(
                type = LanSignalMessage.TYPE_CALL_REQUEST,
                sessionId = newSessionId,
                displayName = Build.MODEL ?: "Android"
            ),
            rollbackOnFailure = true
        )
    }

    private fun handleSignal(envelope: LanSignalEnvelope) {
        val message = envelope.message
        // 所有信令统一在这里按 type 分发，Activity 相当于一个小型通话状态机。
        when (message.type) {
            LanSignalMessage.TYPE_CALL_REQUEST -> handleCallRequest(envelope)
            LanSignalMessage.TYPE_CALL_ACCEPT -> handleCallAccept(envelope)
            LanSignalMessage.TYPE_CALL_REJECT -> handleCallRejected(envelope, message.reason ?: "Rejected")
            LanSignalMessage.TYPE_BUSY -> handleCallRejected(envelope, "Peer is busy")
            LanSignalMessage.TYPE_OFFER -> handleOffer(envelope)
            LanSignalMessage.TYPE_ANSWER -> handleAnswer(envelope)
            LanSignalMessage.TYPE_ICE -> handleIce(envelope)
            LanSignalMessage.TYPE_BYE -> handleBye(envelope)
            LanSignalMessage.TYPE_ERROR -> setStatus("Peer error: ${message.reason ?: "unknown"}")
        }
    }

    private fun handleCallRequest(envelope: LanSignalEnvelope) {
        val message = envelope.message
        if (inCall || sessionId != null) {
            // 已经在通话/呼叫中时，直接返回 busy，避免两个 PeerConnection 抢摄像头和麦克风。
            sendSignal(
                envelope.remoteIp,
                LanSignalMessage(
                    type = LanSignalMessage.TYPE_BUSY,
                    sessionId = message.sessionId,
                    reason = "Busy"
                )
            )
            return
        }
        val peerName = message.displayName ?: envelope.remoteIp
        // 收到请求不自动接听，必须让用户明确接受后才打开摄像头和麦克风。
        MaterialAlertDialogBuilder(this)
            .setTitle("Incoming LAN call")
            .setMessage("$peerName wants to start a video chat from ${envelope.remoteIp}.")
            .setNegativeButton("Reject") { dialog, _ ->
                dialog.dismiss()
                sendSignal(
                    envelope.remoteIp,
                    LanSignalMessage(
                        type = LanSignalMessage.TYPE_CALL_REJECT,
                        sessionId = message.sessionId,
                        reason = "Rejected"
                    )
                )
            }
            .setPositiveButton("Accept") { dialog, _ ->
                dialog.dismiss()
                acceptIncomingCall(envelope)
            }
            .show()
    }

    private fun acceptIncomingCall(envelope: LanSignalEnvelope) {
        if (!hasRequiredPermissions()) {
            // 没有权限时不能接听，否则对方会进入连接流程但本机无法发送音视频。
            permissionLauncher.launch(REQUIRED_PERMISSIONS)
            sendSignal(
                envelope.remoteIp,
                LanSignalMessage(
                    type = LanSignalMessage.TYPE_CALL_REJECT,
                    sessionId = envelope.message.sessionId,
                    reason = "Camera or microphone permission missing"
                )
            )
            return
        }
        sessionId = envelope.message.sessionId
        remoteIp = envelope.remoteIp
        inCall = true
        binding.callButton.isEnabled = false
        binding.hangupButton.isEnabled = true
        binding.peerIpEdit.setText(envelope.remoteIp)
        if (!prepareLocalMedia()) {
            // 本地采集启动失败要通知对方，避免对方一直等待 offer/answer。
            sendSignal(
                envelope.remoteIp,
                LanSignalMessage(
                    type = LanSignalMessage.TYPE_ERROR,
                    sessionId = envelope.message.sessionId,
                    reason = "Failed to start local media"
                )
            )
            endCall(notifyPeer = false, status = "Failed to start local media")
            return
        }
        setStatus("Accepted call from ${envelope.remoteIp}; waiting for offer")
        sendSignal(
            envelope.remoteIp,
            LanSignalMessage(
                type = LanSignalMessage.TYPE_CALL_ACCEPT,
                sessionId = envelope.message.sessionId,
                displayName = Build.MODEL ?: "Android"
            )
        )
    }

    private fun handleCallAccept(envelope: LanSignalEnvelope) {
        if (!isCurrentSession(envelope.message)) {
            return
        }
        remoteIp = envelope.remoteIp
        if (!prepareLocalMedia()) {
            endCall(notifyPeer = true, status = "Failed to start local media")
            return
        }
        // 呼叫方在对方接受后才创建 offer，这样对方明确同意前不会启动媒体协商。
        setStatus("Peer accepted; creating offer")
        rtcCallManager.createOffer { wDescription ->
            sendSdp(LanSignalMessage.TYPE_OFFER, wDescription)
        }
    }

    private fun handleCallRejected(envelope: LanSignalEnvelope, reason: String) {
        if (!isCurrentSession(envelope.message)) {
            return
        }
        endCall(notifyPeer = false, status = reason)
    }

    private fun handleOffer(envelope: LanSignalEnvelope) {
        if (!isCurrentSession(envelope.message)) {
            return
        }
        remoteIp = envelope.remoteIp
        if (!prepareLocalMedia()) {
            endCall(notifyPeer = true, status = "Failed to start local media")
            return
        }
        val sdp = envelope.message.sdp ?: return
        // 接收方收到 offer 后设置远端描述，再创建 answer 回给呼叫方。
        setStatus("Offer received; creating answer")
        rtcCallManager.handleRemoteOffer(sdp) { wAnswer ->
            sendSdp(LanSignalMessage.TYPE_ANSWER, wAnswer)
        }
    }

    private fun handleAnswer(envelope: LanSignalEnvelope) {
        if (!isCurrentSession(envelope.message)) {
            return
        }
        envelope.message.sdp?.let { sdp ->
            // 呼叫方收到 answer 后，SDP 协商完成；接下来靠 ICE 让网络真正连通。
            rtcCallManager.handleRemoteAnswer(sdp)
            setStatus("Answer received; connecting media")
        }
    }

    private fun handleIce(envelope: LanSignalEnvelope) {
        if (!isCurrentSession(envelope.message)) {
            return
        }
        val candidate = envelope.message.candidate ?: return
        // ICE Candidate 是“对方可能可达的地址/端口”。加入后 WebRTC 会自己尝试连通。
        rtcCallManager.addRemoteIceCandidate(
            IceCandidate(
                envelope.message.sdpMid ?: "",
                envelope.message.sdpMLineIndex ?: 0,
                candidate
            )
        )
    }

    private fun handleBye(envelope: LanSignalEnvelope) {
        if (sessionId == envelope.message.sessionId) {
            endCall(notifyPeer = false, status = "Peer hung up")
        }
    }

    private fun prepareLocalMedia(): Boolean {
        if (localMediaStarted) {
            return true
        }
        return try {
            // 这里会真正打开摄像头和麦克风，并把本地 track 加入 PeerConnection。
            rtcCallManager.startLocalMedia()
            localMediaStarted = true
            setAudioModeForCall(true)
            true
        } catch (t: Throwable) {
            WJLog.e("Start local media failed: ${t.message}")
            setStatus("Start local media failed: ${t.message ?: t.javaClass.simpleName}")
            false
        }
    }

    private fun sendSdp(type: String, wDescription: SessionDescription) {
        val ip = remoteIp ?: return
        val currentSessionId = sessionId ?: return
        // SDP 是纯文本，可能包含多行和大量参数，放进 JSON 后通过 TCP 信令发送。
        sendSignal(
            ip,
            LanSignalMessage(
                type = type,
                sessionId = currentSessionId,
                sdpType = wDescription.type.name.lowercase(Locale.US),
                sdp = wDescription.description
            )
        )
    }

    private fun sendSignal(
        host: String,
        message: LanSignalMessage,
        rollbackOnFailure: Boolean = false
    ) {
        lifecycleScope.launch {
            try {
                signalingServer.send(host, message)
            } catch (t: Throwable) {
                WJLog.e("Send signal failed: ${t.message}")
                setStatus("Send failed: ${t.message ?: t.javaClass.simpleName}")
                if (rollbackOnFailure) {
                    // 发起呼叫时连不上对方，直接回到空闲状态。
                    endCall(notifyPeer = false, status = "Unable to reach $host")
                }
            }
        }
    }

    private fun endCall(notifyPeer: Boolean, status: String) {
        val ip = remoteIp
        val currentSessionId = sessionId
        if (notifyPeer && ip != null && currentSessionId != null) {
            // 主动挂断时给对方发 bye，让对方也释放摄像头、麦克风和 PeerConnection。
            sendSignal(
                ip,
                LanSignalMessage(
                    type = LanSignalMessage.TYPE_BYE,
                    sessionId = currentSessionId
                )
            )
        }
        // dispose 只释放音视频/PeerConnection，信令监听仍保留，用户可以继续接下一通。
        rtcCallManager.dispose()
        localMediaStarted = false
        inCall = false
        sessionId = null
        remoteIp = null
        swapped = false
        setAudioModeForCall(false)
        binding.primaryLabel.text = "Remote"
        binding.secondaryLabel.text = "Local"
        binding.callButton.isEnabled = hasRequiredPermissions()
        binding.hangupButton.isEnabled = false
        setStatus(status)
    }

    private fun toggleVideoSwap() {
        swapped = !swapped
        // 这里只切换渲染绑定，不影响 WebRTC 连接，也不重新协商 SDP。
        rtcCallManager.setSwapped(swapped)
        binding.primaryLabel.text = if (swapped) "Local" else "Remote"
        binding.secondaryLabel.text = if (swapped) "Remote" else "Local"
    }

    private fun isCurrentSession(message: LanSignalMessage): Boolean {
        // 过滤掉非当前通话的拒绝、ICE、bye 等消息，避免误伤当前通话。
        return sessionId == message.sessionId
    }

    private fun setStatus(text: String) {
        runOnUiThread {
            binding.statusText.text = text
        }
    }

    private fun hasRequiredPermissions(): Boolean {
        return REQUIRED_PERMISSIONS.all { hasPermission(it) }
    }

    private fun hasPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
    }

    private fun setAudioModeForCall(enabled: Boolean) {
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        if (enabled) {
            // MODE_IN_COMMUNICATION 会让系统按通话场景处理音频路由、音量和部分设备效果。
            previousAudioMode = audioManager.mode
            previousSpeakerphone = audioManager.isSpeakerphoneOn
            audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
            // 视频聊天通常默认外放；如果后续要支持听筒/蓝牙，可在这里扩展路由选择。
            audioManager.isSpeakerphoneOn = true
        } else {
            // 挂断后恢复进入通话前的音频状态，避免影响用户后续播放音乐/视频。
            audioManager.mode = previousAudioMode
            audioManager.isSpeakerphoneOn = previousSpeakerphone
        }
    }

    private val rtcListener = object : RtcCallManager.Listener {
        override fun onStatus(message: String) {
            setStatus(message)
        }

        override fun onLocalIceCandidate(wCandidate: IceCandidate) {
            val ip = remoteIp ?: return
            val currentSessionId = sessionId ?: return
            // 本机每发现一个 ICE Candidate，就立刻发给对方，越早交换越快连通。
            sendSignal(
                ip,
                LanSignalMessage(
                    type = LanSignalMessage.TYPE_ICE,
                    sessionId = currentSessionId,
                    candidate = wCandidate.sdp,
                    sdpMid = wCandidate.sdpMid,
                    sdpMLineIndex = wCandidate.sdpMLineIndex
                )
            )
        }

        override fun onConnected() {
            setStatus("Connected")
        }

        override fun onDisconnected() {
            setStatus("Disconnected")
        }

        override fun onError(message: String, throwable: Throwable?) {
            WJLog.e("$message ${throwable?.message ?: ""}")
            setStatus(message)
        }
    }

    companion object {
        private val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        )

        private fun isValidIpv4(value: String): Boolean {
            // V1 明确要求“输入对方局域网 IP”，所以这里只校验 IPv4，不做域名/mDNS。
            val parts = value.split(".")
            return parts.size == 4 && parts.all { part ->
                part.toIntOrNull()?.let { it in 0..255 } == true
            }
        }

        private fun findLocalIpv4Address(): String? {
            // 用于 UI 提示，方便另一台手机输入；真正回复消息时仍以 socket 来源 IP 为准。
            return NetworkInterface.getNetworkInterfaces().toList()
                .flatMap { it.inetAddresses.toList() }
                .firstOrNull { address ->
                    address is Inet4Address && !address.isLoopbackAddress
                }?.hostAddress
        }
    }
}
