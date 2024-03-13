package ru.pixnews.sqlite.open.helper.graalvm.host.emscripten.func

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary
import com.oracle.truffle.api.frame.VirtualFrame
import java.util.logging.Logger
import ru.pixnews.sqlite.open.helper.graalvm.ext.asWasmPtr
import ru.pixnews.sqlite.open.helper.graalvm.host.BaseWasmNode
import ru.pixnews.sqlite.open.helper.graalvm.host.Host
import org.graalvm.wasm.WasmContext
import org.graalvm.wasm.WasmInstance
import org.graalvm.wasm.WasmLanguage
import ru.pixnews.sqlite.open.helper.common.api.WasmPtr
import ru.pixnews.sqlite.open.helper.host.memory.encodeToNullTerminatedByteArray
import ru.pixnews.sqlite.open.helper.host.memory.write
import ru.pixnews.sqlite.open.helper.host.wasi.preview1.type.Errno

internal class SyscallGetcwd(
    language: WasmLanguage,
    instance: WasmInstance,
    private val host: Host,
    functionName: String = "__syscall_getcwd",
    private val logger: Logger = Logger.getLogger(SyscallGetcwd::class.qualifiedName)
): BaseWasmNode(language, instance, functionName) {
    override fun executeWithContext(frame: VirtualFrame, context: WasmContext): Int {
        val args = frame.arguments
        return syscallGetcwd(
            args.asWasmPtr(0),
            args[1] as Int,
        )
    }

    @TruffleBoundary
    private fun syscallGetcwd(
        dst: WasmPtr<Byte>,
        size: Int
    ): Int {
        logger.finest { "getCwd(dst: $dst size: $size)" }
        if (size == 0) return -Errno.INVAL.code

        val path = host.fileSystem.getCwd()
        val pathBytes: ByteArray = path.encodeToNullTerminatedByteArray()

        if (size < pathBytes.size) {
            return -Errno.RANGE.code
        }
        memory.write(dst, pathBytes)

        return pathBytes.size
    }
}
