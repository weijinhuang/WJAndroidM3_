package com.wj.basecomponent.util

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

/**
*@Create by H.W.J 2025/11/5/Wed
*/
class SPUtils {
    companion object {
        private const val DEFAULT_SP_NAME = "default_sp"

        @Volatile
        private var instance: SPUtils? = null
        private lateinit var sharedPreferences: SharedPreferences

        fun init(context: Context, spName: String = DEFAULT_SP_NAME) {
            sharedPreferences = context.getSharedPreferences(spName, Context.MODE_PRIVATE)
        }

        @JvmStatic
        fun getInstance(): SPUtils {
            return instance ?: synchronized(this) {
                instance ?: SPUtils().also { instance = it }
            }
        }
    }

    /**
     * 存储字符串
     */
    fun putString(key: String, value: String) {
        sharedPreferences.edit { putString(key, value) }
    }

    /**
     * 存储整型
     */
    fun putInt(key: String, value: Int) {
        sharedPreferences.edit { putInt(key, value) }
    }

    /**
     * 存储长整型
     */
    fun putLong(key: String, value: Long) {
        sharedPreferences.edit { putLong(key, value) }
    }

    /**
     * 存储浮点型
     */
    fun putFloat(key: String, value: Float) {
        sharedPreferences.edit().putFloat(key, value).apply()
    }

    /**
     * 存储布尔值
     */
    fun putBoolean(key: String, value: Boolean) {
        sharedPreferences.edit().putBoolean(key, value).apply()
    }

    /**
     * 获取字符串
     * @param defaultValue 默认值
     */
    fun getString(key: String, defaultValue: String = ""): String {
        return sharedPreferences.getString(key, defaultValue) ?: defaultValue
    }

    /**
     * 获取整型
     * @param defaultValue 默认值
     */
    fun getInt(key: String, defaultValue: Int = 0): Int {
        return sharedPreferences.getInt(key, defaultValue)
    }

    /**
     * 获取长整型
     * @param defaultValue 默认值
     */
    fun getLong(key: String, defaultValue: Long = 0L): Long {
        return sharedPreferences.getLong(key, defaultValue)
    }

    /**
     * 获取浮点型
     * @param defaultValue 默认值
     */
    fun getFloat(key: String, defaultValue: Float = 0f): Float {
        return sharedPreferences.getFloat(key, defaultValue)
    }

    /**
     * 获取布尔值
     * @param defaultValue 默认值
     */
    fun getBoolean(key: String, defaultValue: Boolean = false): Boolean {
        return sharedPreferences.getBoolean(key, defaultValue)
    }

    /**
     * 删除指定key的数据
     */
    fun remove(key: String) {
        sharedPreferences.edit().remove(key).apply()
    }

    /**
     * 清空所有数据
     */
    fun clear() {
        sharedPreferences.edit().clear().apply()
    }

    /**
     * 检查是否包含某个key
     */
    fun contains(key: String): Boolean {
        return sharedPreferences.contains(key)
    }
}