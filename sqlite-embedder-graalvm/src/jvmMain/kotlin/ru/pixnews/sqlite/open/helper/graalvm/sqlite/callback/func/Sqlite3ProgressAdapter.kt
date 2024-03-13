package ru.pixnews.sqlite.open.helper.graalvm.sqlite.callback.func

import com.oracle.truffle.api.CompilerDirectives
import com.oracle.truffle.api.frame.VirtualFrame
import java.util.logging.Logger
import ru.pixnews.sqlite.open.helper.graalvm.ext.asWasmPtr
import ru.pixnews.sqlite.open.helper.graalvm.host.BaseWasmNode
import ru.pixnews.sqlite.open.helper.graalvm.sqlite.callback.Sqlite3CallbackStore
import org.graalvm.wasm.WasmContext
import org.graalvm.wasm.WasmInstance
import org.graalvm.wasm.WasmLanguage
import ru.pixnews.sqlite.open.helper.common.api.WasmPtr
import ru.pixnews.sqlite.open.helper.sqlite.common.api.SqliteDb
import ru.pixnews.sqlite.open.helper.sqlite.common.api.SqliteProgressCallback

internal const val SQLITE3_PROGRESS_CB_FUNCTION_NAME = "sqlite3_progress_cb"

internal class Sqlite3ProgressAdapter(
    language: WasmLanguage,
    instance: WasmInstance,
    private val callbackStore: Sqlite3CallbackStore,
    functionName: String,
    private val logger: Logger = Logger.getLogger(Sqlite3ProgressAdapter::class.qualifiedName)
) : BaseWasmNode(language, instance, functionName) {
    override fun executeWithContext(frame: VirtualFrame, context: WasmContext): Int {
        val args = frame.arguments
        return invokeProgressCallback(
            args.asWasmPtr(0),
        )
    }

    @CompilerDirectives.TruffleBoundary
    private fun invokeProgressCallback(
        contextPointer: WasmPtr<SqliteDb>,
    ): Int {
        logger.finest { "invokeProgressCallback() db: $contextPointer" }
        val delegate: SqliteProgressCallback = callbackStore.sqlite3ProgressCallbacks[contextPointer]
            ?: error("Callback $contextPointer not registered")

        return delegate.invoke(contextPointer)
    }
}