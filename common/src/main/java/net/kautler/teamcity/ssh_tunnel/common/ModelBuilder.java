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

package net.kautler.teamcity.ssh_tunnel.common;

import net.kautler.teamcity.ssh_tunnel.common.model.AddressPortPart;
import net.kautler.teamcity.ssh_tunnel.common.model.Connection;
import net.kautler.teamcity.ssh_tunnel.common.model.Part;
import net.kautler.teamcity.ssh_tunnel.common.model.SocketPart;
import net.kautler.teamcity.ssh_tunnel.common.model.SshTunnel;

import java.util.Map;

import static net.kautler.teamcity.ssh_tunnel.common.Constants.ADDRESS_PORT_PART_NAME;
import static net.kautler.teamcity.ssh_tunnel.common.Constants.HOST_PROPERTY_NAME;
import static net.kautler.teamcity.ssh_tunnel.common.Constants.LOCAL_ADDRESS_PROPERTY_NAME;
import static net.kautler.teamcity.ssh_tunnel.common.Constants.LOCAL_PART_PROPERTY_NAME;
import static net.kautler.teamcity.ssh_tunnel.common.Constants.LOCAL_PORT_PROPERTY_NAME;
import static net.kautler.teamcity.ssh_tunnel.common.Constants.LOCAL_SOCKET_PROPERTY_NAME;
import static net.kautler.teamcity.ssh_tunnel.common.Constants.NAME_PROPERTY_NAME;
import static net.kautler.teamcity.ssh_tunnel.common.Constants.PORT_PROPERTY_NAME;
import static net.kautler.teamcity.ssh_tunnel.common.Constants.REMOTE_ADDRESS_PROPERTY_NAME;
import static net.kautler.teamcity.ssh_tunnel.common.Constants.REMOTE_PART_PROPERTY_NAME;
import static net.kautler.teamcity.ssh_tunnel.common.Constants.REMOTE_PORT_PROPERTY_NAME;
import static net.kautler.teamcity.ssh_tunnel.common.Constants.REMOTE_SOCKET_PROPERTY_NAME;
import static net.kautler.teamcity.ssh_tunnel.common.Constants.SOCKET_PART_NAME;
import static net.kautler.teamcity.ssh_tunnel.common.Constants.SSH_KEY_PASSPHRASE_PROPERTY_NAME;
import static net.kautler.teamcity.ssh_tunnel.common.Constants.SSH_KEY_PROPERTY_NAME;
import static net.kautler.teamcity.ssh_tunnel.common.Constants.USER_PROPERTY_NAME;

public class ModelBuilder {
    public static SshTunnel buildSshTunnel(Map<String, String> params) {
        Part remotePart;
        switch (params.getOrDefault(REMOTE_PART_PROPERTY_NAME, "")) {
            case ADDRESS_PORT_PART_NAME:
                String address = params.get(REMOTE_ADDRESS_PROPERTY_NAME);
                String port = params.get(REMOTE_PORT_PROPERTY_NAME);
                remotePart = new AddressPortPart(address, port);
                break;

            case SOCKET_PART_NAME:
                String socket = params.get(REMOTE_SOCKET_PROPERTY_NAME);
                remotePart = new SocketPart(socket);
                break;

            default:
                throw new AssertionError("missing case: " + params.get(REMOTE_PART_PROPERTY_NAME));
        }

        Part localPart;
        switch (params.getOrDefault(LOCAL_PART_PROPERTY_NAME, "")) {
            case ADDRESS_PORT_PART_NAME:
                String address = params.get(LOCAL_ADDRESS_PROPERTY_NAME);
                String port = params.get(LOCAL_PORT_PROPERTY_NAME);
                localPart = new AddressPortPart(address, port);
                break;

            case SOCKET_PART_NAME:
                String socket = params.get(LOCAL_SOCKET_PROPERTY_NAME);
                localPart = new SocketPart(socket);
                break;

            default:
                localPart = null;
                break;
        }

        String name = params.get(NAME_PROPERTY_NAME);

        String user = params.get(USER_PROPERTY_NAME);
        String sshKey = params.get(SSH_KEY_PROPERTY_NAME);
        String sshKeyPassphrase = params.get(SSH_KEY_PASSPHRASE_PROPERTY_NAME);
        String host = params.get(HOST_PROPERTY_NAME);
        String port = params.get(PORT_PROPERTY_NAME);
        Connection connection = new Connection(user, sshKey, sshKeyPassphrase, host, port);

        return new SshTunnel(name, connection, localPart, remotePart);
    }
}
