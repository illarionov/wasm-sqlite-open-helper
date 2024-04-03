/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.builder.icu

import org.gradle.api.Named
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.provider.SetProperty
import ru.pixnews.wasm.sqlite.open.helper.builder.attribute.ICU_DATA_PACKAGING_STATIC
import ru.pixnews.wasm.sqlite.open.helper.builder.ext.WasmBuildDsl
import ru.pixnews.wasm.sqlite.open.helper.builder.icu.internal.IcuBuildDefaults
import ru.pixnews.wasm.sqlite.open.helper.builder.icu.internal.IcuBuildDefaults.ICU_DEFAULT_TARGET
import java.io.Serializable
import javax.inject.Inject

@WasmBuildDsl
public abstract class IcuWasmBuildSpec @Inject internal constructor(
    objects: ObjectFactory,
    private val name: String,
) : Named, Serializable {
    public val dataPackaging: Property<String> = objects.property(String::class.java)
        .convention(ICU_DATA_PACKAGING_STATIC)
    public val target: Property<String> = objects.property(String::class.java)
        .convention(ICU_DEFAULT_TARGET)
    public val buildFeatures: SetProperty<IcuBuildFeature> = objects.setProperty(IcuBuildFeature::class.java)
        .convention(IcuBuildFeature.DEFAULT)
    public val icuAdditionalCflags: ListProperty<String> = objects.listProperty(String::class.java)
        .convention(IcuBuildDefaults.ICU_CFLAGS)
    public val icuAdditionalCxxflags: ListProperty<String> = objects.listProperty(String::class.java)
        .convention(icuAdditionalCflags)
    public val icuAdditionalForceLibs: ListProperty<String> = objects.listProperty(String::class.java)
        .convention(IcuBuildDefaults.ICU_FORCE_LIBS)
    public val usePthreads: Provider<Boolean> = objects.property(Boolean::class.java)
        .convention(IcuBuildDefaults.ICU_USE_PTHREADS)

    override fun getName(): String = name

    public companion object {
        private const val serialVersionUID: Long = -1
    }
}
