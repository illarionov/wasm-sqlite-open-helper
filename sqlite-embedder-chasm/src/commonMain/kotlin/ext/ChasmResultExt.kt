/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package at.released.wasm.sqlite.open.helper.chasm.ext

import at.released.wasm.sqlite.open.helper.chasm.host.exception.ChasmErrorException
import io.github.charlietap.chasm.embedding.error.ChasmError
import io.github.charlietap.chasm.embedding.shapes.ChasmResult
import io.github.charlietap.chasm.embedding.shapes.fold

internal fun <S, E : ChasmError> ChasmResult<S, E>.orThrow(
    message: (() -> String?)? = null,
): S = fold(
    { it },
    { error ->
        throw ChasmErrorException(error, message?.invoke() + "; " + error.toString())
    },
)
