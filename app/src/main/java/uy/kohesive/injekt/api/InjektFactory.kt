// Created by Notch
package uy.kohesive.injekt.api
import java.lang.reflect.Type

interface InjektFactory {
    fun getInstance(type: Type): Any
}
