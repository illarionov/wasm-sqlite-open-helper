/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.internal

/*
 * Original Copyrights:
 * Copyright (C) 2017-2024 requery.io
 * Copyright (C) 2005-2012 The Android Open Source Project
 * Licensed under the Apache License, Version 2.0 (the "License")
 */

import java.io.Closeable
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/**
 * An object created from a SQLiteDatabase that can be closed.
 *
 * This class implements a primitive reference counting scheme for database objects.
 */
internal abstract class SQLiteClosable internal constructor() : Closeable {
    private var referenceCount = 1

    /**
     * Called when the last reference to the object was released by
     * a call to [.releaseReference] or [.close].
     */
    protected abstract fun onAllReferencesReleased()

    /**
     * Acquires a reference to the object.
     *
     * @throws IllegalStateException if the last reference to the object has already
     * been released.
     */
    fun acquireReference(): Unit = synchronized(this) {
        check(referenceCount > 0) { "attempt to re-open an already-closed object: $this" }
        referenceCount++
    }

    /**
     * Releases a reference to the object, closing the object if the last reference
     * was released.
     *
     * @see .onAllReferencesReleased
     */
    fun releaseReference() {
        var refCountIsZero: Boolean
        synchronized(this) {
            refCountIsZero = --referenceCount == 0
        }
        if (refCountIsZero) {
            onAllReferencesReleased()
        }
    }

    /**
     * Releases a reference to the object, closing the object if the last reference
     * was released.
     *
     * Calling this method is equivalent to calling [.releaseReference].
     *
     * @see .releaseReference
     * @see .onAllReferencesReleased
     */
    override fun close() {
        releaseReference()
    }
}

@OptIn(ExperimentalContracts::class)
internal inline fun <R : Any?> SQLiteClosable.useReference(
    block: () -> R,
): R {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    acquireReference()
    return try {
        block()
    } finally {
        releaseReference()
    }
}
