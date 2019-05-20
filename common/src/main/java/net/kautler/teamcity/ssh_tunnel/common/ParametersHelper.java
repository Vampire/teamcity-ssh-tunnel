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

import jetbrains.buildServer.util.Couple;
import jetbrains.buildServer.util.MultiMap;

import java.math.BigInteger;
import java.util.Collection;
import java.util.Map;

import static java.math.BigInteger.ZERO;
import static java.util.stream.Collectors.joining;
import static jetbrains.buildServer.parameters.ReferencesResolverUtil.isReference;
import static jetbrains.buildServer.util.StringUtil.isEmpty;
import static jetbrains.buildServer.util.StringUtil.isNotEmpty;
import static net.kautler.teamcity.ssh_tunnel.common.Constants.ADDRESS_PORT_PART_NAME;
import static net.kautler.teamcity.ssh_tunnel.common.Constants.HOST_PROPERTY_NAME;
import static net.kautler.teamcity.ssh_tunnel.common.Constants.LOCAL_ADDRESS_PROPERTY_NAME;
import static net.kautler.teamcity.ssh_tunnel.common.Constants.LOCAL_PART_PROPERTY_NAME;
import static net.kautler.teamcity.ssh_tunnel.common.Constants.LOCAL_PORT_PROPERTY_NAME;
import static net.kautler.teamcity.ssh_tunnel.common.Constants.LOCAL_SOCKET_PROPERTY_NAME;
import static net.kautler.teamcity.ssh_tunnel.common.Constants.MAX_PORT_NUMBER;
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
import static net.kautler.teamcity.ssh_tunnel.common.Constants.VALID_PART_NAMES;
import static net.kautler.teamcity.ssh_tunnel.common.Constants.VALID_PROPERTY_NAMES;
import static net.kautler.teamcity.ssh_tunnel.common.ModelBuilder.buildSshTunnel;

public abstract class ParametersHelper {
    public String describeParameters(Map<String, String> parameters) {
        MultiMap<String, String> invalidProperties = validate(parameters);
        return invalidProperties.isEmpty()
                ? buildSshTunnel(parameters).toString()
                : "Parameters are invalid:\n- " + invalidProperties.values().stream()
                .flatMap(Collection::stream)
                .distinct()
                .collect(joining("\n- "));
    }

    /**
     * Properties map passed as argument can be verified or modified by the processor
     * (for example, one could remove all properties with empty values from this map).
     *
     * @param properties properties to process
     * @return map of invalid property names to reasons
     */
    public MultiMap<String, String> validate(Map<String, String> properties) {
        MultiMap<String, String> invalidProperties = properties.keySet().stream()
                .filter(name -> !VALID_PROPERTY_NAMES.contains(name))
                .map(name -> Couple.of(name, String.format("Parameter '%s' is unknown", name)))
                .collect(MultiMap::new,
                        (map, invalidProperty) -> map.putValue(invalidProperty.a, invalidProperty.b),
                        (map1, map2) -> map2.entrySet().forEach(entry -> entry.getValue().forEach(value -> map1.putValue(entry.getKey(), value))));

        VALID_PROPERTY_NAMES.forEach(propertyName -> properties.computeIfPresent(propertyName, (name, value) -> value.trim()));

        if (isEmpty(properties.get(NAME_PROPERTY_NAME))) {
            invalidProperties.putValue(NAME_PROPERTY_NAME, "Name must be specified");
        }

        if (isEmpty(properties.get(USER_PROPERTY_NAME))) {
            invalidProperties.putValue(USER_PROPERTY_NAME, "User must be specified");
        }

        String sshKeyName = properties.get(SSH_KEY_PROPERTY_NAME);
        if (isEmpty(sshKeyName)) {
            invalidProperties.putValue(SSH_KEY_PROPERTY_NAME, "SSH key must be specified");
        } else if (isValidKeyName(sshKeyName)) {
            boolean sshKeyEncrypted = isEncrypted(sshKeyName);
            boolean sshKeyPassphraseGiven = isNotEmpty(properties.get(SSH_KEY_PASSPHRASE_PROPERTY_NAME));
            if (sshKeyEncrypted && !sshKeyPassphraseGiven) {
                invalidProperties.putValue(SSH_KEY_PASSPHRASE_PROPERTY_NAME, "SSH key passphrase must be specified for encrypted SSH key");
            } else if (!sshKeyEncrypted && sshKeyPassphraseGiven) {
                invalidProperties.putValue(SSH_KEY_PASSPHRASE_PROPERTY_NAME, "SSH key passphrase must not be specified for unencrypted SSH key");
            }
        } else {
            invalidProperties.putValue(SSH_KEY_PROPERTY_NAME, String.format("SSH key '%s' not found", sshKeyName));
        }

        if (isEmpty(properties.get(HOST_PROPERTY_NAME))) {
            invalidProperties.putValue(HOST_PROPERTY_NAME, "Host must be specified");
        }

        if (isNotEmpty(properties.get(PORT_PROPERTY_NAME)) && !isReference(properties.get(PORT_PROPERTY_NAME))) {
            try {
                BigInteger port = new BigInteger(properties.get(PORT_PROPERTY_NAME));
                if (port.compareTo(ZERO) < 0) {
                    invalidProperties.putValue(PORT_PROPERTY_NAME, "Port if given must be a number between 0 and 65535");
                } else if (port.compareTo(MAX_PORT_NUMBER) > 0) {
                    invalidProperties.putValue(PORT_PROPERTY_NAME, "Port if given must be a number between 0 and 65535");
                }
            } catch (NumberFormatException nfe) {
                invalidProperties.putValue(PORT_PROPERTY_NAME, "Port if given must be a number between 0 and 65535");
            }
        }

        boolean localAddressGiven = isNotEmpty(properties.get(LOCAL_ADDRESS_PROPERTY_NAME));
        boolean localPortGiven = isNotEmpty(properties.get(LOCAL_PORT_PROPERTY_NAME));
        boolean localSocketGiven = isNotEmpty(properties.get(LOCAL_SOCKET_PROPERTY_NAME));
        if (isEmpty(properties.get(LOCAL_PART_PROPERTY_NAME))) {
            if ((localAddressGiven || localPortGiven) && localSocketGiven) {
                invalidProperties.putValue(LOCAL_PART_PROPERTY_NAME, "Local part must be specified");
            } else if (localSocketGiven) {
                properties.put(LOCAL_PART_PROPERTY_NAME, SOCKET_PART_NAME);
            } else {
                properties.put(LOCAL_PART_PROPERTY_NAME, ADDRESS_PORT_PART_NAME);
            }
        }

        boolean remotePortGiven = isNotEmpty(properties.get(REMOTE_PORT_PROPERTY_NAME));
        boolean remoteSocketGiven = isNotEmpty(properties.get(REMOTE_SOCKET_PROPERTY_NAME));
        if (isEmpty(properties.get(REMOTE_PART_PROPERTY_NAME))) {
            if ((remotePortGiven && remoteSocketGiven) || (!remotePortGiven && !remoteSocketGiven)) {
                invalidProperties.putValue(REMOTE_PART_PROPERTY_NAME, "Remote part must be specified");
            } else if (remoteSocketGiven) {
                properties.put(REMOTE_PART_PROPERTY_NAME, SOCKET_PART_NAME);
            } else {
                properties.put(REMOTE_PART_PROPERTY_NAME, ADDRESS_PORT_PART_NAME);
            }
        }

        if (!VALID_PART_NAMES.contains(properties.getOrDefault(LOCAL_PART_PROPERTY_NAME, ADDRESS_PORT_PART_NAME))) {
            invalidProperties.putValue(LOCAL_PART_PROPERTY_NAME, "Local part value is invalid");
        }

        if (!VALID_PART_NAMES.contains(properties.getOrDefault(REMOTE_PART_PROPERTY_NAME, ADDRESS_PORT_PART_NAME))) {
            invalidProperties.putValue(REMOTE_PART_PROPERTY_NAME, "Remote part value is invalid");
        }

        switch (properties.getOrDefault(LOCAL_PART_PROPERTY_NAME, "")) {
            case ADDRESS_PORT_PART_NAME:
                properties.remove(LOCAL_SOCKET_PROPERTY_NAME);

                if (localPortGiven && !isReference(properties.get(LOCAL_PORT_PROPERTY_NAME))) {
                    try {
                        BigInteger localPort = new BigInteger(properties.get(LOCAL_PORT_PROPERTY_NAME));
                        if (localPort.compareTo(ZERO) < 0) {
                            invalidProperties.putValue(LOCAL_PORT_PROPERTY_NAME, "Local port if given must be a number between 0 and 65535");
                        } else if (localPort.compareTo(MAX_PORT_NUMBER) > 0) {
                            invalidProperties.putValue(LOCAL_PORT_PROPERTY_NAME, "Local port if given must be a number between 0 and 65535");
                        }
                    } catch (NumberFormatException nfe) {
                        invalidProperties.putValue(LOCAL_PORT_PROPERTY_NAME, "Local port if given must be a number between 0 and 65535");
                    }
                } else if (!localAddressGiven) {
                    properties.remove(LOCAL_PART_PROPERTY_NAME);
                }
                break;

            case SOCKET_PART_NAME:
                properties.remove(LOCAL_ADDRESS_PROPERTY_NAME);
                properties.remove(LOCAL_PORT_PROPERTY_NAME);

                if (!localSocketGiven) {
                    invalidProperties.putValue(LOCAL_SOCKET_PROPERTY_NAME, "Local socket must be specified");
                }
                break;

            default:
                break;
        }

        switch (properties.getOrDefault(REMOTE_PART_PROPERTY_NAME, "")) {
            case ADDRESS_PORT_PART_NAME:
                properties.remove(REMOTE_SOCKET_PROPERTY_NAME);

                if (remotePortGiven && !isReference(properties.get(REMOTE_PORT_PROPERTY_NAME))) {
                    try {
                        BigInteger remotePort = new BigInteger(properties.get(REMOTE_PORT_PROPERTY_NAME));
                        if (remotePort.compareTo(ZERO) < 0) {
                            invalidProperties.putValue(REMOTE_PORT_PROPERTY_NAME, "Remote port must be a number between 0 and 65535");
                        } else if (remotePort.compareTo(MAX_PORT_NUMBER) > 0) {
                            invalidProperties.putValue(REMOTE_PORT_PROPERTY_NAME, "Remote port must be a number between 0 and 65535");
                        }
                    } catch (NumberFormatException nfe) {
                        invalidProperties.putValue(REMOTE_PORT_PROPERTY_NAME, "Remote port must be a number between 0 and 65535");
                    }
                } else {
                    invalidProperties.putValue(REMOTE_PORT_PROPERTY_NAME, "Remote port must be specified");
                }
                break;

            case SOCKET_PART_NAME:
                properties.remove(REMOTE_ADDRESS_PROPERTY_NAME);
                properties.remove(REMOTE_PORT_PROPERTY_NAME);

                if (!remoteSocketGiven) {
                    invalidProperties.putValue(REMOTE_SOCKET_PROPERTY_NAME, "Remote socket must be specified");
                }
                break;

            default:
                break;
        }

        return invalidProperties;
    }

    protected abstract boolean isValidKeyName(String shKeyName);

    protected abstract boolean isEncrypted(String shKeyName);
}
