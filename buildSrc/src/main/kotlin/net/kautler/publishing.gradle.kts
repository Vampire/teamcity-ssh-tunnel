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

import net.researchgate.release.GitAdapter.GitConfig
import net.researchgate.release.ReleasePlugin
import org.ajoberstar.grgit.Grgit
import org.kohsuke.github.GHIssueState.OPEN
import org.kohsuke.github.GitHub
import wooga.gradle.github.publish.tasks.GithubPublish
import kotlin.LazyThreadSafetyMode.NONE
import com.github.rodm.teamcity.tasks.PublishTask

plugins {
    id("net.researchgate.release")
    id("net.wooga.github")
}

val releaseVersion = !version.toString().endsWith("-SNAPSHOT")

extra["release.useAutomaticVersion"] = BooleanProperty(project, "release.useAutomaticVersion").getValue()
extra["release.releaseVersion"] = StringProperty(project, "release.releaseVersion").getValue()
extra["release.newVersion"] = StringProperty(project, "release.newVersion").getValue()
extra["github.token"] = StringProperty(project, "github.token").getValue()

release {
    tagTemplate = "v\$version"
    val gitConfig = getProperty("git") as GitConfig
    gitConfig.signTag = true
}

tasks.afterReleaseBuild {
    dependsOn(provider { allprojects.map { it.tasks.withType<PublishTask>() } })
}

val grgit: Grgit? by project

val githubRepositoryName by lazy(NONE) {
    grgit?.let {
        it.remote.list()
                .find { it.name == "origin" }
                ?.let {
                    Regex("""(?x)
                        (?:
                            ://([^@]++@)?+github\.com(?::\d++)?+/ |
                            ([^@]++@)?+github\.com:
                        )
                        (?<repositoryName>.*)
                        \.git
                    """)
                            .find(it.url)
                            ?.let { it.groups["repositoryName"]!!.value }
                }
    } ?: "Vampire/teamcity-ssh-tunnel"
}

val releaseTagName by lazy(NONE) { plugins.findPlugin(ReleasePlugin::class)!!.tagName()!! }

val github by lazy(NONE) { GitHub.connectUsingOAuth(extra["github.token"] as String)!! }

val releaseBody by lazy(NONE) {
    grgit?.let {
        it.log {
            github.getRepository(githubRepositoryName).latestRelease?.run { excludes.add(tagName) }
        }
                .filter { !it.shortMessage.startsWith("[Gradle Release Plugin] ") }
                .joinToString("\n") { "- ${it.shortMessage} [${it.id}]" }
    } ?: ""
}

tasks.githubPublish {
    val serverPluginTasks = provider { allprojects.map { it.tasks.findByName("serverPlugin") }.filterNotNull() }
    enabled = releaseVersion
    dependsOn(serverPluginTasks)
    repositoryName(githubRepositoryName)
    tagName(releaseTagName)
    releaseName(releaseTagName)
    body { releaseBody }
    draft(true)
    from(serverPluginTasks)
}

tasks.afterReleaseBuild {
    dependsOn(tasks.withType<GithubPublish>())
}

val finishMilestone by tasks.registering {
    enabled = releaseVersion
    shouldRunAfter(tasks.withType<GithubPublish>())

    @Suppress("UnstableApiUsage")
    doLast("finish milestone") {
        github.getRepository(githubRepositoryName)!!.run {
            listMilestones(OPEN)
                    .find { it.title == "Next Version" }!!
                    .run {
                        updateTitle(releaseTagName)
                        close()
                    }

            createMilestone("Next Version", null)
        }
    }
}

tasks.afterReleaseBuild {
    dependsOn(finishMilestone)
}
