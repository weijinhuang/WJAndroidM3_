package com.wj.androidm3.business.lan

import org.json.JSONObject

/**
 * 局域网视频通话的“信令消息”。
 *
 * 新手容易把“信令”和“音视频数据”混在一起：这里的 JSON 不承载视频帧/音频帧，
 * 只承载建连需要的控制信息，例如：
 * 1. 谁要呼叫谁：call_request/call_accept/call_reject。
 * 2. WebRTC 双方如何协商媒体能力：offer/answer，也就是 SDP。
 * 3. WebRTC 双方如何找到可连通的网络地址：ice，也就是 ICE Candidate。
 *
 * 真正的摄像头画面和麦克风声音由 WebRTC 在 PeerConnection 内部通过 UDP/SRTP 发送。
 */
data class LanSignalMessage(
    // 消息类型决定接收方应该执行哪个状态转换，例如弹出接听框、设置远端 SDP、加入 ICE。
    val type: String,

    // 一次通话一个 sessionId。局域网内可能收到旧消息或其他手机的消息，用它过滤误消息。
    val sessionId: String,

    // 只用于来电弹窗显示，让对方知道是谁发起了呼叫。
    val displayName: String? = null,

    // SDP 类型，通常是 offer 或 answer。保留这个字段方便调试和后续扩展。
    val sdpType: String? = null,

    // SDP 是 WebRTC 的媒体协商文本，里面包含音视频方向、编解码器、RTP 参数等。
    val sdp: String? = null,

    // ICE Candidate 是 WebRTC 发现到的本机网络候选地址，双方交换后才能尝试直连。
    val candidate: String? = null,
    val sdpMid: String? = null,
    val sdpMLineIndex: Int? = null,

    // 拒绝、忙线、错误等消息用 reason 给 UI 展示原因。
    val reason: String? = null
) {
    fun toJsonString(): String {
        // 使用 JSONObject 而不是手写字符串拼接，避免 SDP/Candidate 中的换行、引号破坏 JSON。
        return JSONObject().apply {
            put(KEY_TYPE, type)
            put(KEY_SESSION_ID, sessionId)
            displayName?.let { put(KEY_DISPLAY_NAME, it) }
            sdpType?.let { put(KEY_SDP_TYPE, it) }
            sdp?.let { put(KEY_SDP, it) }
            candidate?.let { put(KEY_CANDIDATE, it) }
            sdpMid?.let { put(KEY_SDP_MID, it) }
            sdpMLineIndex?.let { put(KEY_SDP_M_LINE_INDEX, it) }
            reason?.let { put(KEY_REASON, it) }
        }.toString()
    }

    companion object {
        // 呼叫控制消息：决定“是否要开始一通电话”。
        const val TYPE_CALL_REQUEST = "call_request"
        const val TYPE_CALL_ACCEPT = "call_accept"
        const val TYPE_CALL_REJECT = "call_reject"

        // WebRTC 协商消息：决定“这通电话怎么传音视频”。
        const val TYPE_OFFER = "offer"
        const val TYPE_ANSWER = "answer"
        const val TYPE_ICE = "ice"

        // 通话收尾和异常状态。
        const val TYPE_BYE = "bye"
        const val TYPE_BUSY = "busy"
        const val TYPE_ERROR = "error"

        private const val KEY_TYPE = "type"
        private const val KEY_SESSION_ID = "sessionId"
        private const val KEY_DISPLAY_NAME = "displayName"
        private const val KEY_SDP_TYPE = "sdpType"
        private const val KEY_SDP = "sdp"
        private const val KEY_CANDIDATE = "candidate"
        private const val KEY_SDP_MID = "sdpMid"
        private const val KEY_SDP_M_LINE_INDEX = "sdpMLineIndex"
        private const val KEY_REASON = "reason"

        fun parse(json: String): LanSignalMessage {
            // 接收端先把 TCP 里读到的一帧 UTF-8 文本还原成对象，再按 type 分发。
            val obj = JSONObject(json)
            return LanSignalMessage(
                type = obj.getString(KEY_TYPE),
                sessionId = obj.getString(KEY_SESSION_ID),
                displayName = obj.optNullableString(KEY_DISPLAY_NAME),
                sdpType = obj.optNullableString(KEY_SDP_TYPE),
                sdp = obj.optNullableString(KEY_SDP),
                candidate = obj.optNullableString(KEY_CANDIDATE),
                sdpMid = obj.optNullableString(KEY_SDP_MID),
                sdpMLineIndex = if (obj.has(KEY_SDP_M_LINE_INDEX)) obj.optInt(KEY_SDP_M_LINE_INDEX) else null,
                reason = obj.optNullableString(KEY_REASON)
            )
        }
    }
}

/**
 * 服务端收到消息时，除了消息本身，还需要知道它来自哪个 IP。
 * 这个 remoteIp 来自 socket.inetAddress，比让对方自己在 JSON 里填 IP 更可信。
 */
data class LanSignalEnvelope(
    val remoteIp: String,
    val message: LanSignalMessage
)

private fun JSONObject.optNullableString(key: String): String? {
    // JSONObject.optString() 在字段不存在时会返回空字符串。
    // 业务层更希望用 null 表示“没有这个字段”，所以这里统一做一次转换。
    if (!has(key) || isNull(key)) {
        return null
    }
    return optString(key).takeIf { it.isNotEmpty() }
}
