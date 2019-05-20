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

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.Formatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

import static java.lang.Integer.parseInt;
import static java.util.FormattableFlags.ALTERNATE;
import static java.util.FormattableFlags.LEFT_JUSTIFY;
import static java.util.FormattableFlags.UPPERCASE;
import static jetbrains.buildServer.util.StringUtil.isEmptyOrSpaces;

public final class AddressPortPart implements Part {
    private final String address;
    private final String port;
    private final AtomicInteger randomPort = new AtomicInteger();

    public AddressPortPart() {
        this(null, null);
    }

    public AddressPortPart(String address, String port) {
        this.address = isEmptyOrSpaces(address) ? "127.0.0.1" : address;
        this.port = isEmptyOrSpaces(port) ? null : port;
    }

    public String getAddress() {
        return address;
    }

    public int getPort() {
        if (port == null) {
            return randomPort.updateAndGet(port -> {
                if (port == 0) {
                    try (ServerSocket serverSocket = new ServerSocket(0, 0, InetAddress.getByName(address))) {
                        return serverSocket.getLocalPort();
                    } catch (IOException ioe) {
                        throw new RuntimeException("could not determine dynamic local port", ioe);
                    }
                } else {
                    return port;
                }
            });
        } else {
            return parseInt(port);
        }
    }

    @Override
    public Map<String, String> getConfigParameters(String prefix, boolean emulationMode) {
        Map<String, String> result = new HashMap<>();
        result.put(prefix + "address", address);
        result.put(prefix + "port", (emulationMode && (port == null)) ? "0" : String.valueOf(getPort()));
        return result;
    }

    @Override
    public void formatTo(Formatter formatter, int flags, int width, int precision) {
        boolean alternate = (flags & ALTERNATE) != 0;
        boolean uppercase = (flags & UPPERCASE) != 0;
        boolean leftJustify = (flags & LEFT_JUSTIFY) != 0;

        String result = alternate ? String.format("%s:%d", address, getPort()) : toString();
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
        AddressPortPart that = (AddressPortPart) o;
        return Objects.equals(address, that.address)
                && Objects.equals(port, that.port);
    }

    @Override
    public int hashCode() {
        return Objects.hash(address, port);
    }

    @Override
    public String toString() {
        return String.format("%s:%s", address, port == null ? "<random port>" : port);
    }
}
