/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.host.filesystem.model

/**
 * Base path used to resolve relative paths
 */
@Suppress("WRONG_ORDER_IN_CLASS_LIKE_STRUCTURES")
public sealed class BaseDirectory {
    /**
     * Relative paths are not allowed
     */
    public data object None : BaseDirectory()

    /**
     * Paths are resolved relative to the current working directory
     */
    public data object CurrentWorkingDirectory : BaseDirectory()

    /**
     * Relative paths are resolved relative to the directory associated with the file descriptor [fd]
     */
    public data class DirectoryFd(val fd: Fd) : BaseDirectory()

    public companion object
}
