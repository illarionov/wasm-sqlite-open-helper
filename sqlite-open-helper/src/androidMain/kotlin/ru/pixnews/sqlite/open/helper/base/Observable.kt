/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.sqlite.open.helper.base

/*
 * Original Copyrights:
 * Copyright (C) 2005-2012 The Android Open Source Project
 * Licensed under the Apache License, Version 2.0 (the "License")
 */

import android.database.ContentObserver
import android.database.DataSetObserver
import android.net.Uri

// Copy of the android.database.Observable
internal open class Observable<T : Any> {
    protected val observers: MutableList<T> = mutableListOf()

    open fun registerObserver(observer: T) {
        synchronized(observers) {
            check(!observers.contains(observer)) { "Observer $observer is already registered." }
            observers.add(observer)
        }
    }

    fun unregisterObserver(observer: T) {
        synchronized(observers) {
            check(observers.contains(observer)) { "Observer $observer was not registered." }
            observers.remove(observer)
        }
    }

    fun unregisterAll() {
        synchronized(observers) {
            observers.clear()
        }
    }
}

internal class ContentObservable : Observable<ContentObserver>() {
    fun dispatchChange(selfChange: Boolean, uri: Uri?) {
        synchronized(observers) {
            for (observer in observers) {
                if (!selfChange || observer.deliverSelfNotifications()) {
                    observer.dispatchChange(selfChange, uri)
                }
            }
        }
    }
}

internal class DataSetObservable : Observable<DataSetObserver>() {
    fun notifyChanged() {
        synchronized(observers) {
            for (i in observers.indices.reversed()) {
                observers[i].onChanged()
            }
        }
    }

    /**
     * Invokes [DataSetObserver.onInvalidated] on each observer.
     * Called when the data set is no longer valid and cannot be queried again,
     * such as when the data set has been closed.
     */
    fun notifyInvalidated() {
        synchronized(observers) {
            for (i in observers.indices.reversed()) {
                observers[i].onInvalidated()
            }
        }
    }
}
