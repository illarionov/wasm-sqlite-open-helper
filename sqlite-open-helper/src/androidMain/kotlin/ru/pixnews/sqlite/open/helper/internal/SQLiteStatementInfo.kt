/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.sqlite.open.helper.internal

/*
 * Original Copyrights:
 * Copyright (C) 2017-2024 requery.io
 * Copyright (C) 2005-2012 The Android Open Source Project
 * Licensed under the Apache License, Version 2.0 (the "License")
 */

/**
 * Describes a SQLite statement.
 *
 * @property numParameters
 *   The number of parameters that the statement has.
 * @property columnNames
 *   The names of all columns in the result set of the statement.
 * @property readOnly
 *   True if the statement is read-only.
 */
internal data class SQLiteStatementInfo(
    val numParameters: Int,
    val columnNames: List<String>,
    val readOnly: Boolean,
)
