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

package net.kautler.teamcity.ssh_tunnel.server.common;

import jetbrains.buildServer.serverSide.ProjectManager;
import jetbrains.buildServer.serverSide.SBuildServer;
import jetbrains.buildServer.ssh.SecureServerSshKeyManager;
import jetbrains.buildServer.ssh.ServerSshKeyManager;
import jetbrains.buildServer.ssh.TeamCitySshKey;
import jetbrains.buildServer.util.MultiMap;
import net.kautler.teamcity.ssh_tunnel.common.Constants;
import net.kautler.teamcity.ssh_tunnel.common.ParametersHelper;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

import static java.lang.Integer.parseInt;
import static java.util.stream.Collectors.toList;
import static net.kautler.teamcity.ssh_tunnel.common.Constants.MIN_VERSION_SUPPORTING_BUILD_FEATURE_REQUIREMENTS;
import static net.kautler.teamcity.ssh_tunnel.common.Constants.SSH_TUNNEL_REQUIREMENT_PROPERTY_NAME;

public class ServerParametersHelper extends ParametersHelper {
    @NotNull
    private final ProjectManager projectManager;

    @NotNull
    private final ServerSshKeyManager sshKeyManager;

    @NotNull
    private final SBuildServer buildServer;

    public ServerParametersHelper(@NotNull ProjectManager projectManager,
                                  @NotNull SecureServerSshKeyManager sshKeyManager,
                                  @NotNull SBuildServer buildServer) {
        this.projectManager = projectManager;
        this.sshKeyManager = sshKeyManager;
        this.buildServer = buildServer;
    }

    @Override
    public MultiMap<String, String> validate(Map<String, String> properties) {
        MultiMap<String, String> result = super.validate(properties);
        // part of work-around for missing BuildFeature#getRequirements
        if (parseInt(buildServer.getBuildNumber()) >= MIN_VERSION_SUPPORTING_BUILD_FEATURE_REQUIREMENTS) {
            properties.remove(SSH_TUNNEL_REQUIREMENT_PROPERTY_NAME);
        }
        return result;
    }

    @Override
    protected boolean isValidKeyName(String sshKeyName) {
        return projectManager.getActiveProjects().stream()
                .flatMap(project -> sshKeyManager.getOwnKeys(project).stream())
                .anyMatch(teamCitySshKey -> teamCitySshKey.getName().equals(sshKeyName));
    }

    @Override
    protected boolean isEncrypted(String sshKeyName) {
        List<Boolean> encrypted = projectManager.getActiveProjects().stream()
                .flatMap(project -> sshKeyManager.getOwnKeys(project).stream())
                .filter(teamCitySshKey -> teamCitySshKey.getName().equals(sshKeyName))
                .map(TeamCitySshKey::isEncrypted)
                .distinct()
                .collect(toList());
        return (encrypted.size() == 1) && encrypted.get(0);
    }
}
