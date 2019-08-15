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

import jetbrains.buildServer.agent.AgentLifeCycleAdapter;
import jetbrains.buildServer.agent.AgentLifeCycleListener;
import jetbrains.buildServer.agent.BuildAgent;
import jetbrains.buildServer.log.Loggers;
import jetbrains.buildServer.util.EventDispatcher;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static java.lang.Runtime.getRuntime;
import static java.lang.System.getenv;
import static java.lang.System.nanoTime;
import static java.lang.Thread.currentThread;
import static java.nio.file.FileVisitResult.CONTINUE;
import static java.nio.file.FileVisitResult.TERMINATE;
import static java.nio.file.Files.walkFileTree;
import static java.util.Locale.ENGLISH;
import static jetbrains.buildServer.util.StringUtil.isEmptyOrSpaces;
import static net.kautler.teamcity.ssh_tunnel.common.Constants.SSH_EXECUTABLE_CONFIGURATION_PARAMETER_NAME;
import static net.kautler.teamcity.ssh_tunnel.common.Constants.SSH_EXECUTABLE_ENVIRONMENT_VARIABLE_NAME;
import static net.kautler.teamcity.ssh_tunnel.common.Util.streamToString;

public class SshDetector extends AgentLifeCycleAdapter implements InitializingBean {
    private static final Logger LOG = LoggerFactory.getLogger(Loggers.AGENT_CATEGORY + '.' + SshDetector.class.getName());
    private static final PathMatcher SSH_FILE_NAME_MATCHER = FileSystems.getDefault().getPathMatcher("glob:ssh{,.*}");

    @NotNull
    private final EventDispatcher<AgentLifeCycleListener> agentLifeCycleEventDispatcher;

    public SshDetector(@NotNull EventDispatcher<AgentLifeCycleListener> agentLifeCycleEventDispatcher) {
        this.agentLifeCycleEventDispatcher = agentLifeCycleEventDispatcher;
    }

    @Override
    public void afterPropertiesSet() {
        agentLifeCycleEventDispatcher.addListener(this);
    }

    @Override
    public void beforeAgentConfigurationLoaded(@NotNull BuildAgent agent) {
        Stream.<Supplier<String>>of(
                () -> agent.getConfiguration().getConfigurationParameters().get(SSH_EXECUTABLE_CONFIGURATION_PARAMETER_NAME),
                () -> System.getProperty(SSH_EXECUTABLE_CONFIGURATION_PARAMETER_NAME),
                () -> getenv().get(SSH_EXECUTABLE_ENVIRONMENT_VARIABLE_NAME),
                () -> Stream.of("ssh", "/usr/local/bin/ssh", "/usr/sbin/ssh", "/sbin/ssh", "/usr/bin/ssh", "/bin/ssh")
                        .filter(SshDetector::sshAvailableAtPath)
                        .findAny()
                        .orElse(null))
                .map(Supplier::get)
                .filter(Objects::nonNull)
                .findFirst()
                .map(possibleSshExecutable -> {
                    if (!possibleSshExecutable.equals("auto")) {
                        return possibleSshExecutable;
                    }
                    LOG.warn("SSH executable is configured to value 'auto'. " +
                                    "Going to search the whole file tree for an SSH executable. " +
                                    "This might constitute a serious security risk if malicious builds " +
                                    "leave a fake ssh executable on the file system that could steal your " +
                                    "SSH key. Do not enable this if you run untrusted builds. " +
                                    "Additionally this is a pretty heavy operation and will " +
                                    "significantly slow down the agent startup. " +
                                    "To prevent this or if the wrong one is found, " +
                                    "you can configure the path to the SSH executable " +
                                    "using an agent configuration property or system property named '{}' " +
                                    "or an environment variable named '{}'.",
                            SSH_EXECUTABLE_CONFIGURATION_PARAMETER_NAME, SSH_EXECUTABLE_ENVIRONMENT_VARIABLE_NAME);
                    AtomicLong searchedFiles = new AtomicLong();
                    AtomicLong nextLog = new AtomicLong(nanoTime() + 5_000_000_000L);
                    return StreamSupport.stream(FileSystems.getDefault().getRootDirectories().spliterator(), true)
                            .flatMap(rootDirectory -> {
                                try {
                                    AtomicReference<Optional<String>> sshExecutable = new AtomicReference<>(Optional.empty());
                                    walkFileTree(rootDirectory, new FileVisitor<Path>() {
                                        @Override
                                        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                                            return CONTINUE;
                                        }

                                        @Override
                                        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                                            // log progress every 5 seconds
                                            if (nanoTime() >= nextLog.get()) {
                                                nextLog.addAndGet(5_000_000_000L);
                                                LOG.info("Still walking the file tree. Searched path: {}. Current path: {}", searchedFiles.get(), file);
                                            }
                                            searchedFiles.incrementAndGet();
                                            if (!attrs.isDirectory()
                                                    && SSH_FILE_NAME_MATCHER.matches(file.getFileName())
                                                    && file.toFile().canExecute()
                                                    && sshAvailableAtPath(file.toString())) {
                                                sshExecutable.set(Optional.of(file.toString()));
                                                return TERMINATE;
                                            } else {
                                                return CONTINUE;
                                            }
                                        }

                                        @Override
                                        public FileVisitResult visitFileFailed(Path file, IOException exc) {
                                            logVisitException(file, exc);
                                            return CONTINUE;
                                        }

                                        @Override
                                        public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
                                            logVisitException(dir, exc);
                                            return CONTINUE;
                                        }

                                        private void logVisitException(Path path, IOException exc) {
                                            if (exc != null) {
                                                if (LOG.isDebugEnabled()) {
                                                    LOG.info("Exception during walking the file tree for '{}' at {}", rootDirectory, path, exc);
                                                } else {
                                                    LOG.debug("Exception during walking the file tree for '{}' at {}: {}", rootDirectory, path, exc.getMessage());
                                                }
                                            }
                                        }
                                    });
                                    return sshExecutable.get().map(Stream::of).orElseGet(Stream::empty);
                                } catch (IOException ioe) {
                                    if (LOG.isDebugEnabled()) {
                                        LOG.info("Exception during walking the file tree for '{}'", rootDirectory, ioe);
                                    } else {
                                        LOG.debug("Exception during walking the file tree for '{}': {}", rootDirectory, ioe.getMessage());
                                    }
                                    return Stream.empty();
                                }
                            })
                            .findAny()
                            .orElseGet(() -> {
                                LOG.info("SSH executable is configured to value 'auto', but no SSH executable was found in the file tree");
                                return null;
                            });
                })
                .ifPresent(sshExecutable -> agent.getConfiguration().addConfigurationParameter(SSH_EXECUTABLE_CONFIGURATION_PARAMETER_NAME, sshExecutable));
    }

    private static boolean sshAvailableAtPath(String sshPath) {
        LOG.info("Testing potential ssh path {}", sshPath);
        try {
            Process process = getRuntime().exec(new String[] { sshPath, "-V" });
            int exitValue = process.waitFor();
            String stdout = streamToString(process.getInputStream());
            if (!isEmptyOrSpaces(stdout)) {
                LOG.info("stdout ({}): {}", sshPath, stdout);
            }
            String stderr = streamToString(process.getErrorStream());
            if (!isEmptyOrSpaces(stderr)) {
                LOG.info("stderr ({}): {}", sshPath, stderr);
            }
            if ((exitValue == 0) && stderr.toLowerCase(ENGLISH).contains("ssh")) {
                LOG.info("SSH executable found at {}", sshPath);
                return true;
            }
        } catch (InterruptedException ie) {
            currentThread().interrupt();
        } catch (IOException ioe) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Exception during testing potential ssh path {}", sshPath, ioe);
            } else {
                LOG.info("Exception during testing potential ssh path {}: {}", sshPath, ioe.getMessage());
            }
        }
        LOG.info("SSH executable not found at {}", sshPath);
        return false;
    }
}
