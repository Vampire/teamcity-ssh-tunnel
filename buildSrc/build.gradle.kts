/*
 * Copyright 2019 Björn Kautler
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.benmanes.gradle.versions.reporter.result.Result
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `kotlin-dsl`
    id("com.github.ben-manes.versions") version "0.21.0"
}

buildscript {
    repositories {
        // have both in case JCenter is again refusing to work properly and Maven Central first
        mavenCentral()
        jcenter()
    }
    dependencies {
        classpath("com.fasterxml.jackson.core:jackson-databind:2.9.9")
    }
}

repositories {
    // have both in case JCenter is again refusing to work properly and Maven Central first
    mavenCentral()
    jcenter()
    @Suppress("UnstableApiUsage")
    gradlePluginPortal()
}

dependencies {
    implementation(gradlePlugin("com.github.ben-manes.versions:0.21.0"))
    implementation(gradlePlugin("org.ajoberstar.grgit:3.1.1"))
    implementation(gradlePlugin("com.github.rodm.teamcity-environments:1.3.2"))
    implementation(gradlePlugin("com.github.rodm.teamcity-common:1.3.2"))
    implementation(gradlePlugin("com.github.rodm.teamcity-agent:1.3.2"))
    implementation(gradlePlugin("com.github.rodm.teamcity-server:1.3.2"))
    implementation(gradlePlugin("net.researchgate.release:2.8.0"))
    implementation(gradlePlugin("net.wooga.github:1.4.0"))
    implementation("com.fasterxml.jackson.core:jackson-databind:2.9.9")
    implementation("org.kohsuke:github-api:1.95")
}

kotlinDslPluginOptions {
    experimentalWarning(false)
}

tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions {
        allWarningsAsErrors = true
    }
}

tasks.dependencyUpdates {
    checkForGradleUpdate = false

    resolutionStrategy {
        componentSelection {
            all {
                if (Regex("""(?i)[.-](?:${listOf(
                                "alpha",
                                "beta",
                                "rc",
                                "cr",
                                "m",
                                "preview",
                                "test",
                                "pre",
                                "b",
                                "ea"
                        ).joinToString("|")})[.\d-]*""").containsMatchIn(candidate.version)) {
                    reject("preliminary release")
                }
            }
        }
    }

    outputFormatter = closureOf<Result> {
        gradle = null
        file("build/dependencyUpdates/report.json")
                .apply { parentFile.mkdirs() }
                .also {
                    ObjectMapper().writerWithDefaultPrettyPrinter().writeValue(it, this)
                }
    }
}

@Suppress("UnstableApiUsage")
operator fun <T> Property<T>.invoke(value: T) = set(value)

fun gradlePlugin(plugin: String): String = plugin.let {
    val (id, version) = it.split(":", limit = 2)
    "$id:$id.gradle.plugin:$version"
}
