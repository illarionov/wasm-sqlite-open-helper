/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

import ru.pixnews.wasm.sqlite.open.helper.builder.attribute.ICU_DATA_PACKAGING_ARCHIVE

plugins {
    id("ru.pixnews.icu-wasm-builder")
}

group = "ru.pixnews.icu-wasm"

icuBuild {
    builds {
        create("main-archive") {
            dataPackaging = ICU_DATA_PACKAGING_ARCHIVE
        }
    }
}
