/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.chasm.io

import okio.FileSystem
import okio.Path
import ru.pixnews.wasm.sqlite.open.helper.common.api.InternalWasmSqliteHelperApi

@InternalWasmSqliteHelperApi
public actual fun getBinaryReader(): WasmBinaryReader = NativeBinaryReader()

private class NativeBinaryReader(
    private val appName: String = "wasm-sqlite-open-helper",
    private val xdgBaseDirs: XdgBaseDirectory = XdgBaseDirectory,
) : WasmBinaryReader {
    override fun readBytes(url: String): ByteArray {
        val fs = FileSystem.SYSTEM
        val failedPaths: MutableList<Pair<String, Throwable>> = mutableListOf()

        for (base in getAppDataDirs()) {
            val path = base.resolve(url)
            val result: Result<ByteArray> = kotlin.runCatching {
                fs.read(path) {
                    this.readByteArray()
                }
            }
            result.onSuccess {
                return it
            }.onFailure {
                failedPaths.add(path.toString() to it)
            }
        }

        if (failedPaths.isEmpty()) {
            throw WasmBinaryReaderIoException("Could not determine the full path to `$url`")
        } else {
            val (firstPath, firstError) = failedPaths.first()
            throw WasmBinaryReaderIoException(
                "Could not determine the full path to `$url`. " +
                        "Error when opening a file `$firstPath`: $firstError",
                failedPaths,
            )
        }
    }

    private fun getAppDataDirs(): List<Path> {
        return xdgBaseDirs.getBaseDataDirectories()
            .map { it.resolve(appName) }
    }
}

public class WasmBinaryReaderIoException(
    message: String?,
    paths: List<Pair<String, Throwable>> = emptyList(),
) : RuntimeException(
    message,
    paths.lastOrNull()?.second,
)
