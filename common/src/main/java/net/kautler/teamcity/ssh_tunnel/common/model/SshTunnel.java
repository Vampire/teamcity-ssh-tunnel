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

import java.util.Formattable;
import java.util.Formatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static java.util.FormattableFlags.ALTERNATE;
import static java.util.FormattableFlags.LEFT_JUSTIFY;
import static java.util.FormattableFlags.UPPERCASE;
import static jetbrains.buildServer.util.StringUtil.replaceNonAlphaNumericChars;

public class SshTunnel implements Formattable {
    private final String name;
    private final Connection connection;
    private final Part localPart;
    private final Part remotePart;

    public SshTunnel(String name, Connection connection, Part localPart, Part remotePart) {
        Objects.requireNonNull(name, "'name' must not be 'null'");
        Objects.requireNonNull(connection, "'connection' must not be 'null'");
        Objects.requireNonNull(remotePart, "'remotePart' must not be 'null'");
        this.name = name;
        this.connection = connection;
        this.localPart = localPart == null ? new AddressPortPart() : localPart;
        this.remotePart = remotePart;
    }

    public String getName() {
        return name;
    }

    public Connection getConnection() {
        return connection;
    }

    public Part getLocalPart() {
        return localPart;
    }

    public Part getRemotePart() {
        return remotePart;
    }

    public Map<String, String> getConfigParameters() {
        return getConfigParameters(false);
    }

    public Map<String, String> getConfigParameters(boolean emulationMode) {
        String prefix = String.format("sshTunnel.%s.", replaceNonAlphaNumericChars(name, '_'));
        Map<String, String> result = new HashMap<>();
        result.putAll(connection.getConfigParameters(prefix + "connection.", emulationMode));
        result.putAll(localPart.getConfigParameters(prefix + "local.", emulationMode));
        result.putAll(remotePart.getConfigParameters(prefix + "remote.", emulationMode));
        return result;
    }

    @Override
    public void formatTo(Formatter formatter, int flags, int width, int precision) {
        boolean alternate = (flags & ALTERNATE) != 0;
        boolean uppercase = (flags & UPPERCASE) != 0;
        boolean leftJustify = (flags & LEFT_JUSTIFY) != 0;

        String result = alternate ? String.format("%#s:%#s", localPart, remotePart) : toString();
        if (uppercase) {
            result = result.toUpperCase(formatter.locale());
        }

        formatter.format(String.format("%%%s%s%ss",
                leftJustify ? "-" : "",
                (width == -1) ? "" : width,
                (precision == -1) ? "" : "." + precision), result);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if ((o == null) || (getClass() != o.getClass())) {
            return false;
        }
        SshTunnel sshTunnel = (SshTunnel) o;
        return Objects.equals(name, sshTunnel.name)
                && Objects.equals(connection, sshTunnel.connection)
                && Objects.equals(localPart, sshTunnel.localPart)
                && Objects.equals(remotePart, sshTunnel.remotePart);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, connection, localPart, remotePart);
    }

    @Override
    public String toString() {
        return String.format("Open an SSH tunnel via '%s' identified by key '%s' from '%s' to '%s' with name '%s'",
                connection, connection.getSshKey(), localPart, remotePart, name);
    }
}
