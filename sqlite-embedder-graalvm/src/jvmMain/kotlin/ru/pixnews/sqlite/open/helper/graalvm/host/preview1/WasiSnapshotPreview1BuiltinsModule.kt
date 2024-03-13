/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.sqlite.open.helper.graalvm.host.preview1

import ru.pixnews.sqlite.open.helper.graalvm.host.preview1.func.WasiUnsupportedFunctionNode
import org.graalvm.wasm.WasmContext
import org.graalvm.wasm.WasmInstance
import org.graalvm.wasm.WasmLanguage
import org.graalvm.wasm.WasmModule
import org.graalvm.wasm.WasmType
import org.graalvm.wasm.constants.Sizes
import org.graalvm.wasm.predefined.BuiltinModule
import org.graalvm.wasm.predefined.wasi.WasiArgsGetNode
import org.graalvm.wasm.predefined.wasi.WasiArgsSizesGetNode
import org.graalvm.wasm.predefined.wasi.WasiClockTimeGetNode
import org.graalvm.wasm.predefined.wasi.WasiConstantRandomGetNode
import org.graalvm.wasm.predefined.wasi.WasiEnvironGetNode
import org.graalvm.wasm.predefined.wasi.WasiEnvironSizesGetNode
import org.graalvm.wasm.predefined.wasi.WasiFdCloseNode
import org.graalvm.wasm.predefined.wasi.WasiFdFdstatGetNode
import org.graalvm.wasm.predefined.wasi.WasiFdFdstatSetFlagsNode
import org.graalvm.wasm.predefined.wasi.WasiFdFilestatGetNode
import org.graalvm.wasm.predefined.wasi.WasiFdPrestatDirNameNode
import org.graalvm.wasm.predefined.wasi.WasiFdPrestatGetNode
import org.graalvm.wasm.predefined.wasi.WasiFdReadNode
import org.graalvm.wasm.predefined.wasi.WasiFdSeekNode
import org.graalvm.wasm.predefined.wasi.WasiFdWriteNode
import org.graalvm.wasm.predefined.wasi.WasiPathCreateDirectoryNode
import org.graalvm.wasm.predefined.wasi.WasiPathFileStatGetNode
import org.graalvm.wasm.predefined.wasi.WasiPathFilestatSetTimesNode
import org.graalvm.wasm.predefined.wasi.WasiPathLinkNode
import org.graalvm.wasm.predefined.wasi.WasiPathOpenNode
import org.graalvm.wasm.predefined.wasi.WasiPathReadLinkNode
import org.graalvm.wasm.predefined.wasi.WasiPathRemoveDirectoryNode
import org.graalvm.wasm.predefined.wasi.WasiPathRenameNode
import org.graalvm.wasm.predefined.wasi.WasiPathSymlinkNode
import org.graalvm.wasm.predefined.wasi.WasiPathUnlinkFileNode
import org.graalvm.wasm.predefined.wasi.WasiProcExitNode
import org.graalvm.wasm.predefined.wasi.WasiRandomGetNode
import org.graalvm.wasm.predefined.wasi.WasiSchedYieldNode

internal class WasiSnapshotPreview1BuiltinsModule : BuiltinModule() {
    val NUMBER_OF_FUNCTIONS = 28

    public override fun createInstance(language: WasmLanguage, context: WasmContext, name: String): WasmInstance {
        val instance = WasmInstance(context, WasmModule.createBuiltin(name), NUMBER_OF_FUNCTIONS)
        if (context.contextOptions.supportMemory64()) {
            importMemory(instance, "env", "memory", 0, Sizes.MAX_MEMORY_64_DECLARATION_SIZE, true, false)
        } else {
            importMemory(instance, "env", "memory", 0, Sizes.MAX_MEMORY_DECLARATION_SIZE.toLong(), false, false)
        }
        defineFunction(
            instance,
            "args_sizes_get",
            types(WasmType.I32_TYPE, WasmType.I32_TYPE),
            types(WasmType.I32_TYPE),
            WasiArgsSizesGetNode(language, instance)
        )
        defineFunction(
            instance,
            "args_get",
            types(WasmType.I32_TYPE, WasmType.I32_TYPE),
            types(WasmType.I32_TYPE),
            WasiArgsGetNode(language, instance)
        )
        defineFunction(
            instance,
            "environ_sizes_get",
            types(WasmType.I32_TYPE, WasmType.I32_TYPE),
            types(WasmType.I32_TYPE),
            WasiEnvironSizesGetNode(language, instance)
        )
        defineFunction(
            instance,
            "environ_get",
            types(WasmType.I32_TYPE, WasmType.I32_TYPE),
            types(WasmType.I32_TYPE),
            WasiEnvironGetNode(language, instance)
        )
        defineFunction(
            instance, "clock_time_get",
            types(WasmType.I32_TYPE, WasmType.I64_TYPE, WasmType.I32_TYPE),
            types(WasmType.I32_TYPE),
            WasiClockTimeGetNode(language, instance)
        )
        defineFunction(instance, "proc_exit", types(WasmType.I32_TYPE), types(), WasiProcExitNode(language, instance))
        defineFunction(
            instance,
            "fd_write",
            types(WasmType.I32_TYPE, WasmType.I32_TYPE, WasmType.I32_TYPE, WasmType.I32_TYPE),
            types(
                WasmType.I32_TYPE
            ),
            WasiFdWriteNode(language, instance)
        )
        defineFunction(
            instance,
            "fd_read",
            types(WasmType.I32_TYPE, WasmType.I32_TYPE, WasmType.I32_TYPE, WasmType.I32_TYPE),
            types(
                WasmType.I32_TYPE
            ),
            WasiFdReadNode(language, instance)
        )
        defineFunction(
            instance,
            "fd_close",
            types(WasmType.I32_TYPE),
            types(WasmType.I32_TYPE),
            WasiFdCloseNode(language, instance)
        )
        defineFunction(
            instance,
            "fd_seek",
            types(WasmType.I32_TYPE, WasmType.I64_TYPE, WasmType.I32_TYPE, WasmType.I32_TYPE),
            types(
                WasmType.I32_TYPE
            ),
            WasiFdSeekNode(language, instance)
        )
        defineFunction(
            instance,
            "fd_fdstat_get",
            types(WasmType.I32_TYPE, WasmType.I32_TYPE),
            types(WasmType.I32_TYPE),
            WasiFdFdstatGetNode(language, instance)
        )
        defineFunction(
            instance,
            "fd_fdstat_set_flags",
            types(WasmType.I32_TYPE, WasmType.I32_TYPE),
            types(WasmType.I32_TYPE),
            WasiFdFdstatSetFlagsNode(language, instance)
        )
        defineFunction(
            instance,
            "fd_prestat_get",
            types(WasmType.I32_TYPE, WasmType.I32_TYPE),
            types(WasmType.I32_TYPE),
            WasiFdPrestatGetNode(language, instance)
        )
        defineFunction(
            instance,
            "fd_prestat_dir_name",
            types(WasmType.I32_TYPE, WasmType.I32_TYPE, WasmType.I32_TYPE),
            types(WasmType.I32_TYPE),
            WasiFdPrestatDirNameNode(language, instance)
        )
        defineFunction(
            instance,
            "fd_sync",
            types(WasmType.I32_TYPE),
            types(WasmType.I32_TYPE),
            WasiUnsupportedFunctionNode(language, instance, "__wasi_fd_sync")
        )
        defineFunction(
            instance,
            "fd_filestat_get",
            types(WasmType.I32_TYPE, WasmType.I32_TYPE),
            types(WasmType.I32_TYPE),
            WasiFdFilestatGetNode(language, instance)
        )
        defineFunction(
            instance,
            "path_open",
            types(
                WasmType.I32_TYPE,
                WasmType.I32_TYPE,
                WasmType.I32_TYPE,
                WasmType.I32_TYPE,
                WasmType.I32_TYPE,
                WasmType.I64_TYPE,
                WasmType.I64_TYPE,
                WasmType.I32_TYPE,
                WasmType.I32_TYPE
            ),
            types(
                WasmType.I32_TYPE
            ),
            WasiPathOpenNode(language, instance)
        )
        defineFunction(
            instance, "path_create_directory",
            types(WasmType.I32_TYPE, WasmType.I32_TYPE, WasmType.I32_TYPE), types(
                WasmType.I32_TYPE
            ), WasiPathCreateDirectoryNode(language, instance)
        )
        defineFunction(
            instance, "path_remove_directory",
            types(WasmType.I32_TYPE, WasmType.I32_TYPE, WasmType.I32_TYPE), types(
                WasmType.I32_TYPE
            ), WasiPathRemoveDirectoryNode(language, instance)
        )
        defineFunction(
            instance,
            "path_filestat_set_times",
            types(
                WasmType.I32_TYPE,
                WasmType.I32_TYPE,
                WasmType.I32_TYPE,
                WasmType.I32_TYPE,
                WasmType.I64_TYPE,
                WasmType.I64_TYPE,
                WasmType.I32_TYPE
            ),
            types(
                WasmType.I32_TYPE
            ),
            WasiPathFilestatSetTimesNode(language, instance)
        )
        defineFunction(
            instance,
            "path_link",
            types(
                WasmType.I32_TYPE,
                WasmType.I32_TYPE,
                WasmType.I32_TYPE,
                WasmType.I32_TYPE,
                WasmType.I32_TYPE,
                WasmType.I32_TYPE,
                WasmType.I32_TYPE
            ),
            types(
                WasmType.I32_TYPE
            ),
            WasiPathLinkNode(language, instance)
        )
        defineFunction(
            instance,
            "path_rename",
            types(
                WasmType.I32_TYPE,
                WasmType.I32_TYPE,
                WasmType.I32_TYPE,
                WasmType.I32_TYPE,
                WasmType.I32_TYPE,
                WasmType.I32_TYPE
            ),
            types(
                WasmType.I32_TYPE
            ),
            WasiPathRenameNode(language, instance)
        )
        defineFunction(
            instance,
            "path_symlink",
            types(
                WasmType.I32_TYPE,
                WasmType.I32_TYPE,
                WasmType.I32_TYPE,
                WasmType.I32_TYPE,
                WasmType.I32_TYPE
            ),
            types(
                WasmType.I32_TYPE
            ),
            WasiPathSymlinkNode(language, instance)
        )
        defineFunction(
            instance, "path_unlink_file",
            types(WasmType.I32_TYPE, WasmType.I32_TYPE, WasmType.I32_TYPE), types(
                WasmType.I32_TYPE
            ), WasiPathUnlinkFileNode(language, instance)
        )
        defineFunction(
            instance,
            "path_readlink",
            types(WasmType.I32_TYPE, WasmType.I32_TYPE, WasmType.I32_TYPE, WasmType.I32_TYPE, WasmType.I32_TYPE),
            types(
                WasmType.I32_TYPE
            ),
            WasiPathReadLinkNode(language, instance)
        )
        defineFunction(
            instance,
            "path_filestat_get",
            types(WasmType.I32_TYPE, WasmType.I32_TYPE, WasmType.I32_TYPE, WasmType.I32_TYPE, WasmType.I32_TYPE),
            types(
                WasmType.I32_TYPE
            ),
            WasiPathFileStatGetNode(language, instance)
        )
        defineFunction(
            instance,
            "sched_yield",
            types(),
            types(WasmType.I32_TYPE),
            WasiSchedYieldNode(language, instance)
        )
        if (context.contextOptions.constantRandomGet()) {
            defineFunction(
                instance,
                "random_get",
                types(WasmType.I32_TYPE, WasmType.I32_TYPE),
                types(WasmType.I32_TYPE),
                WasiConstantRandomGetNode(language, instance)
            )
        } else {
            defineFunction(
                instance,
                "random_get",
                types(WasmType.I32_TYPE, WasmType.I32_TYPE),
                types(WasmType.I32_TYPE),
                WasiRandomGetNode(language, instance)
            )
        }
        return instance
    }
}
