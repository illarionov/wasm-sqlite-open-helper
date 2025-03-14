/*
 * Copyright 2024-2025, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package at.released.wasm.sqlite.open.helper.gradle.multiplatform

import at.released.wasm.sqlite.open.helper.gradle.multiplatform.publish.createWasmSqliteVersionsExtension
import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.KotlinMultiplatform
import com.vanniktech.maven.publish.SonatypeHost

/*
 * Convention plugin with publishing defaults
 */
plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("com.vanniktech.maven.publish.base")
}

private val wasmVersions = createWasmSqliteVersionsExtension()

tasks.withType<AbstractArchiveTask>().configureEach {
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
}

@Suppress("UnstableApiUsage")
private val publishedMavenLocalRoot = layout.settingsDirectory.dir("build/localMaven")
private val downloadableReleaseDirName = wasmVersions.rootVersion.map { "maven-wasm-sqlite-open-helper-$it" }
private val downloadableReleaseRoot = downloadableReleaseDirName.map { publishedMavenLocalRoot.dir(it) }

@Suppress("UnstableApiUsage")
private val distributionDir = layout.settingsDirectory.dir("build/distribution")

mavenPublishing {
    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL)
    publishing {
        repositories {
            maven {
                name = "test"
                setUrl(publishedMavenLocalRoot.dir("test"))
            }
            maven {
                name = "downloadableRelease"
                setUrl(downloadableReleaseRoot)
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
        KotlinMultiplatform(javadocJar = JavadocJar.Dokka("dokkaGenerateModuleHtml")),
    )

    pom {
        name.set(project.name)
        description.set(
            "Implementation of SQLiteDriver and SupportSQLiteOpenHelper based on SQLite compiled for WebAssembly",
        )
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

tasks.register<Zip>("packageMavenDistribution") {
    archiveBaseName = "maven-wasm-sqlite-open-helper"
    destinationDirectory = distributionDir

    from(downloadableReleaseRoot)
    into(downloadableReleaseDirName)

    isReproducibleFileOrder = true
    isPreserveFileTimestamps = false

    dependsOn(tasks.named("publishAllPublicationsToDownloadableReleaseRepository"))
}
