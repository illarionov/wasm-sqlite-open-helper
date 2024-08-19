package ru.pixnews.wasm.sqlite.open.helper.host.filesystem.error

import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.model.Errno
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.model.Errno.INVAL

public data class InvalidArgument(
    override val message: String,
) : FileSystemOperationError,
    ReadError,
    WriteError
{
    override val errno: Errno = INVAL
}
