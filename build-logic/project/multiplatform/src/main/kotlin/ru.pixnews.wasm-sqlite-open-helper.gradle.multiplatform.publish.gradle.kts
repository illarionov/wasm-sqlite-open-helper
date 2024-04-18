/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.KotlinMultiplatform
import org.jetbrains.dokka.gradle.DokkaTask
import ru.pixnews.wasm.sqlite.open.helper.gradle.publish.createWasmSqliteVersionsExtension

/*
 * Convention plugin with publishing defaults
 */
plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("com.vanniktech.maven.publish.base")
    id("org.jetbrains.dokka")
}

createWasmSqliteVersionsExtension()

tasks.withType<AbstractArchiveTask>().configureEach {
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
}

mavenPublishing {
    publishing {
        repositories {
            maven {
                name = "test"
                setUrl(project.rootProject.layout.buildDirectory.dir("localMaven"))
            }
            maven {
                name = "PixnewsS3"
                setUrl("s3://maven.pixnews.ru/")
                credentials(AwsCredentials::class) {
                    accessKey = providers.environmentVariable("YANDEX_S3_ACCESS_KEY_ID").getOrElse("")
                    secretKey = providers.environmentVariable("YANDEX_S3_SECRET_ACCESS_KEY").getOrElse("")
                }
            }
        }
    }

    signAllPublications()

    configure(
        KotlinMultiplatform(javadocJar = JavadocJar.Dokka("dokkaGfm")),
    )

    pom {
        name.set(project.name)
        description.set("Implementation of SupportSQLiteOpenHelper based on SQLite compiled for WebAssembly")
        url.set("https://github.com/illarionov/wasm-sqlite-open-helper")

        licenses {
            license {
                name.set("The Apache License, Version 2.0")
                url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                distribution.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
            }
        }
        developers {
            developer {
                id.set("illarionov")
                name.set("Alexey Illarionov")
                email.set("alexey@0xdc.ru")
            }
        }
        scm {
            connection.set("scm:git:git://github.com/illarionov/wasm-sqlite-open-helper.git")
            developerConnection.set("scm:git:ssh://github.com:illarionov/wasm-sqlite-open-helper.git")
            url.set("https://github.com/illarionov/wasm-sqlite-open-helper")
        }
    }
}

tasks.withType<DokkaTask> {
    notCompatibleWithConfigurationCache("https://github.com/Kotlin/dokka/issues/2231")
    dokkaSourceSets.configureEach {
        skipDeprecated.set(true)
    }
}
