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

import java.util.Map;
import java.util.Objects;

import static java.util.Collections.singletonMap;

public final class SocketPart implements Part {
    private final String socket;

    public SocketPart(String socket) {
        Objects.requireNonNull(socket, "'socket' must not be 'null'");
        this.socket = socket;
    }

    public String getSocket() {
        return socket;
    }

    @Override
    public Map<String, String> getConfigParameters(String prefix, boolean emulationMode) {
        return singletonMap(prefix + "socket", socket);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if ((o == null) || (getClass() != o.getClass())) {
            return false;
        }
        SocketPart that = (SocketPart) o;
        return Objects.equals(socket, that.socket);
    }

    @Override
    public int hashCode() {
        return Objects.hash(socket);
    }

    @Override
    public String toString() {
        return socket;
    }
}
