package com.wj.androidm3.business.ui.recyclerview

data class RecyclerViewPractiseItem(
    var id: Int,

    var name: String
) {


    var areContentsTheSame = true
    override fun toString(): String {
        return "(id=$id, name='$name', areContentsTheSame='$areContentsTheSame' hashCode:${hashCode()})"
    }
}
