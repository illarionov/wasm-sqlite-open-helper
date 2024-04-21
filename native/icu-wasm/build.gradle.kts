/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

import ru.pixnews.wasm.sqlite.open.helper.builder.attribute.ICU_DATA_PACKAGING_ARCHIVE
import ru.pixnews.wasm.sqlite.open.helper.builder.attribute.ICU_DATA_PACKAGING_STATIC
import ru.pixnews.wasm.sqlite.open.helper.builder.icu.IcuWasmBuildSpec

plugins {
    id("ru.pixnews.icu-wasm-builder")
}

group = "ru.pixnews.icu-wasm"

/*
 * ICU builds in different configurations
 */
icuBuild {
    builds {
        create("main-datastatic-multithread") {
            dataPackaging = ICU_DATA_PACKAGING_STATIC
            usePthreads = true
        }
        create("main-datastatic") {
            dataPackaging = ICU_DATA_PACKAGING_STATIC
            usePthreads = false
        }
        createWithArchiveDatapackagingArchive("main-dataarchive-multithread") {
            usePthreads = false
        }
        createWithArchiveDatapackagingArchive("main-dataarchive") {
            usePthreads = false
        }
    }
}

fun NamedDomainObjectContainer<IcuWasmBuildSpec>.createWithArchiveDatapackagingArchive(
    name: String,
    configurationBlock: IcuWasmBuildSpec.() -> Unit,
) {
    create(name) {
        dataPackaging = ICU_DATA_PACKAGING_ARCHIVE
        icuDataDir = "/usr/share/icu/${libs.versions.icu}"
        icuAdditionalCflags = listOf(
            "-O3",
            "-DU_HAVE_MMAP=0",
            "-DUCONFIG_NO_FORMATTING",
            "-DUCONFIG_NO_LEGACY_CONVERSION",
            "-DUCONFIG_NO_TRANSLITERATION",
        )
        buildByDefault = false
        configurationBlock()
    }
}
