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

import com.github.rodm.teamcity.TeamCityPlugin

plugins {
    java
}

apply<TeamCityPlugin>()
apply<JavaPlugin>()

dependencies {
    val teamcityHomeDir: File by project
    val versions: Map<String, String> by project
    implementation(project(":common"))
    compileOnly(files("$teamcityHomeDir/webapps/ROOT/WEB-INF/plugins/ssh-manager/server/ssh-manager.jar"))
    compileOnly("org.jetbrains.teamcity:server-api:${versions["teamcity"]}")
}
