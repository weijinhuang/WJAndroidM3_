package com.wj.basecomponent.util

import com.wj.basecomponent.util.fw.annotations.FWByteOrder
import com.wj.basecomponent.util.fw.annotations.FWType
import com.wj.basecomponent.util.fw.annotations.Size
import com.wj.basecomponent.util.fw.basictype.*
import java.lang.reflect.Field

object BufferConverter {

    /**
     * 为FW通讯所编写的字节数组解析工具。 将JavaBean解析为字节数组。
     * 注意，仅适用于与FW通讯的接口解析。
     * 使用方法如下：
     *
     * 根据FW接口文档，将结构体声明为Java类，字段使用{@FWType}注解，具体注解字段含义请看注解的类注释
     * 字段类型对应如下：
     * unsigned char  ->  FWUnsignedChar
     * char           ->  FWChar
     * int            ->  FWInt
     * unsigned short ->  FWUnsignedShort
     * unsigned long  ->  FWUnsignedLong
     * unsigned int   ->  FWUnsignedInt
     * char[]         ->  FWString
     *
     * 目前除了char数组以外暂无其他数组类型，故未支持基本类型的数组类型。
     *
     * 支持结构体嵌套。
     */
    @JvmStatic
    fun getBuffer(t: Any): ByteArray {
        val byteList = ArrayList<Byte>()
        parseObject(
            t,
            t.javaClass,
            byteList
        )
        return byteList.toByteArray()
    }

    @JvmStatic
    fun <T> parseObject(
        t: T,
        clazzJava: Class<*>,
        byteList: ArrayList<Byte>
    ) {

        var declaredFields = clazzJava.declaredFields
        if (declaredFields.isEmpty()) {
            return
        }
        sortFields(declaredFields)
        for (field in declaredFields) {
            if (field == null) {
                continue
            }
            field.isAccessible = true
            val fieldAnnotationType = field.getAnnotation(FWType::class.java) ?: continue
            if (fieldAnnotationType.isObject) {//对象
                val obj = field.get(t)
                parseObject(obj, obj::class.java, byteList)
            } else if (fieldAnnotationType.isArray) {
                val array = field.get(t) as Array<*>
                for (e in array) {

                    if (e is FWBasicType<*>) {
                        paseFieldInstanceToByte(
                            e,
                            e.javaClass,
                            byteList,
                            fieldAnnotationType.byteOrder
                        )
                    }
                    parseObject(e, e!!::class.java, byteList)
                }

            } else {
                if (field.type == EmptyPack::class.java) {
                    byteList.addAll(ByteArray(fieldAnnotationType.size).toList())
                } else {
                    val fwByteOrder = fieldAnnotationType.byteOrder
                    val fieldValue = field.get(t)
                    val type = field.type
                    paseFieldInstanceToByte(fieldValue, type, byteList, fwByteOrder)
                }
            }
        }
    }

    private fun paseFieldInstanceToByte(
        fieldValue: Any?,
        field: Class<*>,
        byteList: ArrayList<Byte>,
        fwByteOrder: String
    ) {
        if (fieldValue == null) {
            when (field) {
                FWChar::class.java -> {
                    byteList.addAll(arrayListOf(0))
                }
                FWUnsignedChar::class.java -> {
                    byteList.addAll(arrayListOf(0))
                }
                FWUnsignedInt::class.java -> {
                    byteList.addAll(arrayListOf(0, 0, 0, 0))
                }
                FWInt::class.java -> {
                    byteList.addAll(arrayListOf(0, 0, 0, 0))
                }
                FWUnsignedShort::class.java -> {
                    byteList.addAll(arrayListOf(0, 0))
                }
                FWUnsignedLong::class.java -> {
                    byteList.addAll(arrayListOf(0, 0, 0, 0))
                }
            }

        } else {
            if (fieldValue is FWBasicType<*>) {
                if (fwByteOrder == FWByteOrder.LITTLE) {
                    byteList.addAll(fieldValue.bytesLittleOrder.toList())
                } else {
                    byteList.addAll(fieldValue.bytesBigOrder.toList())
                }
            }
        }
    }

    /**
     * 按照注解的字段顺序号给字段排序。因为有时候反射得到的字段顺序不一定是符合类声明上的顺序。
     */
    @JvmStatic
    fun sortFields(declaredFields: Array<Field>) {
        val arrayLength = declaredFields.size
        for (i in 0 until arrayLength) {
            for (j in 0 until arrayLength - i - 1) {
                if (declaredFields[j].fieldOrder() > declaredFields[j + 1].fieldOrder()) {
                    val temp = declaredFields[j]
                    declaredFields[j] = declaredFields[j + 1]
                    declaredFields[j + 1] = temp
                }
            }
        }
    }

    @JvmStatic
    fun Field.fieldOrder(): Int {
        val annotation = getAnnotation(FWType::class.java)
        return annotation?.fieldOrder ?: 0
    }


    @JvmStatic
    fun <T> getObject(byteArray: ByteArray, clazzJava: Class<T>): T {
        val currentPos = intArrayOf(0)

        return getObject(byteArray, currentPos, clazzJava)
    }

    /**
     *注意，此对象必须要有一个无参构造函数
     * @param pos 初始传 0
     * @author H.W.J
     * @date 2021/3/10
     */
    @JvmStatic
    fun <T> getObject(byteArray: ByteArray, currentPos: IntArray, clazzJava: Class<T>): T {
        var declaredFields = clazzJava.declaredFields
        sortFields(declaredFields)
        val newInstance = clazzJava.newInstance()
        if (declaredFields.isEmpty()) {
            return newInstance
        }
        for (field in declaredFields) {
            if (field == null) {
                continue
            }
            field.isAccessible = true
            val fieldClass = field.type
            val fieldAnnotation = field.getAnnotation(FWType::class.java) ?: continue
            if (fieldClass == EmptyPack::class.java) {
                currentPos[0] += fieldAnnotation.size
                continue
            }
            if (fieldAnnotation.isObject) {
                val any = getObject(byteArray, currentPos, fieldClass)
                field.set(newInstance, any)
            } else if (fieldAnnotation.isArray) {
                if (fieldAnnotation.size == Int.MIN_VALUE) {
                    throw RuntimeException("illegal annotation size")
                }
                val genericType = (field.genericType as Class<*>).componentType
                val fieldInstance =
                    java.lang.reflect.Array.newInstance(genericType!!, fieldAnnotation.size)
                for (i in 0 until fieldAnnotation.size) {
                    val arrayMemberFieldInstance = genericType.newInstance()
                    if (arrayMemberFieldInstance is FWBasicType<*>) {
                        val arrayMemberClassAnnotation = genericType.getAnnotation(Size::class.java)
                        val size = arrayMemberClassAnnotation?.value
                            ?: throw RuntimeException("undefine type size")
                        if (fieldAnnotation.byteOrder == FWByteOrder.LITTLE) {//默认小端
                            arrayMemberFieldInstance.value =
                                arrayMemberFieldInstance.getValueLittleOrder(
                                    byteArray.copyOfRange(
                                        currentPos[0],
                                        currentPos[0] + size
                                    )
                                )
                        } else {
                            arrayMemberFieldInstance.value =
                                arrayMemberFieldInstance.getValueBigOrder(
                                    byteArray.copyOfRange(
                                        currentPos[0],
                                        currentPos[0] + size
                                    )
                                )
                        }
                        currentPos[0] += size
                        java.lang.reflect.Array.set(fieldInstance, i, arrayMemberFieldInstance)
                    }else {
                        val any = getObject(byteArray, currentPos, genericType)
                        java.lang.reflect.Array.set(fieldInstance, i, any)
                    }
                }
                field.set(newInstance, fieldInstance)
            } else {
                val fieldInstance = fieldClass.newInstance()
                if (fieldInstance is FWBasicType<*>) {
                    var size = fieldAnnotation.size
                    if (fieldInstance is FWString) {
                        if (size != 0) {
                            fieldInstance.value = fieldInstance.getValueLittleOrder(
                                byteArray.copyOfRange(
                                    currentPos[0],
                                    currentPos[0] + size
                                )
                            )
                            currentPos[0] += size
                        } else {
                            throw RuntimeException("undefine type size")
                        }
                    } else {
                        if (size <= 0) {
                            val classAnnotation = fieldClass.getAnnotation(Size::class.java)
                            size =
                                classAnnotation?.value
                                    ?: throw RuntimeException("undefine type size")
                        }
                        if (fieldAnnotation.byteOrder == FWByteOrder.LITTLE) {//默认小端
                            fieldInstance.value = fieldInstance.getValueLittleOrder(
                                byteArray.copyOfRange(
                                    currentPos[0],
                                    currentPos[0] + size
                                )
                            )
                        } else {
                            fieldInstance.value = fieldInstance.getValueBigOrder(
                                byteArray.copyOfRange(
                                    currentPos[0],
                                    currentPos[0] + size
                                )
                            )
                        }
                        currentPos[0] += size

                    }
                    field.set(newInstance, fieldInstance)
                }

            }
        }
        return newInstance
    }


}