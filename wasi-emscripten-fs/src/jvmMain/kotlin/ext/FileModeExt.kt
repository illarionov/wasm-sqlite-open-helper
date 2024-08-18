/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.host.filesystem.ext

import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.model.FileMode
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.model.FileMode.Companion.S_IRGRP
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.model.FileMode.Companion.S_IROTH
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.model.FileMode.Companion.S_IRUSR
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.model.FileMode.Companion.S_IWGRP
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.model.FileMode.Companion.S_IWOTH
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.model.FileMode.Companion.S_IWUSR
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.model.FileMode.Companion.S_IXGRP
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.model.FileMode.Companion.S_IXOTH
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.model.FileMode.Companion.S_IXUSR
import java.nio.file.attribute.FileAttribute
import java.nio.file.attribute.PosixFilePermission
import java.nio.file.attribute.PosixFilePermissions

internal fun Set<PosixFilePermission>.asFileAttribute(): FileAttribute<Set<PosixFilePermission>> =
    PosixFilePermissions.asFileAttribute(this)

@Suppress("NO_BRACES_IN_CONDITIONALS_AND_LOOPS")
internal fun FileMode.toPosixFilePermissions(): Set<PosixFilePermission> {
    val permissions: MutableSet<PosixFilePermission> = mutableSetOf()

    if (mask and S_IRUSR != 0U) permissions += PosixFilePermission.OWNER_READ
    if (mask and S_IWUSR != 0U) permissions += PosixFilePermission.OWNER_WRITE
    if (mask and S_IXUSR != 0U) permissions += PosixFilePermission.OWNER_EXECUTE

    if (mask and S_IRGRP != 0U) permissions += PosixFilePermission.GROUP_READ
    if (mask and S_IWGRP != 0U) permissions += PosixFilePermission.GROUP_WRITE
    if (mask and S_IXGRP != 0U) permissions += PosixFilePermission.GROUP_EXECUTE

    if (mask and S_IROTH != 0U) permissions += PosixFilePermission.OTHERS_READ
    if (mask and S_IWOTH != 0U) permissions += PosixFilePermission.OTHERS_WRITE
    if (mask and S_IXOTH != 0U) permissions += PosixFilePermission.OTHERS_EXECUTE

    return permissions
}
