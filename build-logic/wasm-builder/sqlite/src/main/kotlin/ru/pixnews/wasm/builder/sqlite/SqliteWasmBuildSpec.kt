/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

@file:Suppress("BLANK_LINE_BETWEEN_PROPERTIES")

package ru.pixnews.wasm.builder.sqlite

import org.gradle.api.Named
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.ProjectLayout
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.ProviderFactory
import ru.pixnews.wasm.builder.base.WasmBuildDsl
import java.io.Serializable
import javax.inject.Inject

@WasmBuildDsl
public open class SqliteWasmBuildSpec @Inject internal constructor(
    objects: ObjectFactory,
    providers: ProviderFactory,
    projectLayout: ProjectLayout,
    private val name: String,
) : Named, Serializable {
    private val sqliteWasmFilesSrdDir = projectLayout.projectDirectory.dir("../sqlite-android-common/sqlite")

    public val sqliteVersion: Property<String> = objects.property(String::class.java)
        .convention("3450100")

    public val sqlite3Source: ConfigurableFileCollection = objects.fileCollection()

    public val additionalSourceFiles: ConfigurableFileCollection = objects.fileCollection().apply {
        this.from(sqliteWasmFilesSrdDir.file("wasm/api/sqlite3-wasm.c"))
    }

    public val additionalIncludes: ConfigurableFileCollection = objects.fileCollection().apply {
        this.from(sqliteWasmFilesSrdDir.dir("wasm/api"))
    }

    public val additionalLibs: ConfigurableFileCollection = objects.fileCollection()

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

    public val codeGenerationOptions: ListProperty<String> = objects.listProperty(String::class.java)
        .convention(SqliteCodeGenerationOptions.codeGenerationOptionsMultithread)

    public val codeOptimizationOptions: ListProperty<String> = objects.listProperty(String::class.java)
        .convention(SqliteCodeGenerationOptions.codeOptimizationOptionsO2)

    public val emscriptenConfigurationOptions: ListProperty<String> = objects.listProperty(String::class.java)
        .convention(SqliteCodeGenerationOptions.emscriptenConfigurationOptionMultithread)

    public val exportedFunctions: ListProperty<String> = objects.listProperty(String::class.java)
        .convention(SqliteExportedFunctions.openHelperExportedFunctions)

    public val sqliteConfigOptions: ListProperty<String> = objects.listProperty(String::class.java)
        .convention(SqliteConfigurationOptions.openHelperConfig())

    override fun getName(): String = name

    public companion object {
        private const val serialVersionUID: Long = -2
    }
}
