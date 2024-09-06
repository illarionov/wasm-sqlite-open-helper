/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.chasm.host.exception

import io.github.charlietap.chasm.embedding.error.ChasmError

public class ChasmModuleRuntimeErrorException(
    public val error: ChasmError,
    message: String? = null,
    cause: Throwable? = null,
) : ChasmException(message, cause)
