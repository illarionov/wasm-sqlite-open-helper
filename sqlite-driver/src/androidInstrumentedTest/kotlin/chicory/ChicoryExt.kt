/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.driver.chicory

import android.os.Build
import org.junit.Assume.assumeTrue

internal fun checkChicorySdk() {
    assumeTrue(
        "Chicory requires API Level 28",
        Build.VERSION.SDK_INT >= 28,
    )
}
