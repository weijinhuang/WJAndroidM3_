package com.wj.androidm3.business.rtc

import android.content.Context
import org.webrtc.AudioSource
import org.webrtc.AudioTrack
import org.webrtc.Camera1Enumerator
import org.webrtc.Camera2Enumerator
import org.webrtc.CameraEnumerator
import org.webrtc.DataChannel
import org.webrtc.DefaultVideoDecoderFactory
import org.webrtc.DefaultVideoEncoderFactory
import org.webrtc.EglBase
import org.webrtc.IceCandidate
import org.webrtc.MediaConstraints
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import org.webrtc.RendererCommon
import org.webrtc.RtpReceiver
import org.webrtc.SdpObserver
import org.webrtc.SessionDescription
import org.webrtc.SurfaceTextureHelper
import org.webrtc.SurfaceViewRenderer
import org.webrtc.VideoCapturer
import org.webrtc.VideoSource
import org.webrtc.VideoTrack
import org.webrtc.audio.JavaAudioDeviceModule

/**
 * WebRTC 通话内核。
 *
 * 这个类把音视频通话拆成几件事：
 * 1. 初始化 WebRTC 工厂和 EGL 环境。
 * 2. 打开本机摄像头/麦克风，生成 local audio/video track。
 * 3. 创建 PeerConnection，并把本机 track 加进去。
 * 4. 生成/接收 SDP offer-answer。
 * 5. 生成/接收 ICE Candidate。
 * 6. 把本地/远端视频 track 绑定到两个 SurfaceViewRenderer。
 *
 * 注意：这里没有自己写 H.264 编码器、RTP 分包、jitter buffer 或解码器。
 * 这些实时音视频细节都由 WebRTC 处理，我们只负责把“协商结果”和“候选地址”通过局域网信令发给对方。
 */
class RtcCallManager(
    private val context: Context,
    private val listener: Listener
) {
    interface Listener {
        // 用于把 WebRTC 状态反馈给 Activity 的状态栏。
        fun onStatus(message: String)

        // WebRTC 发现新的本机网络候选地址时，通过 Activity 的 TCP 信令发给对方。
        fun onLocalIceCandidate(wCandidate: IceCandidate)
        fun onConnected()
        fun onDisconnected()
        fun onError(message: String, throwable: Throwable? = null)
    }

    private val appContext = context.applicationContext

    // SurfaceViewRenderer 和摄像头采集都依赖 EGL/OpenGL 环境。
    // 两个渲染窗口共享同一个 wEglContext，避免创建多套 GL 上下文带来的资源浪费。
    private val wEglBase: EglBase = EglBase.create()
    val wEglContext: EglBase.Context = wEglBase.eglBaseContext

    // PeerConnectionFactory 是 WebRTC 创建音视频源、track、PeerConnection 的入口。
    private var wPeerConnectionFactory: PeerConnectionFactory? = null

    // PeerConnection 是真正的 P2P 连接对象，内部负责 ICE、DTLS、SRTP、RTP/RTCP。
    private var wPeerConnection: PeerConnection? = null

    // 摄像头帧会先进入 SurfaceTextureHelper，再交给 WebRTC VideoSource。
    private var wSurfaceTextureHelper: SurfaceTextureHelper? = null
    private var wVideoCapturer: VideoCapturer? = null
    private var wVideoSource: VideoSource? = null
    private var wAudioSource: AudioSource? = null

    // localTrack 是己方采集到的音视频；wRemoteVideoTrack 是对方通过网络发来的画面。
    private var wLocalVideoTrack: VideoTrack? = null
    private var wLocalAudioTrack: AudioTrack? = null
    private var wRemoteVideoTrack: VideoTrack? = null

    // wPrimaryRenderer 对应大画面 A，wSecondaryRenderer 对应小画面 B。
    private var wPrimaryRenderer: SurfaceViewRenderer? = null
    private var wSecondaryRenderer: SurfaceViewRenderer? = null
    private var renderersInitialized = false
    private var swapped = false

    // ICE Candidate 必须在 setRemoteDescription 成功后再加入。
    // 如果信令先到了 ICE、后到 SDP，就先缓存起来，等远端 SDP 设置完成再 drain。
    private var remoteDescriptionSet = false
    private val wPendingRemoteCandidates = mutableListOf<IceCandidate>()

    fun initRenderers(wPrimary: SurfaceViewRenderer, wSecondary: SurfaceViewRenderer) {
        if (renderersInitialized) {
            return
        }
        wPrimaryRenderer = wPrimary
        wSecondaryRenderer = wSecondary
        listOf(wPrimary, wSecondary).forEach { wRenderer ->
            wRenderer.init(wEglContext, null)
            // 硬件缩放减少 CPU 参与，视频渲染更顺滑。
            wRenderer.setEnableHardwareScaler(true)
            // 用填充模式让画面铺满容器，类似视频通话常见的 centerCrop 效果。
            wRenderer.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL)
        }
        renderersInitialized = true
        applyRendererBindings()
    }

    fun startLocalMedia() {
        // 重复点击接听/呼叫时避免重复打开摄像头和麦克风。
        if (wLocalVideoTrack != null && wLocalAudioTrack != null && wPeerConnection != null) {
            return
        }
        ensureFactory()
        ensurePeerConnection()
        val wFactory = wPeerConnectionFactory ?: return
        val wCapturer = createVideoCapturer()
        wVideoCapturer = wCapturer
        wSurfaceTextureHelper = SurfaceTextureHelper.create("LanVideoCaptureThread", wEglContext)

        // VideoSource 接收摄像头帧，VideoTrack 则是可以被 PeerConnection 发送的媒体轨道。
        wVideoSource = wFactory.createVideoSource(wCapturer.isScreencast)
        wCapturer.initialize(wSurfaceTextureHelper, appContext, wVideoSource?.capturerObserver)
        // 640x480@15fps 对局域网视频聊天足够清晰，码率/CPU 压力也比较低，适合作为新手版默认值。
        wCapturer.startCapture(VIDEO_WIDTH, VIDEO_HEIGHT, VIDEO_FPS)
        wLocalVideoTrack = wFactory.createVideoTrack(LOCAL_VIDEO_TRACK_ID, wVideoSource).apply {
            setEnabled(true)
        }

        // 音频采集、回声消除、降噪由 WebRTC 的 AudioDeviceModule/AudioSource 处理。
        wAudioSource = wFactory.createAudioSource(MediaConstraints())
        wLocalAudioTrack = wFactory.createAudioTrack(LOCAL_AUDIO_TRACK_ID, wAudioSource).apply {
            setEnabled(true)
        }

        // 旧版 google-webrtc 使用 addStream 最稳定；同一个 stream 里放本地音频和视频。
        val wStream = wFactory.createLocalMediaStream(LOCAL_STREAM_ID).apply {
            addTrack(wLocalVideoTrack)
            addTrack(wLocalAudioTrack)
        }
        wPeerConnection?.addStream(wStream)
        applyRendererBindings()
        listener.onStatus("Local media started")
    }

    fun createOffer(onOffer: (SessionDescription) -> Unit) {
        val wPc = wPeerConnection ?: return
        // 呼叫方创建 offer：告诉接收方“我能发送/接收什么媒体、支持哪些编解码器”。
        wPc.createOffer(object : SimpleSdpObserver() {
            override fun onCreateSuccess(wDescription: SessionDescription) {
                // 先 setLocalDescription，再通过信令发给对方。
                // 这样 PeerConnection 内部状态和发出的 SDP 保持一致。
                setLocalDescription(wDescription, onOffer)
            }

            override fun onCreateFailure(error: String) {
                listener.onError("Create offer failed: $error")
            }
        }, offerAnswerConstraints())
    }

    fun handleRemoteOffer(sdp: String, onAnswer: (SessionDescription) -> Unit) {
        ensureFactory()
        ensurePeerConnection()
        // 接收方先设置远端 offer，再基于这个 offer 创建 answer。
        setRemoteDescription(SessionDescription(SessionDescription.Type.OFFER, sdp)) {
            val wPc = wPeerConnection ?: return@setRemoteDescription
            wPc.createAnswer(object : SimpleSdpObserver() {
                override fun onCreateSuccess(wDescription: SessionDescription) {
                    // answer 同样需要先成为本地描述，再发回呼叫方。
                    setLocalDescription(wDescription, onAnswer)
                }

                override fun onCreateFailure(error: String) {
                    listener.onError("Create answer failed: $error")
                }
            }, offerAnswerConstraints())
        }
    }

    fun handleRemoteAnswer(sdp: String) {
        // 呼叫方收到 answer 后，双方的媒体能力协商才算闭环。
        setRemoteDescription(SessionDescription(SessionDescription.Type.ANSWER, sdp), onSet = null)
    }

    fun addRemoteIceCandidate(wCandidate: IceCandidate) {
        // ICE 可能比 SDP 更早通过信令到达；未设置远端 SDP 前 addIceCandidate 会失败。
        if (remoteDescriptionSet) {
            wPeerConnection?.addIceCandidate(wCandidate)
        } else {
            wPendingRemoteCandidates.add(wCandidate)
        }
    }

    fun setSwapped(value: Boolean) {
        // A/B 互换并不是重新建立通话，只是把 local/remote track 重新绑定到不同 renderer。
        swapped = value
        applyRendererBindings()
    }

    fun releaseRenderers() {
        removeRendererBindings()
        wPrimaryRenderer?.release()
        wSecondaryRenderer?.release()
        wPrimaryRenderer = null
        wSecondaryRenderer = null
        renderersInitialized = false
    }

    fun dispose() {
        // 释放顺序很重要：先解绑渲染，再关闭 PeerConnection，再停采集和释放 source/track。
        // 这样可以避免摄像头线程还在推帧时 renderer 或 PeerConnection 已经被销毁。
        removeRendererBindings()
        wPendingRemoteCandidates.clear()
        remoteDescriptionSet = false
        wRemoteVideoTrack = null
        wPeerConnection?.close()
        wPeerConnection?.dispose()
        wPeerConnection = null
        try {
            wVideoCapturer?.stopCapture()
        } catch (ignored: InterruptedException) {
            Thread.currentThread().interrupt()
        } catch (ignored: Throwable) {
        }
        wVideoCapturer?.dispose()
        wVideoCapturer = null
        wSurfaceTextureHelper?.dispose()
        wSurfaceTextureHelper = null
        wLocalVideoTrack?.dispose()
        wLocalVideoTrack = null
        wLocalAudioTrack?.dispose()
        wLocalAudioTrack = null
        wVideoSource?.dispose()
        wVideoSource = null
        wAudioSource?.dispose()
        wAudioSource = null
        wPeerConnectionFactory?.dispose()
        wPeerConnectionFactory = null
    }

    fun release() {
        dispose()
        releaseRenderers()
        wEglBase.release()
    }

    private fun ensureFactory() {
        if (wPeerConnectionFactory != null) {
            return
        }
        ensurePeerConnectionFactoryInitialized(appContext)
        // JavaAudioDeviceModule 让 WebRTC 接管 Android 音频设备，并启用硬件 AEC/NS。
        // AEC 是 Acoustic Echo Canceler，减少免提时“对方声音又被麦克风采回去”的回声。
        val wAudioDeviceModule = JavaAudioDeviceModule.builder(appContext)
            .setUseHardwareAcousticEchoCanceler(true)
            .setUseHardwareNoiseSuppressor(true)
            .createAudioDeviceModule()

        // Encoder/DecoderFactory 决定视频编解码能力。
        // 第二个参数 true 表示允许 H.264 High Profile；第三个 true 表示允许硬件编码。
        val wEncoderFactory = DefaultVideoEncoderFactory(wEglContext, true, true)
        val wDecoderFactory = DefaultVideoDecoderFactory(wEglContext)
        wPeerConnectionFactory = PeerConnectionFactory.builder()
            .setAudioDeviceModule(wAudioDeviceModule)
            .setVideoEncoderFactory(wEncoderFactory)
            .setVideoDecoderFactory(wDecoderFactory)
            .createPeerConnectionFactory()
        wAudioDeviceModule.release()
    }

    private fun ensurePeerConnection() {
        if (wPeerConnection != null) {
            return
        }
        // 局域网点对点通话不配置 STUN/TURN，WebRTC 会使用 host candidate 尝试同网段直连。
        val wRtcConfig = PeerConnection.RTCConfiguration(emptyList()).apply {
            iceTransportsType = PeerConnection.IceTransportsType.ALL
            // 只建一个 bundle 传输通道承载音频/视频，减少连接数量。
            bundlePolicy = PeerConnection.BundlePolicy.MAXBUNDLE
            // RTCP 与 RTP 复用端口，是现代 WebRTC 的常见配置。
            rtcpMuxPolicy = PeerConnection.RtcpMuxPolicy.REQUIRE
            // 网络变化时继续收集候选地址，Wi-Fi 切换等场景更有机会恢复。
            continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY
        }
        wPeerConnection = wPeerConnectionFactory?.createPeerConnection(wRtcConfig, wPeerObserver)
    }

    private fun setLocalDescription(
        wDescription: SessionDescription,
        onSet: (SessionDescription) -> Unit
    ) {
        wPeerConnection?.setLocalDescription(object : SimpleSdpObserver() {
            override fun onSetSuccess() {
                // setLocalDescription 成功后再通知 Activity 发送 SDP，避免“发出去了但本地状态没设置上”。
                onSet(wDescription)
            }

            override fun onSetFailure(error: String) {
                listener.onError("Set local description failed: $error")
            }
        }, wDescription)
    }

    private fun setRemoteDescription(
        wDescription: SessionDescription,
        onSet: (() -> Unit)?
    ) {
        wPeerConnection?.setRemoteDescription(object : SimpleSdpObserver() {
            override fun onSetSuccess() {
                remoteDescriptionSet = true
                // 远端 SDP 设置完成后，把之前缓存的 ICE Candidate 一次性加入 PeerConnection。
                drainPendingCandidates()
                onSet?.invoke()
            }

            override fun onSetFailure(error: String) {
                listener.onError("Set remote description failed: $error")
            }
        }, wDescription)
    }

    private fun drainPendingCandidates() {
        val wPc = wPeerConnection ?: return
        wPendingRemoteCandidates.forEach { wPc.addIceCandidate(it) }
        wPendingRemoteCandidates.clear()
    }

    private fun createVideoCapturer(): VideoCapturer {
        // 优先 Camera2，失败再回退 Camera1，保证旧设备也尽量可用。
        val wEnumerator: CameraEnumerator = if (Camera2Enumerator.isSupported(appContext)) {
            Camera2Enumerator(appContext)
        } else {
            Camera1Enumerator(true)
        }
        // 视频聊天默认用前置摄像头；没有前置摄像头时才使用任意可用摄像头。
        wEnumerator.deviceNames.firstOrNull { wEnumerator.isFrontFacing(it) }?.let { deviceName ->
            wEnumerator.createCapturer(deviceName, null)?.let { return it }
        }
        wEnumerator.deviceNames.firstOrNull()?.let { deviceName ->
            wEnumerator.createCapturer(deviceName, null)?.let { return it }
        }
        throw IllegalStateException("No camera capturer available")
    }

    private fun offerAnswerConstraints(): MediaConstraints {
        return MediaConstraints().apply {
            // 明确告诉 WebRTC：双方都希望收音频和视频。
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))
        }
    }

    private fun applyRendererBindings() {
        removeRendererBindings()
        val wPrimary = wPrimaryRenderer
        val wSecondary = wSecondaryRenderer
        if (wPrimary == null || wSecondary == null) {
            return
        }
        if (swapped) {
            // swapped=true 时，大画面显示自己，小画面显示对方。
            wLocalVideoTrack?.addSink(wPrimary)
            wRemoteVideoTrack?.addSink(wSecondary)
            wPrimary.setMirror(true)
            wSecondary.setMirror(false)
        } else {
            // 默认通话 UI：大画面 A 显示对方，小画面 B 显示自己。
            wRemoteVideoTrack?.addSink(wPrimary)
            wLocalVideoTrack?.addSink(wSecondary)
            wPrimary.setMirror(false)
            wSecondary.setMirror(true)
        }
    }

    private fun removeRendererBindings() {
        wPrimaryRenderer?.let { wRenderer ->
            wLocalVideoTrack?.removeSink(wRenderer)
            wRemoteVideoTrack?.removeSink(wRenderer)
        }
        wSecondaryRenderer?.let { wRenderer ->
            wLocalVideoTrack?.removeSink(wRenderer)
            wRemoteVideoTrack?.removeSink(wRenderer)
        }
    }

    private val wPeerObserver = object : PeerConnection.Observer {
        override fun onSignalingChange(wNewState: PeerConnection.SignalingState) {
            // signaling state 反映 offer/answer 状态机变化，主要用于调试。
            listener.onStatus("Signaling: $wNewState")
        }

        override fun onIceConnectionChange(wNewState: PeerConnection.IceConnectionState) {
            // ICE state 反映 P2P 网络是否真正连通；CONNECTED 后才能稳定收发媒体。
            listener.onStatus("ICE: $wNewState")
            when (wNewState) {
                PeerConnection.IceConnectionState.CONNECTED,
                PeerConnection.IceConnectionState.COMPLETED -> listener.onConnected()
                PeerConnection.IceConnectionState.DISCONNECTED,
                PeerConnection.IceConnectionState.FAILED,
                PeerConnection.IceConnectionState.CLOSED -> listener.onDisconnected()
                else -> Unit
            }
        }

        override fun onIceConnectionReceivingChange(receiving: Boolean) = Unit

        override fun onIceGatheringChange(wNewState: PeerConnection.IceGatheringState) {
            listener.onStatus("ICE gathering: $wNewState")
        }

        override fun onIceCandidate(wCandidate: IceCandidate) {
            // 本机发现候选地址后必须发给对方，否则对方不知道该尝试连哪个地址/端口。
            listener.onLocalIceCandidate(wCandidate)
        }

        override fun onIceCandidatesRemoved(wCandidates: Array<out IceCandidate>) = Unit

        override fun onAddStream(wStream: MediaStream) {
            // 远端媒体流到达后，从里面取出视频 track，绑定到 renderer 才能看到对方画面。
            wRemoteVideoTrack = wStream.videoTracks.firstOrNull()
            wRemoteVideoTrack?.setEnabled(true)
            applyRendererBindings()
            listener.onStatus("Remote media received")
        }

        override fun onRemoveStream(wStream: MediaStream) {
            if (wRemoteVideoTrack != null && wStream.videoTracks.contains(wRemoteVideoTrack)) {
                removeRendererBindings()
                wRemoteVideoTrack = null
            }
        }

        override fun onDataChannel(wDataChannel: DataChannel) = Unit

        override fun onRenegotiationNeeded() = Unit

        override fun onAddTrack(wReceiver: RtpReceiver, wMediaStreams: Array<out MediaStream>) {
            // 某些 WebRTC 版本/协商模式会走 onAddTrack 而不是 onAddStream，这里兼容一下。
            val wTrack = wReceiver.track()
            if (wTrack is VideoTrack) {
                wRemoteVideoTrack = wTrack
                wRemoteVideoTrack?.setEnabled(true)
                applyRendererBindings()
                listener.onStatus("Remote video track added")
            }
        }
    }

    private open class SimpleSdpObserver : SdpObserver {
        override fun onCreateSuccess(wDescription: SessionDescription) = Unit
        override fun onSetSuccess() = Unit
        override fun onCreateFailure(error: String) = Unit
        override fun onSetFailure(error: String) = Unit
    }

    companion object {
        private const val LOCAL_STREAM_ID = "lan_stream"
        private const val LOCAL_VIDEO_TRACK_ID = "lan_video"
        private const val LOCAL_AUDIO_TRACK_ID = "lan_audio"
        private const val VIDEO_WIDTH = 640
        private const val VIDEO_HEIGHT = 480
        private const val VIDEO_FPS = 15

        @Volatile
        private var factoryInitialized = false

        private fun ensurePeerConnectionFactoryInitialized(context: Context) {
            // WebRTC 全局初始化整个进程只需要做一次；重复初始化没有意义，也可能浪费资源。
            if (factoryInitialized) {
                return
            }
            synchronized(RtcCallManager::class.java) {
                if (!factoryInitialized) {
                    PeerConnectionFactory.initialize(
                        PeerConnectionFactory.InitializationOptions.builder(context)
                            .createInitializationOptions()
                    )
                    factoryInitialized = true
                }
            }
        }
    }
}
