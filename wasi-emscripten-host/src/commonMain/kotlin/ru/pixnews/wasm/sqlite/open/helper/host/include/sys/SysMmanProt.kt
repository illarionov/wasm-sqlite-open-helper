/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.host.include.sys

import ru.pixnews.wasm.sqlite.open.helper.common.api.SqliteUintBitMask

/**
 * Memory protection bits for mmap
 *
 * <sys/mman.h>
 */
@JvmInline
public value class SysMmanProt(
    public override val mask: UInt,
) : SqliteUintBitMask<SysMmanProt> {
    override val newInstance: (UInt) -> SysMmanProt get() = ::SysMmanProt

    public companion object {
        public val PROT_NONE: SysMmanProt = SysMmanProt(0U)
        public val PROT_READ: SysMmanProt = SysMmanProt(1U)
        public val PROT_WRITE: SysMmanProt = SysMmanProt(2U)
        public val PROT_EXEC: SysMmanProt = SysMmanProt(4U)
        public val PROT_GROWSDOWN: SysMmanProt = SysMmanProt(0x0100_0000_U)
        public val PROT_GROWSUP: SysMmanProt = SysMmanProt(0x0200_0000_U)
    }
}
