/*
 * Copyright (c) 2024, the wasm-sqlite-open-helper project authors and contributors.
 * Please see the AUTHORS file for details.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package ru.pixnews.wasm.sqlite.open.helper.gradle.settings

/*
 * Base settings convention plugin for the use in library modules
 */
plugins {
    id("ru.pixnews.wasm.sqlite.open.helper.gradle.settings.common")
    id("ru.pixnews.wasm.sqlite.open.helper.gradle.settings.repositories")
}
