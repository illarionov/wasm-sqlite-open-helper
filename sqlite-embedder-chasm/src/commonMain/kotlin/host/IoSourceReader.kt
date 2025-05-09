/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package at.released.wasm.sqlite.open.helper.chasm.host

import io.github.charlietap.chasm.stream.SourceReader
import kotlinx.io.Source
import kotlinx.io.readByteArray

internal fun Source.toChasmSourceReader(): SourceReader = IoSourceReader(this)

private class IoSourceReader(
    private val source: Source,
) : SourceReader {
    override fun byte(): Byte = source.readByte()
    override fun bytes(amount: Int): ByteArray = source.readByteArray(amount)
    override fun exhausted(): Boolean = source.exhausted()
    override fun peek(): SourceReader = IoSourceReader(source.peek())
}
