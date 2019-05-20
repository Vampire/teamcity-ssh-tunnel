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

import jetbrains.buildServer.requirements.Requirement;
import jetbrains.buildServer.serverSide.BuildFeature;
import jetbrains.buildServer.serverSide.InvalidProperty;
import jetbrains.buildServer.serverSide.PropertiesProcessor;
import jetbrains.buildServer.web.openapi.PluginDescriptor;
import net.kautler.teamcity.ssh_tunnel.common.ParametersHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static java.util.Collections.singleton;
import static java.util.stream.Collectors.toList;
import static jetbrains.buildServer.requirements.RequirementType.EXISTS;
import static net.kautler.teamcity.ssh_tunnel.common.Constants.BUILD_FEATURE_TYPE;
import static net.kautler.teamcity.ssh_tunnel.common.Constants.SSH_EXECUTABLE_CONFIGURATION_PARAMETER_NAME;
import static net.kautler.teamcity.ssh_tunnel.common.Constants.SSH_EXECUTABLE_REQUIREMENT_ID;

public class SshTunnelBuildFeature extends BuildFeature {
    @NotNull
    private final ParametersHelper parametersHelper;

    private final String editParametersUrl;

    public SshTunnelBuildFeature(@NotNull ParametersHelper parametersHelper, @NotNull PluginDescriptor descriptor) {
        this.parametersHelper = parametersHelper;
        editParametersUrl = descriptor.getPluginResourcesPath("sshTunnelBuildFeature.jsp");
    }

    @NotNull
    @Override
    public String getType() {
        return BUILD_FEATURE_TYPE;
    }

    @NotNull
    @Override
    public String getDisplayName() {
        return "SSH Tunnel";
    }

    @Nullable
    @Override
    public String getEditParametersUrl() {
        return editParametersUrl;
    }

    @NotNull
    @Override
    public String describeParameters(@NotNull Map<String, String> params) {
        return parametersHelper.describeParameters(new HashMap<>(params));
    }

    @Nullable
    @Override
    public PropertiesProcessor getParametersProcessor() {
        return properties -> parametersHelper.validate(properties).entrySet().stream()
                .map(entry -> new InvalidProperty(entry.getKey(), String.join("; ", entry.getValue())))
                .collect(toList());
    }

    @NotNull
    @Override
    // This was added in 2019.1, in previous versions this method should simply be ignored
    // and a work-around to add this requirement is in effect in this plugin
    public Collection<Requirement> getRequirements(Map<String, String> params) {
        return singleton(new Requirement(SSH_EXECUTABLE_REQUIREMENT_ID, SSH_EXECUTABLE_CONFIGURATION_PARAMETER_NAME, null, EXISTS));
    }
}
