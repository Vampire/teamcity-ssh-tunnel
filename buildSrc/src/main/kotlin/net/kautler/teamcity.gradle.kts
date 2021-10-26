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

plugins {
    id("com.github.rodm.teamcity-environments")
}

configure(listOf(project(":server"), project(":serverPre2018.2"))) {
    apply<TeamcityServerPlugin>()
}

teamcity {
    val versions: Map<String, String> by project
    version = versions["teamcity"]

    environments {
        val teamcityEnvironment = environments.create("teamcity") {
            version = versions["teamcityTest"]
            val serverPlugin by project(":server").tasks.existing
            plugins(serverPlugin)
        }

        extra["teamcityHomeDir"] = teamcityEnvironment.homeDir ?: file("${teamcity.environments.baseHomeDir}/Teamcity-${teamcityEnvironment.version}")

        environments.register("teamcity2021.1") {
            version = versions["teamcity2021.1Test"]
            val serverPlugin by project(":server").tasks.existing
            plugins(serverPlugin)
        }

        environments.register("teamcity2020.2") {
            version = versions["teamcity2020.2Test"]
            val serverPlugin by project(":server").tasks.existing
            plugins(serverPlugin)
        }

        environments.register("teamcity2020.1") {
            version = versions["teamcity2020.1Test"]
            val serverPlugin by project(":server").tasks.existing
            plugins(serverPlugin)
        }

        environments.register("teamcity2019.2") {
            version = versions["teamcity2019.2Test"]
            val serverPlugin by project(":server").tasks.existing
            plugins(serverPlugin)
        }

        environments.register("teamcity2019.1") {
            version = versions["teamcity2019.1Test"]
            val serverPlugin by project(":server").tasks.existing
            plugins(serverPlugin)
        }

        environments.register("teamcity2018.2") {
            version = versions["teamcity2018.2Test"]
            val serverPlugin by project(":server").tasks.existing
            plugins(serverPlugin)
        }

        environments.register("teamcity2018.1") {
            version = versions["teamcity2018.1Test"]
            val serverPlugin by project(":serverPre2018.2").tasks.existing
            plugins(serverPlugin)
        }
    }
}

project(":common") {
    apply<TeamcityCommonPlugin>()
}

project(":agent") {
    apply<TeamcityAgentPlugin>()
}

project(":commonServer") {
    apply<TeamcityCommonServerPlugin>()
}
