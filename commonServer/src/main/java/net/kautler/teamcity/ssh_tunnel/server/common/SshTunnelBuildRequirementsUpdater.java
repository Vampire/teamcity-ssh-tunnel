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

import jetbrains.buildServer.serverSide.BuildTypeIdentity;
import jetbrains.buildServer.serverSide.BuildTypeSettings;
import jetbrains.buildServer.serverSide.BuildTypeTemplate;
import jetbrains.buildServer.serverSide.ProjectManager;
import jetbrains.buildServer.serverSide.ProjectsModelListener;
import jetbrains.buildServer.serverSide.ProjectsModelListenerAdapter;
import jetbrains.buildServer.serverSide.SBuildServer;
import jetbrains.buildServer.serverSide.SBuildType;
import jetbrains.buildServer.serverSide.SProject;
import jetbrains.buildServer.users.SUser;
import jetbrains.buildServer.util.EventDispatcher;
import jetbrains.buildServer.vcs.VcsRootInstance;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.InitializingBean;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.lang.Integer.parseInt;
import static net.kautler.teamcity.ssh_tunnel.common.Constants.BUILD_FEATURE_TYPE;
import static net.kautler.teamcity.ssh_tunnel.common.Constants.MIN_VERSION_SUPPORTING_BUILD_FEATURE_REQUIREMENTS;
import static net.kautler.teamcity.ssh_tunnel.common.Constants.SSH_EXECUTABLE_CONFIGURATION_PARAMETER_NAME;
import static net.kautler.teamcity.ssh_tunnel.common.Constants.SSH_TUNNEL_REQUIREMENT_PROPERTY_NAME;

public class SshTunnelBuildRequirementsUpdater extends ProjectsModelListenerAdapter implements InitializingBean {
    @NotNull
    private final EventDispatcher<ProjectsModelListener> projectsModelEventDispatcher;

    @NotNull
    private final ProjectManager projectManager;

    @NotNull
    private final SBuildServer buildServer;

    private final List<BuildTypeSettings> recursionLocks = new ArrayList<>();

    public SshTunnelBuildRequirementsUpdater(@NotNull EventDispatcher<ProjectsModelListener> projectsModelEventDispatcher,
                                             @NotNull ProjectManager projectManager,
                                             @NotNull SBuildServer buildServer) {
        this.projectsModelEventDispatcher = projectsModelEventDispatcher;
        this.projectManager = projectManager;
        this.buildServer = buildServer;
    }

    @Override
    public void afterPropertiesSet() {
        // part of work-around for missing BuildFeature#getRequirements
        if (parseInt(buildServer.getBuildNumber()) < MIN_VERSION_SUPPORTING_BUILD_FEATURE_REQUIREMENTS) {
            projectsModelEventDispatcher.addListener(this);
        }
    }

    @Override
    public void projectRestored(@NotNull String projectId) {
        SProject project = projectManager.getProjects().stream()
                .filter(p -> projectId.equals(p.getProjectId()))
                .findAny()
                .orElseThrow(AssertionError::new);
        project.getBuildTypeTemplates().forEach(this::buildTypeSettingsPersisted);
        project.getBuildTypes().forEach(this::buildTypeSettingsPersisted);
    }

    @Override
    public void buildTypeTemplatePersisted(@NotNull BuildTypeTemplate buildTypeTemplate) {
        buildTypeSettingsPersisted(buildTypeTemplate);
    }

    @Override
    public void buildTypePersisted(@NotNull SBuildType buildType) {
        buildTypeSettingsPersisted(buildType);
    }

    private <T extends BuildTypeSettings & BuildTypeIdentity> void buildTypeSettingsPersisted(@NotNull T buildTypeSettings) {
        if (recursionLocks.contains(buildTypeSettings)) {
            return;
        }
        buildTypeSettings.getBuildFeaturesOfType(BUILD_FEATURE_TYPE).forEach(buildFeature -> {
            Map<String, String> parameters = buildFeature.getParameters();
            String sshTunnelRequirementPropertyValue = '%' + SSH_EXECUTABLE_CONFIGURATION_PARAMETER_NAME + '%';
            if (!sshTunnelRequirementPropertyValue.equals(parameters.get(SSH_TUNNEL_REQUIREMENT_PROPERTY_NAME))) {
                Map<String, String> correctedParameters = new HashMap<>(parameters);
                correctedParameters.put(SSH_TUNNEL_REQUIREMENT_PROPERTY_NAME, sshTunnelRequirementPropertyValue);
                buildTypeSettings.updateBuildFeature(buildFeature.getId(), buildFeature.getType(), correctedParameters);
                persistBuildTypeSettings(buildTypeSettings, String.format(
                        "Add build requirement for configuration parameter '%s' to build configuration '%s'",
                        SSH_EXECUTABLE_CONFIGURATION_PARAMETER_NAME,
                        buildTypeSettings.getName()));
            }
        });
    }

    private void persistBuildTypeSettings(@NotNull BuildTypeSettings buildTypeSettings, String description) {
        recursionLocks.add(buildTypeSettings);
        try {
            SProject buildTypeSettingsProject = buildTypeSettings.getProject();
            buildTypeSettings.persist(new ConfigAction(description, buildTypeSettingsProject));
        } finally {
            recursionLocks.remove(buildTypeSettings);
        }
    }

    private static class ConfigAction implements jetbrains.buildServer.serverSide.ConfigAction {
        private final String description;
        private final SProject buildTypeSettingsProject;
        private String cancelReason;

        public ConfigAction(String description, SProject buildTypeSettingsProject) {
            this.description = description;
            this.buildTypeSettingsProject = buildTypeSettingsProject;
        }

        @Override
        public long getId() {
            return -1;
        }

        @NotNull
        @Override
        public String getUserName(@NotNull VcsRootInstance root) {
            return "ssh-tunnel";
        }

        @NotNull
        @Override
        public String getDescription() {
            return description;
        }

        @Nullable
        @Override
        public SProject getProject() {
            return buildTypeSettingsProject;
        }

        @Nullable
        @Override
        public SUser getUser() {
            return null;
        }

        @Override
        public void cancelCommit(@NotNull String reason) {
            cancelReason = reason;
        }

        @Nullable
        @Override
        public String getCommitCancelReason() {
            return cancelReason;
        }

        @NotNull
        @Override
        public String describe(boolean verbose) {
            return description;
        }
    }
}
