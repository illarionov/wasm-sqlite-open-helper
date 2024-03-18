/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")

package ru.pixnews.wasm.sqlite.open.helper.exception

import android.database.SQLException
import android.database.sqlite.SQLiteException

public actual typealias AndroidSqlException = SQLException
public actual typealias AndroidSqliteException = SQLiteException
