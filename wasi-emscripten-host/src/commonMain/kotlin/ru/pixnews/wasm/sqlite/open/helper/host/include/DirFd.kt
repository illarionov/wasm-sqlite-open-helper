/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.host.include

import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type.Fd as WasiFd

public sealed class DirFd(
    public open val rawValue: Int,
) {
    public data object Cwd : DirFd(Fcntl.AT_FDCWD) {
        override fun toString(): String = "AT_FDCWD"
    }

    public class FileDescriptor internal constructor(override val rawValue: Int) : DirFd(rawValue) {
        public val fd: WasiFd get() = WasiFd(rawValue)

        init {
            check(rawValue != Fcntl.AT_FDCWD)
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) {
                return true
            }
            if (javaClass != other?.javaClass) {
                return false
            }

            other as FileDescriptor

            return rawValue == other.rawValue
        }

        override fun hashCode(): Int = rawValue

        override fun toString(): String = "Fd($rawValue)"
    }

    public companion object {
        public operator fun invoke(rawDirFd: Int): DirFd = when (rawDirFd) {
            Fcntl.AT_FDCWD -> DirFd.Cwd
            else -> FileDescriptor(rawDirFd)
        }
    }
}
