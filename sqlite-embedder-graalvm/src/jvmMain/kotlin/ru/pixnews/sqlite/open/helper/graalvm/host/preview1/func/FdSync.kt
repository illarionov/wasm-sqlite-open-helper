package ru.pixnews.sqlite.open.helper.graalvm.host.preview1.func

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary
import com.oracle.truffle.api.frame.VirtualFrame
import java.util.logging.Level
import java.util.logging.Logger
import ru.pixnews.sqlite.open.helper.graalvm.host.BaseWasmNode
import ru.pixnews.sqlite.open.helper.graalvm.host.Host
import org.graalvm.wasm.WasmContext
import org.graalvm.wasm.WasmInstance
import org.graalvm.wasm.WasmLanguage
import ru.pixnews.sqlite.open.helper.host.filesystem.SysException
import ru.pixnews.sqlite.open.helper.host.wasi.preview1.type.Errno
import ru.pixnews.sqlite.open.helper.host.wasi.preview1.type.Fd

internal class FdSync(
    language: WasmLanguage,
    instance: WasmInstance,
    private val host: Host,
    functionName: String = "fd_sync",
    private val logger: Logger = Logger.getLogger(FdSync::class.qualifiedName)
): BaseWasmNode(language, instance, functionName) {
    override fun executeWithContext(frame: VirtualFrame, context: WasmContext): Int {
        val args = frame.arguments
        return fdSync(Fd(args[0] as Int))
    }

    @TruffleBoundary
    private fun fdSync(
        fd: Fd
    ): Int {
        return try {
            host.fileSystem.sync(fd, metadata = true)
            Errno.SUCCESS
        } catch (e: SysException) {
            logger.log(Level.INFO, e) { "sync() error" }
            e.errNo
        }.code
    }
}
