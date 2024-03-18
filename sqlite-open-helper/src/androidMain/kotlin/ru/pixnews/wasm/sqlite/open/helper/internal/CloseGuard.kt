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

import android.util.Log
import kotlin.concurrent.Volatile

/**
 * CloseGuard is a mechanism for flagging implicit finalizer cleanup of
 * resources that should have been cleaned up by explicit close
 * methods (aka "explicit termination methods" in Effective Java).
 *
 *
 * A simple example: <pre>   `class Foo {
 *
 * private final CloseGuard guard = CloseGuard.get();
 *
 * ...
 *
 * public Foo() {
 * ...;
 * guard.open("cleanup");
 * }
 *
 * public void cleanup() {
 * guard.close();
 * ...;
 * }
 *
 * protected void finalize() throws Throwable {
 * try {
 * if (guard != null) {
 * guard.warnIfOpen();
 * }
 * cleanup();
 * } finally {
 * super.finalize();
 * }
 * }
 * }
`</pre> *
 *
 * In usage where the resource to be explicitly cleaned up are
 * allocated after object construction, CloseGuard protection can
 * be deferred. For example: <pre>   `class Bar {
 *
 * private final CloseGuard guard = CloseGuard.get();
 *
 * ...
 *
 * public Bar() {
 * ...;
 * }
 *
 * public void connect() {
 * ...;
 * guard.open("cleanup");
 * }
 *
 * public void cleanup() {
 * guard.close();
 * ...;
 * }
 *
 * protected void finalize() throws Throwable {
 * try {
 * if (guard != null) {
 * guard.warnIfOpen();
 * }
 * cleanup();
 * } finally {
 * super.finalize();
 * }
 * }
 * }
`</pre> *
 *
 * When used in a constructor calls to `open` should occur at
 * the end of the constructor since an exception that would cause
 * abrupt termination of the constructor will mean that the user will
 * not have a reference to the object to cleanup explicitly. When used
 * in a method, the call to `open` should occur just after
 * resource acquisition.
 *
 *
 *
 *
 * Note that the null check on `guard` in the finalizer is to
 * cover cases where a constructor throws an exception causing the
 * `guard` to be uninitialized.
 *
 * @hide
 */
internal class CloseGuard private constructor() {
    private var allocationSite: Throwable? = null

    /**
     * Default Reporter which reports CloseGuard violations to the log.
     */
    private object DefaultReporter : Reporter {
        override fun report(message: String?, allocationSite: Throwable?) {
            Log.w("SQLite", message, allocationSite)
        }
    }

    /**
     * If CloseGuard is enabled, `open` initializes the instance
     * with a warning that the caller should have explicitly called the
     * `closer` method instead of relying on finalization.
     *
     * @param closer non-null name of explicit termination method
     * @throws NullPointerException if closer is null, regardless of
     * whether or not CloseGuard is enabled
     */
    fun open(closer: String) {
        // avoid allocating an allocationSite if disabled
        if (this == NOOP || !ENABLED) {
            return
        }
        val message = "Explicit termination method '$closer' not called"
        allocationSite = Throwable(message)
    }

    /**
     * Marks this CloseGuard instance as closed to avoid warnings on
     * finalization.
     */
    fun close() {
        allocationSite = null
    }

    /**
     * If CloseGuard is enabled, logs a warning if the caller did not
     * properly cleanup by calling an explicit close method
     * before finalization. If CloseGuard is disabled, no action is
     * performed.
     */
    fun warnIfOpen() {
        if (allocationSite == null || !ENABLED) {
            return
        }

        val message = "A resource was acquired at attached stack trace but never released. " +
                "See java.io.Closeable for information on avoiding resource leaks."

        _reporter.report(message, allocationSite)
    }

    /**
     * Interface to allow customization of reporting behavior.
     */
    fun interface Reporter {
        fun report(message: String?, allocationSite: Throwable?)
    }

    companion object {
        /**
         * Instance used when CloseGuard is disabled to avoid allocation.
         */
        private val NOOP = CloseGuard()

        /**
         * Enabled by default so we can catch issues early in VM startup.
         * Note, however, that Android disables this early in its startup,
         * but enables it with DropBoxing for system apps on debug builds.
         */
        @Volatile
        private var ENABLED = true

        /**
         * Hook for customizing how CloseGuard issues are reported.
         */
        @Volatile
        private var _reporter: Reporter = DefaultReporter

        var reporter: Reporter?
            /**
             * Returns non-null CloseGuard.Reporter.
             */
            get() = _reporter

            /**
             * Used to replace default Reporter used to warn of CloseGuard
             * violations. Must be non-null.
             */
            set(newReporter) {
                if (newReporter == null) {
                    throw NullPointerException("reporter == null")
                }
                _reporter = newReporter
            }

        /**
         * Returns a CloseGuard instance. If CloseGuard is enabled, `#open(String)` can be used to set up the
         * instance to warn on failure to close.
         * If CloseGuard is disabled, a non-null no-op instance is returned.
         */
        @JvmStatic
        fun get(): CloseGuard {
            if (!ENABLED) {
                return NOOP
            }
            return CloseGuard()
        }

        /**
         * Used to enable or disable CloseGuard. Note that CloseGuard only
         * warns if it is enabled for both allocation and finalization.
         */
        fun setEnabled(enabled: Boolean) {
            ENABLED = enabled
        }
    }
}