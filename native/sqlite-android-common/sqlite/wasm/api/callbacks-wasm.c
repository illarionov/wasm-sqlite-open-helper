/*
 * External host functions that will be called from SQLite callbacks.
 * Keel in sync with SqliteCallbacksModuleFunction
 */
#include <emscripten.h>

#define SQLITE3_CALLBACK_MANAGER_MODULE_NAME "sqlite3-callback-manager"

#ifndef SQLITE_WASM_CB_IMPORT
#define SQLITE_WASM_CB_IMPORT __attribute__((used,noinline,optnone,import_module(SQLITE3_CALLBACK_MANAGER_MODULE_NAME)))
#endif

SQLITE_WASM_CB_IMPORT int ext_sqlite3_trace_cb(unsigned int, void *, void *, int);
SQLITE_WASM_CB_IMPORT int ext_sqlite3_progress_cb(void *);
SQLITE_WASM_CB_IMPORT void ext_sqlite3_logging_cb(int, void *, int);

// Unused function to ensure that Emscripten retains function declarations in WASM imports
__attribute__((used,noinline))
void __keep_sqlite_cb_callbacks() {
  ext_sqlite3_trace_cb(0, 0, 0, 0);
  ext_sqlite3_progress_cb(0);
  ext_sqlite3_logging_cb(0, 0, 0);
}
