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

package net.kautler.teamcity.ssh_tunnel.common.model;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static jetbrains.buildServer.util.StringUtil.isEmptyOrSpaces;

public final class Connection {
    private final String user;
    private final String sshKey;
    private final String sshKeyPassphrase;
    private final String host;
    private final String port;

    public Connection(String user, String sshKey, String sshKeyPassphrase, String host, String port) {
        Objects.requireNonNull(user, "'user' must not be 'null'");
        Objects.requireNonNull(sshKey, "'sshKey' must not be 'null'");
        Objects.requireNonNull(host, "'host' must not be 'null'");
        this.sshKey = sshKey;
        this.sshKeyPassphrase = sshKeyPassphrase;
        this.user = user;
        this.host = host;
        this.port = isEmptyOrSpaces(port) ? "22" : port;
    }

    public String getUser() {
        return user;
    }

    public String getSshKey() {
        return sshKey;
    }

    public String getSshKeyPassphrase() {
        return sshKeyPassphrase;
    }

    public String getHost() {
        return host;
    }

    public String getPort() {
        return port;
    }

    public Map<String, String> getConfigParameters(String prefix, boolean emulationMode) {
        Map<String, String> result = new HashMap<>();
        result.put(prefix + "user", user);
        result.put(prefix + "sshKey", sshKey);
        result.put(prefix + "host", host);
        result.put(prefix + "port", port);
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if ((o == null) || (getClass() != o.getClass())) {
            return false;
        }
        Connection that = (Connection) o;
        return Objects.equals(user, that.user)
                && Objects.equals(sshKey, that.sshKey)
                && Objects.equals(sshKeyPassphrase, that.sshKeyPassphrase)
                && Objects.equals(host, that.host)
                && Objects.equals(port, that.port);
    }

    @Override
    public int hashCode() {
        return Objects.hash(user, sshKey, sshKeyPassphrase, host, port);
    }

    @Override
    public String toString() {
        return String.format("%s@%s:%s", user, host, port);
    }
}
