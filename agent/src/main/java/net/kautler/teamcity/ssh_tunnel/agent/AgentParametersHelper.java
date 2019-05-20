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

package net.kautler.teamcity.ssh_tunnel.agent;

import jetbrains.buildServer.agent.ssh.AgentRunningBuildSshKeyManager;
import jetbrains.buildServer.ssh.TeamCitySshKey;
import net.kautler.teamcity.ssh_tunnel.common.ParametersHelper;
import org.jetbrains.annotations.NotNull;

public class AgentParametersHelper extends ParametersHelper {
    @NotNull
    private final AgentRunningBuildSshKeyManager sshKeyManager;

    public AgentParametersHelper(@NotNull AgentRunningBuildSshKeyManager sshKeyManager) {
        this.sshKeyManager = sshKeyManager;
    }

    @Override
    protected boolean isValidKeyName(String sshKeyName) {
        return sshKeyManager.getKey(sshKeyName) != null;
    }

    @Override
    protected boolean isEncrypted(String sshKeyName) {
        TeamCitySshKey sshKey = sshKeyManager.getKey(sshKeyName);
        return (sshKey != null) && sshKey.isEncrypted();
    }
}
