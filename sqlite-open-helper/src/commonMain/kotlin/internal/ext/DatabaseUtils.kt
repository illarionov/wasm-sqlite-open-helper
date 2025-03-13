/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package at.released.wasm.sqlite.open.helper.internal.ext

@Suppress("AVOID_USING_UTILITY_CLASS")
internal object DatabaseUtils {
    /**
     * Make a deep copy of the given argument list, ensuring that the returned
     * value is completely isolated from any changes to the original arguments.
     */
    fun deepCopyOf(args: List<Any?>): List<Any?> = args.map { arg ->
        when (arg) {
            null, is Number, is String -> {
                // When the argument is immutable, we can copy by reference
                arg
            }

            is ByteArray -> {
                // Need to deep copy blobs
                arg.copyOf()
            }

            else -> {
                // Convert everything else to string, making it immutable
                arg.toString()
            }
        }
    }
}
