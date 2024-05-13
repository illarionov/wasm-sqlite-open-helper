/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.builder.base.ext

import org.gradle.api.file.FileTree
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import java.io.File
import java.util.concurrent.Callable

public fun FileTree.firstDirectory(
    providerFactory: ProviderFactory,
): Provider<File> = providerFactory.provider(FileTreeFirstDirectory(this))

public class FileTreeFirstDirectory(
    private val tree: FileTree,
) : Callable<File> {
    override fun call(): File {
        var dir: File? = null
        tree.visit {
            if (isDirectory) {
                dir = this.file
                stopVisiting()
            }
        }
        return dir ?: error("No directory")
    }
}
