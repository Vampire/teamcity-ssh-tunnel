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

import jetbrains.buildServer.agent.AgentBuildFeature;
import jetbrains.buildServer.agent.AgentLifeCycleAdapter;
import jetbrains.buildServer.agent.AgentLifeCycleListener;
import jetbrains.buildServer.agent.AgentRunningBuild;
import jetbrains.buildServer.agent.BuildFinishedStatus;
import jetbrains.buildServer.agent.BuildProgressLogger;
import jetbrains.buildServer.agent.ssh.AgentRunningBuildSshKeyManager;
import jetbrains.buildServer.ssh.AskPassGenerator;
import jetbrains.buildServer.ssh.AskPassGeneratorUnix;
import jetbrains.buildServer.ssh.AskPassGeneratorWin;
import jetbrains.buildServer.ssh.TeamCitySshKey;
import jetbrains.buildServer.util.EventDispatcher;
import jetbrains.buildServer.util.FileUtil;
import jetbrains.buildServer.util.MultiMap;
import net.kautler.teamcity.ssh_tunnel.common.ModelBuilder;
import net.kautler.teamcity.ssh_tunnel.common.ParametersHelper;
import net.kautler.teamcity.ssh_tunnel.common.model.Connection;
import net.kautler.teamcity.ssh_tunnel.common.model.SshTunnel;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.InitializingBean;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Stream;

import static com.intellij.openapi.util.text.StringUtil.isNotEmpty;
import static java.lang.Thread.currentThread;
import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static jetbrains.buildServer.ssh.Util.writeKey;
import static jetbrains.buildServer.util.FileUtil.createTempFile;
import static jetbrains.buildServer.util.FileUtil.readText;
import static jetbrains.buildServer.util.StringUtil.isEmptyOrSpaces;
import static net.kautler.teamcity.ssh_tunnel.common.Constants.BUILD_FEATURE_ACTIVITY_TYPE;
import static net.kautler.teamcity.ssh_tunnel.common.Constants.BUILD_FEATURE_TYPE;
import static net.kautler.teamcity.ssh_tunnel.common.Constants.SSH_EXECUTABLE_CONFIGURATION_PARAMETER_NAME;

public class SshTunnelBuildFeatureAgentPart extends AgentLifeCycleAdapter implements InitializingBean {
    private static final String TUNNEL_ACTIVITY_PATTERN = "Tunnel via '%s' identified by key '%s'";
    private static final String TUNNEL_NOT_ESTABLISHED = "SSH Tunnel could not be established";
    private static final String TUNNEL_NOT_TERMINATED = "SSH Tunnel could not be terminated";

    @NotNull
    private final EventDispatcher<AgentLifeCycleListener> agentLifeCycleEventDispatcher;

    @NotNull
    private final AgentRunningBuildSshKeyManager agentRunningBuildSshKeyManager;

    @NotNull
    private final ParametersHelper parametersHelper;

    private final ConcurrentMap<AgentRunningBuild, List<SshTunnel>> sshTunnelsPerBuild = new ConcurrentHashMap<>();
    private final ConcurrentMap<AgentRunningBuild, Map<Connection, Process>> processesPerBuild = new ConcurrentHashMap<>();
    private final ConcurrentMap<AgentRunningBuild, List<File>> filesToDeletePerBuild = new ConcurrentHashMap<>();
    private final ConcurrentMap<Process, File> stdoutPerProcess = new ConcurrentHashMap<>();
    private final ConcurrentMap<Process, File> stderrPerProcess = new ConcurrentHashMap<>();

    public SshTunnelBuildFeatureAgentPart(@NotNull EventDispatcher<AgentLifeCycleListener> agentLifeCycleEventDispatcher,
                                          @NotNull AgentRunningBuildSshKeyManager agentRunningBuildSshKeyManager,
                                          @NotNull ParametersHelper parametersHelper) {
        this.agentLifeCycleEventDispatcher = agentLifeCycleEventDispatcher;
        this.agentRunningBuildSshKeyManager = agentRunningBuildSshKeyManager;
        this.parametersHelper = parametersHelper;
    }

    @Override
    public void afterPropertiesSet() {
        agentLifeCycleEventDispatcher.addListener(this);
    }

    @Override
    public void buildStarted(@NotNull AgentRunningBuild runningBuild) {
        sshTunnelsPerBuild.computeIfAbsent(runningBuild, this::getSshTunnels).stream()
                .map(SshTunnel::getConfigParameters)
                .map(Map::entrySet)
                .flatMap(Collection::stream)
                .collect(toMap(Map.Entry::getKey, Map.Entry::getValue))
                .forEach(runningBuild::addSharedConfigParameter);
    }

    @Override
    public void preparationFinished(@NotNull AgentRunningBuild runningBuild) {
        BuildProgressLogger buildLogger = runningBuild.getBuildLogger();
        buildLogger.activityStarted("Establishing SSH Tunnels", BUILD_FEATURE_ACTIVITY_TYPE);
        try {
            MultiMap<Connection, SshTunnel> sshTunnelsPerConnection = sshTunnelsPerBuild.remove(runningBuild).stream()
                    .collect(MultiMap::new,
                            (map, sshTunnel) -> map.putValue(sshTunnel.getConnection(), sshTunnel),
                            (map1, map2) -> map2.entrySet().forEach(entry -> entry.getValue().forEach(value -> map1.putValue(entry.getKey(), value))));

            String sshExecutable = runningBuild.getSharedConfigParameters().get(SSH_EXECUTABLE_CONFIGURATION_PARAMETER_NAME);
            sshTunnelsPerConnection.entrySet().forEach(entry -> {
                Connection connection = entry.getKey();
                buildLogger.activityStarted(String.format(TUNNEL_ACTIVITY_PATTERN, connection, connection.getSshKey()), BUILD_FEATURE_ACTIVITY_TYPE);
                try {
                    List<SshTunnel> forwards = entry.getValue();

                    forwards.forEach(forward -> buildLogger.progressMessage(String.format("Forwarding '%#s'", forward)));

                    List<String> command = new ArrayList<>();
                    command.add(sshExecutable);
                    command.add("-N");
                    forwards.stream()
                            .flatMap(forward -> Stream.of("-L", String.format("%#s", forward)))
                            .forEachOrdered(command::add);
                    command.add("-p");
                    command.add(connection.getPort());
                    command.add("-l");
                    command.add(connection.getUser());
                    command.add("-i");
                    TeamCitySshKey sshKey = agentRunningBuildSshKeyManager.getKey(connection.getSshKey());
                    if (sshKey == null) {
                        throw new RuntimeException(String.format("SSH Key '%s' not found", connection.getSshKey()));
                    }
                    File buildTempDirectory = runningBuild.getBuildTempDirectory();
                    File sshKeyFile = new File(writeKey(buildTempDirectory, sshKey));
                    filesToDeletePerBuild.computeIfAbsent(runningBuild, key -> new ArrayList<>()).add(sshKeyFile);
                    command.add(sshKeyFile.getName());
                    command.add(connection.getHost());

                    ProcessBuilder processBuilder = new ProcessBuilder(command).directory(sshKeyFile.getParentFile());

                    if (isNotEmpty(connection.getSshKeyPassphrase())) {
                        AskPassGenerator askPassGenerator = runningBuild.getAgentConfiguration().getSystemInfo().isWindows() ? new AskPassGeneratorWin() : new AskPassGeneratorUnix();
                        File askPassFile = askPassGenerator.generate(buildTempDirectory, connection.getSshKeyPassphrase());
                        filesToDeletePerBuild.get(runningBuild).add(askPassFile);
                        processBuilder.environment().put("SSH_ASKPASS", "./" + askPassFile.getName());
                    }

                    File stdout = createTempFile(buildTempDirectory, "teamcity", "sshTunnelStdout", true);
                    filesToDeletePerBuild.get(runningBuild).add(stdout);
                    File stderr = createTempFile(buildTempDirectory, "teamcity", "sshTunnelStderr", true);
                    filesToDeletePerBuild.get(runningBuild).add(stderr);
                    Process process = processBuilder.redirectOutput(stdout).redirectError(stderr).start();
                    processesPerBuild.computeIfAbsent(runningBuild, key -> new HashMap<>()).put(connection, process);
                    stdoutPerProcess.put(process, stdout);
                    stderrPerProcess.put(process, stderr);
                    try {
                        process.waitFor(1, SECONDS);
                    } catch (InterruptedException ie) {
                        currentThread().interrupt();
                    }
                    if (!process.isAlive()) {
                        buildLogger.buildFailureDescription(TUNNEL_NOT_ESTABLISHED);
                        runningBuild.stopBuild(TUNNEL_NOT_ESTABLISHED);
                    }
                } catch (IOException ioe) {
                    throw new RuntimeException(ioe.getMessage(), ioe);
                } finally {
                    buildLogger.activityFinished(String.format(TUNNEL_ACTIVITY_PATTERN, connection, connection.getSshKey()), BUILD_FEATURE_ACTIVITY_TYPE);
                }
            });
        } catch (Exception e) {
            String message = e.getMessage();
            buildLogger.internalError(TUNNEL_NOT_ESTABLISHED, (message == null) ? "" : message, e);
            buildLogger.buildFailureDescription(TUNNEL_NOT_ESTABLISHED);
            runningBuild.stopBuild(TUNNEL_NOT_ESTABLISHED);
        } finally {
            buildLogger.activityFinished("Establishing SSH Tunnels", BUILD_FEATURE_ACTIVITY_TYPE);
        }
    }

    @Override
    public void beforeBuildFinish(@NotNull AgentRunningBuild build, @NotNull BuildFinishedStatus buildStatus) {
        BuildProgressLogger buildLogger = build.getBuildLogger();
        buildLogger.activityStarted("Terminating SSH Tunnels", BUILD_FEATURE_ACTIVITY_TYPE);
        try {
            Map<Connection, Process> processPerConnection = processesPerBuild.remove(build);
            if (processPerConnection == null) {
                return;
            }
            processPerConnection.forEach((connection, process) -> {
                buildLogger.activityStarted(String.format(TUNNEL_ACTIVITY_PATTERN, connection, connection.getSshKey()), BUILD_FEATURE_ACTIVITY_TYPE);
                try {
                    buildLogger.progressMessage("Terminate SSH Tunnel");
                    if (process.isAlive()) {
                        try {
                            process.destroyForcibly().waitFor();
                        } catch (InterruptedException ie) {
                            currentThread().interrupt();
                        }
                    } else {
                        int exitValue = process.exitValue();
                        if (exitValue != 0) {
                            buildLogger.buildFailureDescription(TUNNEL_NOT_ESTABLISHED);
                            build.stopBuild(TUNNEL_NOT_ESTABLISHED);
                        }
                        buildLogger.progressMessage("exit code: " + exitValue);
                    }
                    String stdout = readText(stdoutPerProcess.remove(process));
                    if (!isEmptyOrSpaces(stdout)) {
                        buildLogger.progressMessage("stdout: " + stdout);
                    }
                    String stderr = readText(stderrPerProcess.remove(process));
                    if (!isEmptyOrSpaces(stderr)) {
                        buildLogger.warning("stderr: " + stderr);
                    }
                } catch (IOException ioe) {
                    throw new RuntimeException(ioe.getMessage(), ioe);
                } finally {
                    buildLogger.activityFinished(String.format(TUNNEL_ACTIVITY_PATTERN, connection, connection.getSshKey()), BUILD_FEATURE_ACTIVITY_TYPE);
                }
            });
            Optional.ofNullable(filesToDeletePerBuild.remove(build)).ifPresent(files -> files.forEach(FileUtil::delete));
        } catch (Exception e) {
            String message = e.getMessage();
            buildLogger.internalError(TUNNEL_NOT_TERMINATED, (message == null) ? "" : message, e);
            buildLogger.buildFailureDescription(TUNNEL_NOT_TERMINATED);
            build.stopBuild(TUNNEL_NOT_TERMINATED);
        } finally {
            buildLogger.activityFinished("Terminating SSH Tunnels", BUILD_FEATURE_ACTIVITY_TYPE);
        }
    }

    private List<SshTunnel> getSshTunnels(@NotNull AgentRunningBuild runningBuild) {
        return runningBuild.getBuildFeaturesOfType(BUILD_FEATURE_TYPE).stream()
                .map(AgentBuildFeature::getParameters)
                .peek(parameters -> {
                    if (!parametersHelper.validate(parameters).isEmpty()) {
                        throw new RuntimeException(parametersHelper.describeParameters(parameters));
                    }
                })
                .map(ModelBuilder::buildSshTunnel)
                .collect(toList());
    }
}
