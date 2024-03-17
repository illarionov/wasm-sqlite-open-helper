/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

@file:Suppress("NOTHING_TO_INLINE")

package ru.pixnews.wasm.sqlite.open.helper.common.api

public interface SqliteUintBitMask<T : SqliteUintBitMask<T>> {
    public val newInstance: (UInt) -> T
    public val mask: UInt
}

public inline fun SqliteUintBitMask<*>.contains(flags: SqliteUintBitMask<*>): Boolean {
    return this.mask and flags.mask == flags.mask
}

public inline infix fun <T : SqliteUintBitMask<T>> SqliteUintBitMask<T>.and(flags: SqliteUintBitMask<*>): T {
    return newInstance(mask and flags.mask)
}

public inline infix fun <T : SqliteUintBitMask<T>> SqliteUintBitMask<T>.or(flags: SqliteUintBitMask<*>): T {
    return newInstance(mask or flags.mask)
}

public inline infix fun <T : SqliteUintBitMask<T>> SqliteUintBitMask<T>.xor(flags: SqliteUintBitMask<*>): T {
    return newInstance(mask xor flags.mask)
}

public inline infix fun <T : SqliteUintBitMask<T>> SqliteUintBitMask<T>.clear(flags: SqliteUintBitMask<*>): T {
    return newInstance(mask and flags.mask.inv())
}
