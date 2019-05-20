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

import com.github.rodm.teamcity.tasks.PublishTask
import javax.naming.ConfigurationException

plugins {
    java
    id("com.github.rodm.teamcity-server")
}

apply<JavaPlugin>()

val jetbrainsUsername by StringProperty(project, "jetbrains.username")
val jetbrainsPassword by StringProperty(project, "jetbrains.password")

teamcity {
    if (project.name == "serverPre2018.2") {
        val versions: Map<String, String> by project
        version = versions["teamcity2018.1"]
    }

    server {
        descriptor {
            name = "ssh-tunnel"
            displayName = "SSH Tunnel"
            version = project.version.toString()
            description = rootProject.description
            downloadUrl = "https://github.com/Vampire/teamcity-ssh-tunnel/releases/latest"
            email = "Bjoern@Kautler.net"
            // work-around for https://github.com/gradle/gradle/issues/9383
            vendorName = "Bj\u00f6rn Kautler"
            vendorUrl = "https://github.com/Vampire/teamcity-ssh-tunnel"
            useSeparateClassloader = true

            if (project.name == "serverPre2018.2") {
                maximumBuild = "59999"
            } else {
                allowRuntimeReload = true
                minimumBuild = "60000"
            }

            dependencies {
                plugin("ssh-manager")
            }
        }

        files {
            from(project(":commonServer").file("resources")) {
                include("kotlin-dsl/**")
            }
        }

        publish {
            username = jetbrainsUsername
            password = jetbrainsPassword
        }
    }
}

// do this with taskGraph.whenReady as doFirst is too late for the mandatory inputs
// if inputs get optional, for example to support tokens, this can be changed to doFirst
gradle.taskGraph.whenReady {
    if (allTasks.any { it is PublishTask }) {
        if (jetbrainsUsername.isNullOrBlank()) {
            throw ConfigurationException(
                    "Please set the JetBrains username with project property 'jetbrains.username' " +
                            "or '${rootProject.name}.jetbrains.username'. " +
                            "If both are set, the latter will be effective.")
        }
        if (jetbrainsPassword.isNullOrBlank()) {
            throw ConfigurationException(
                    "Please set the JetBrains password with project property 'jetbrains.password' " +
                            "or '${rootProject.name}.jetbrains.password'. " +
                            "If both are set, the latter will be effective.")
        }
    }
}

tasks.jar {
    enabled = false
}

dependencies {
    agent(project(path = ":agent", configuration = "plugin"))
    server(project(":commonServer"))
}
