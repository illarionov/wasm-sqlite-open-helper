/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package at.released.wasm.sqlite.open.helper

public interface UintBitMask<T : UintBitMask<T>> {
    public val newInstance: (UInt) -> T
    public val mask: UInt
}

public inline fun UintBitMask<*>.contains(flags: UintBitMask<*>): Boolean {
    return this.mask and flags.mask == flags.mask
}

public inline infix fun <T : UintBitMask<T>> UintBitMask<T>.and(flags: UintBitMask<*>): T {
    return newInstance(mask and flags.mask)
}

public inline infix fun <T : UintBitMask<T>> UintBitMask<T>.or(flags: UintBitMask<*>): T {
    return newInstance(mask or flags.mask)
}

public inline infix fun <T : UintBitMask<T>> UintBitMask<T>.xor(flags: UintBitMask<*>): T {
    return newInstance(mask xor flags.mask)
}

public inline infix fun <T : UintBitMask<T>> UintBitMask<T>.clear(flags: UintBitMask<*>): T {
    return newInstance(mask and flags.mask.inv())
}
