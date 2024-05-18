/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type

import ru.pixnews.wasm.sqlite.open.helper.host.base.WasmValueType

/**
 * The contents of a `subscription`.
 */
public sealed class SubscriptionU(
    public open val eventType: Eventtype,
) {
    public data class FdRead(
        val subscriptionFdReadwrite: SubscriptionFdReadwrite,
    ) : SubscriptionU(Eventtype.FD_READ)

    public companion object : WasiTypename {
        public override val wasmValueType: WasmValueType = WasmValueType.I32
    }
    public data class Clock(
        val subscriptionClock: SubscriptionClock,
    ) : SubscriptionU(Eventtype.CLOCK)

    public data class FdWrite(
        val subscriptionFdReadwrite: SubscriptionFdReadwrite,
    ) : SubscriptionU(Eventtype.FD_WRITE)
}
