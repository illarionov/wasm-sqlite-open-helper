/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.builder.attribute

import org.gradle.api.attributes.LibraryElements
import org.gradle.api.attributes.LibraryElements.DYNAMIC_LIB
import org.gradle.api.attributes.Usage
import org.gradle.api.model.ObjectFactory
import org.gradle.kotlin.dsl.named
import org.gradle.nativeplatform.MachineArchitecture
import org.gradle.nativeplatform.OperatingSystemFamily

public val ObjectFactory.wasm32Architecture: MachineArchitecture get() = named("wasm32")

public val ObjectFactory.emscriptenOperatingSystem: OperatingSystemFamily get() = named("emscripten")

public val ObjectFactory.wasmApiUsage: Usage get() = named("wasm-api")

public val ObjectFactory.wasmRuntimeUsage: Usage get() = named("wasm-runtime")

public val ObjectFactory.wasmBinaryLibraryElements: LibraryElements get() = named(DYNAMIC_LIB)
