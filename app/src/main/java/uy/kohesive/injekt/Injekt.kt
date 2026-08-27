// Created by Notch
@file:JvmName("InjektKt")
package uy.kohesive.injekt

import uy.kohesive.injekt.api.InjektScope

val Injekt = InjektScope()

inline fun <reified T> injectLazy(): Lazy<T> = lazy { Injekt.get<T>() }
