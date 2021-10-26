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

import java.math.BigInteger;
import java.util.List;

import static java.util.Arrays.asList;

public class Constants {
    public static final String SSH_TUNNEL_REQUIREMENT_PROPERTY_NAME = "sshTunnelRequirement";
    public static final String NAME_PROPERTY_NAME = "name";
    public static final String USER_PROPERTY_NAME = "user";
    public static final String SSH_KEY_PROPERTY_NAME = "teamcitySshKey";
    public static final String SSH_KEY_PASSPHRASE_PROPERTY_NAME = "secure:teamcitySshKeyPassphrase";
    public static final String HOST_PROPERTY_NAME = "host";
    public static final String PORT_PROPERTY_NAME = "port";
    public static final String LOCAL_PART_PROPERTY_NAME = "localPart";
    public static final String LOCAL_ADDRESS_PROPERTY_NAME = "localAddress";
    public static final String LOCAL_PORT_PROPERTY_NAME = "localPort";
    public static final String LOCAL_SOCKET_PROPERTY_NAME = "localSocket";
    public static final String REMOTE_PART_PROPERTY_NAME = "remotePart";
    public static final String REMOTE_ADDRESS_PROPERTY_NAME = "remoteAddress";
    public static final String REMOTE_PORT_PROPERTY_NAME = "remotePort";
    public static final String REMOTE_SOCKET_PROPERTY_NAME = "remoteSocket";

    public static final String ADDRESS_PORT_PART_NAME = "ADDRESS_PORT";
    public static final String SOCKET_PART_NAME = "SOCKET";

    public static final String BUILD_FEATURE_TYPE = "ssh-tunnel-build-feature";
    public static final String BUILD_FEATURE_ACTIVITY_TYPE = "CUSTOM_SSH_TUNNELS";
    public static final String SSH_EXECUTABLE_CONFIGURATION_PARAMETER_NAME = "ssh.executable";
    public static final String SSH_EXECUTABLE_ENVIRONMENT_VARIABLE_NAME = "SSH_EXECUTABLE";
    public static final String SSH_EXECUTABLE_REQUIREMENT_ID = SSH_EXECUTABLE_CONFIGURATION_PARAMETER_NAME + "-exists";

    public static final BigInteger MAX_PORT_NUMBER = BigInteger.valueOf(65_535);
    public static final int MIN_VERSION_SUPPORTING_BUILD_FEATURE_REQUIREMENTS = 65_998;

    public static final List<String> VALID_PROPERTY_NAMES = asList(
            SSH_TUNNEL_REQUIREMENT_PROPERTY_NAME, NAME_PROPERTY_NAME,
            USER_PROPERTY_NAME, SSH_KEY_PROPERTY_NAME, SSH_KEY_PASSPHRASE_PROPERTY_NAME, HOST_PROPERTY_NAME, PORT_PROPERTY_NAME,
            LOCAL_PART_PROPERTY_NAME, LOCAL_ADDRESS_PROPERTY_NAME, LOCAL_PORT_PROPERTY_NAME, LOCAL_SOCKET_PROPERTY_NAME,
            REMOTE_PART_PROPERTY_NAME, REMOTE_ADDRESS_PROPERTY_NAME, REMOTE_PORT_PROPERTY_NAME, REMOTE_SOCKET_PROPERTY_NAME);
    public static final List<String> VALID_PART_NAMES = asList(ADDRESS_PORT_PART_NAME, SOCKET_PART_NAME);

    private Constants() {
        throw new UnsupportedOperationException();
    }
}
