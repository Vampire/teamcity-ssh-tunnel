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

import jetbrains.buildServer.serverSide.ParametersDescriptor;
import jetbrains.buildServer.serverSide.SBuild;
import jetbrains.buildServer.serverSide.parameters.AbstractBuildParametersProvider;
import net.kautler.teamcity.ssh_tunnel.common.ModelBuilder;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

import static java.util.stream.Collectors.toList;
import static net.kautler.teamcity.ssh_tunnel.common.Constants.BUILD_FEATURE_TYPE;

public class SshTunnelBuildParametersProvider extends AbstractBuildParametersProvider {
    @NotNull
    @Override
    public Collection<String> getParametersAvailableOnAgent(@NotNull SBuild build) {
        return build.getBuildFeaturesOfType(BUILD_FEATURE_TYPE).stream()
                .map(ParametersDescriptor::getParameters)
                .map(ModelBuilder::buildSshTunnel)
                .map(sshTunnel -> sshTunnel.getConfigParameters(true))
                .flatMap(configParameters -> configParameters.keySet().stream())
                .collect(toList());
    }
}
