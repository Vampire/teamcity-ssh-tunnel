/*
 * Copyright 2019 Bjoern Kautler
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

package net.kautler

import org.gradle.api.JavaVersion.VERSION_1_8
import org.gradle.api.JavaVersion.toVersion
import kotlin.text.Charsets.UTF_8

plugins {
    java
}

base {
    archivesBaseName = "ssh-tunnel"
}

java {
    sourceCompatibility = VERSION_1_8
}

tasks.jar {
    archiveAppendix(project.name)
}

tasks.withType<JavaCompile>().configureEach {
    options.apply {
        encoding = UTF_8.name()
        if (JavaVersion.current().isJava9Compatible) {
            compilerArgs.apply {
                add("--release")
                add(toVersion(targetCompatibility).majorVersion)
            }
        }
    }
}
