/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

@file:Suppress("BLANK_LINE_BETWEEN_PROPERTIES")

package ru.pixnews.sqlite.open.helper.wasm.builder.sqlite

import org.gradle.api.Named
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.ProviderFactory
import java.io.Serializable
import javax.inject.Inject

public open class SqliteWasmBuildSpec @Inject internal constructor(
    objects: ObjectFactory,
    providers: ProviderFactory,
    private val name: String,
) : Named, Serializable {
    public val sqliteVersion: Property<String> = objects.property(String::class.java)
        .convention("3450100")

    public val sqlite3Source: ConfigurableFileCollection = objects.fileCollection()

    public val wasmBaseFileName: Property<String> = objects.property(String::class.java)
        .convention("sqlite3")

    public val wasmUnstrippedFileName: Property<String> = objects.property(String::class.java)
        .convention(
            providers.provider {
                "${wasmBaseFileName.get()}-$name-${sqliteVersion.get()}-unstripped.wasm"
            },
        )

    public val wasmFileName: Property<String> = objects.property(String::class.java)
        .convention(
            providers.provider {
                "${wasmBaseFileName.get()}-$name-${sqliteVersion.get()}.wasm"
            },
        )

    override fun getName(): String = name

    public companion object {
        private const val serialVersionUID: Long = -1
    }
}
