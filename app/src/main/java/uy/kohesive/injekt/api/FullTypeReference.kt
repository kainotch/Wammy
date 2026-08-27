// Created by Notch
package uy.kohesive.injekt.api

import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type

open class FullTypeReference<T> {
    val type: Type
        get() {
            val superclass = this.javaClass.genericSuperclass
            if (superclass is ParameterizedType) {
                return superclass.actualTypeArguments[0]
            }
            return Any::class.java
        }
}
