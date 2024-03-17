/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type

import ru.pixnews.wasm.sqlite.open.helper.host.WasmValueType

/**
 * Subscription to an event.
 *
 * @param userdata User-provided value that is attached to the subscription in the  implementation and returned
 * through `event::userdata`.
 * @param u The type of the event to which to subscribe, and its contents
 */
@Suppress("KDOC_NO_CONSTRUCTOR_PROPERTY_WITH_COMMENT", "IDENTIFIER_LENGTH")
public data class Subscription(
    val userdata: Userdata, // (field $userdata $userdata)
    val u: SubscriptionU, // (field $u $subscription_u)
) {
    public companion object : WasiTypename {
        public override val wasmValueType: WasmValueType = WasmValueType.I32
    }
}
