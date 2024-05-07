/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.host.jvm.filesystem

import ru.pixnews.wasm.sqlite.open.helper.host.include.Fcntl
import ru.pixnews.wasm.sqlite.open.helper.host.include.FileMode
import java.nio.file.attribute.FileAttribute
import java.nio.file.attribute.PosixFilePermission
import java.nio.file.attribute.PosixFilePermissions

internal fun Set<PosixFilePermission>.asFileAttribute(): FileAttribute<Set<PosixFilePermission>> =
    PosixFilePermissions.asFileAttribute(this)

@Suppress("NO_BRACES_IN_CONDITIONALS_AND_LOOPS")
internal fun FileMode.toPosixFilePermissions(): Set<PosixFilePermission> {
    val permissions: MutableSet<PosixFilePermission> = mutableSetOf()

    if (mask and Fcntl.S_IRUSR != 0U) permissions += PosixFilePermission.OWNER_READ
    if (mask and Fcntl.S_IWUSR != 0U) permissions += PosixFilePermission.OWNER_WRITE
    if (mask and Fcntl.S_IXUSR != 0U) permissions += PosixFilePermission.OWNER_EXECUTE

    if (mask and Fcntl.S_IRGRP != 0U) permissions += PosixFilePermission.GROUP_READ
    if (mask and Fcntl.S_IWGRP != 0U) permissions += PosixFilePermission.GROUP_WRITE
    if (mask and Fcntl.S_IXGRP != 0U) permissions += PosixFilePermission.GROUP_EXECUTE

    if (mask and Fcntl.S_IROTH != 0U) permissions += PosixFilePermission.OTHERS_READ
    if (mask and Fcntl.S_IWOTH != 0U) permissions += PosixFilePermission.OTHERS_WRITE
    if (mask and Fcntl.S_IXOTH != 0U) permissions += PosixFilePermission.OTHERS_EXECUTE

    return permissions
}
