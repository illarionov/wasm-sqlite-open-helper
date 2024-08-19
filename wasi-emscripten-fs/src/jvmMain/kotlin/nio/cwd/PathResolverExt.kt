/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.host.filesystem.nio.cwd

import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.error.BadFileDescriptor
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.error.InvalidArgument
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.error.NotDirectory
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.error.ResolveRelativePathErrors
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.nio.cwd.PathResolver.ResolvePathError

internal fun ResolvePathError.toCommonError(): ResolveRelativePathErrors = when (this) {
    is ResolvePathError.EmptyPath -> InvalidArgument(message)
    is ResolvePathError.FileDescriptorNotOpen -> BadFileDescriptor(message)
    is ResolvePathError.InvalidPath -> BadFileDescriptor(message)
    is ResolvePathError.NotDirectory -> NotDirectory(message)
    is ResolvePathError.RelativePath -> InvalidArgument(message)
}
