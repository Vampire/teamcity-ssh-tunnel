TeamCity SSH Tunnel
===================

This is a plugin for TeamCity that allows SSH Tunnels to be opened with port and socket forwarding as build feature.
This can, for example, be used to securely control Docker daemons on other hosts to implement continuous deployment.



Table of Contents
-----------------
* [Installation](#installation)
  * [TeamCity 2018.2 and newer](#teamcity-20182-and-newer)
  * [TeamCity older than 2018.2](#teamcity-older-than-20182)
* [Requirements](#requirements)
* [Setup](#setup)
  * [In XML Config Files](#in-xml-config-files)
  * [In DSL Config Files](#in-xml-config-files)
* [Usage](#usage)
  * [Example Use Case](#example-use-case)
* [License](#license)



Installation
------------

Since TeamCity 2018.2, reloadable plugins that can be installed, unloaded, and loaded while TeamCity is running can be
used, which is supported by this plugin. Because of that, there are two different releases and two different methods to
install the plugin, but the method for older TeamCity versions would also work on newer versions if you prefer.

### TeamCity 2018.2 and newer

1. If you have not installed any plugins from the plugins repository before,
   go to `Administration -> Plugins List -> Browse plugins repository` so that your TeamCity instance is known to
   the plugins repository and optionally add your instance permanently from the pop-up in the lower right corner.

2. Go to the [plugins repository page] and click on `GET -> Install to ...`

3. Click `Install`

4. Click `Enable`

### TeamCity older than 2018.2

1. Download the ZIP file from the [plugins repository page] and place it as-is into
   the `plugins` directory of your TeamCity data directory. Do not extract the ZIP file.  
   You can, for example,
   * put the ZIP file manually into the data directory, if you know where it is located and how to access it
   * go to `Administration -> Plugins List -> Upload plugin zip` and upload the ZIP via web interface
   * go to `Administration -> Diagnostics -> Browse Data Directory`,
     press `Upload new file`, and upload the ZIP via web interface to the `plugins` directory

2. Delete the ZIP file of the old version from the `plugins` directory if you are updating from a previous version.

3. After the ZIP file is placed where it is supposed to be, restart your TeamCity server,
   as it does not recognize plugin changes until restart.



Requirements
-----

The plugin needs the "SSH Keys Manager" plugin - which is shipped with TeamCity - to be available and enabled.

Additionally, on the build agent the `ssh` client tool needs to be installed. It can be available on the path. If it is
not, some common places in the filesystem are searched for the executable. If the `ssh` client is available but is not
found or a different than the automatically found one should be used, it can be configured on the build agent as a build
agent property in `conf/buildAgent.properties`, as a system property, both with the name `ssh.executable`, or as
environment variable with name `SSH_EXECUTABLE` in that order. The value should either be an executable name available
on the path or an absolute path. Alternatively the value can be set to `auto`. In this case the whole filesystem is
searched for an executable called `ssh` with an optional file extension, which is tested for being an `ssh` client
executable.

**Warnings for usage of `auto`**

- _**Security:**_ If `auto` is used, the whole file tree is scanned for a file which then is executed. If the build
  agent system is not trustworthy, or you are running untrusted builds that could leave a file called accordingly, this
  could maliciously steal your SSH key. Do **not** use this functionality if your build agent system is not trustworthy
  or you are running untrusted builds like pull requests from arbitrary people, but configure the SSH executable
  directly.

- _**Performance:**_ If `auto` is used, the whole file tree is scanned for a file, which is a very expensive operation
  in terms of performance. If you use this functionality, the startup of your build agents will be significantly delayed
  due to this file tree search. Do **not** use this functionality if this is an issue for you, but configure the SSH
  executable directly.



Setup
-----

The plugin adds the `SSH Tunnel` build feature to TeamCity.
You can add the build feature to any build configuration template or build configuration.
You can add the build feature as often as you want to the same or to different hosts.
Multiple build features with the same connection properties in a build will only open
one SSH connection to the target host and open multiple port or socket forwards.

<dl>
    <dt><b>Name</b></dt>
    <dd>The name for this forward. This is also used in the parameter names.</dd>
    <dt><b>User</b></dt>
    <dd>The user to connect as.</dd>
    <dt><b>Uploaded SSH Key</b></dt>
    <dd>
        The SSH key to authenticate with. The keys that are uploaded to TeamCity and are available
        to the current build configuration or build configuration template are displayed for
        selection. Username / password authentication or arbitrary SSH keys are
        currently not supported, if you need them, please open a feature request.
    </dd>
    <dt><b>SSH Key Passphrase</b></dt>
    <dd>
        The passphrase for the selected SSH key.<br/>
        This field is only visible if the selected SSH key is actually encrypted and needs a passphrase.
    </dd>
    <dt><b>Host</b></dt>
    <dd>The host to connect to.</dd>
    <dt><b>Port</b></dt>
    <dd>
        The port to connect to.<br/>
        <b><i>default:</i></b> <code>22</code>
    </dd>
    <dt><b>Local Part</b></dt>
    <dd>
        Either "Address and Port" to forward a local port on an address
        or "Socket" to forward a local socket.<br/>
        <b><i>default:</i></b> <code>Address and Port</code>
    </dd>
    <dt><b>Local Address</b></dt>
    <dd>
        The local address to bind to.<br/>
        This field is only visible if the selected local part is "Address and Port".<br/>
        <b><i>default:</i></b> <code>127.0.0.1</code>
    </dd>
    <dt><b>Local Port</b></dt>
    <dd>
        The local port to forward.<br/>
        This field is only visible if the selected local part is "Address and Port".<br/>
        <b><i>default:</i></b> <code>&lt;random port&gt;</code>
    </dd>
    <dt><b>Local Socket</b></dt>
    <dd>
        The local socket to forward.<br/>
        This field is only visible if the selected local part is "Socket".
    </dd>
    <dt><b>Remote Part</b></dt>
    <dd>
        Either "Address and Port" to forward to a remote port on an address
        or "Socket" to forward to a remote socket.<br/>
    </dd>
    <dt><b>Remote Address</b></dt>
    <dd>
        The remote address to forward to.<br/>
        This field is only visible if the selected remote part is "Address and Port".<br/>
        <b><i>default:</i></b> <code>127.0.0.1</code>
    </dd>
    <dt><b>Remote Port</b></dt>
    <dd>
        The remote port to forward to.<br/>
        This field is only visible if the selected remote part is "Address and Port".<br/>
    </dd>
    <dt><b>Remote Socket</b></dt>
    <dd>
        The remote socket to forward to.<br/>
        This field is only visible if the selected local part is "Socket".
    </dd>
</dl>

### In XML Config Files

If you edit XML config files on the server directly or in versioned settings, the build feature will, for example, look
like the following (values that have their default value will be omitted). For TeamCity versions prior to 2019.1, the
`sshTunnelRequirement` parameter has to be present and set to `%ssh.executable%` as shown, so the build is only
run on compatible agents. For later TeamCity versions this should be omitted as the requirement can be requested
by the build feature implementation. The parameter will be set by a compatible agent automatically or be configured
there if needed (see [Requirements](#requirements)). If you omit the parameter it is not fatal, as the plugin does
its best to add or correct it automatically, but this is also not really supported by TeamCity, so it is better if you
add it as expected.

```xml
<extension id="docker_on_host_x" type="ssh-tunnel-build-feature">
  <parameters>
    <param name="name" value="Docker on host X" />
    <param name="user" value="teamcity" />
    <param name="teamcitySshKey" value="my_host_x_key_name" />
    <param name="host" value="my.host-x.net" />
    <param name="remotePart" value="SOCKET" />
    <param name="remoteSocket" value="/var/run/docker.sock" />
    <!-- Omit this for TeamCity 2019.1 and later -->
    <param name="sshTunnelRequirement" value="%ssh.executable%" />
  </parameters>
</extension>
```

### In DSL Config Files

If you edit DSL config files in versioned settings, the build feature will, for example, look like the following (values
that have their default value will be omitted). For TeamCity versions prior to 2019.1, the `sshTunnelRequirement`
property has to be present and set to `%ssh.executable%` as shown, so the build is only run on compatible agents. For
later TeamCity versions this should be omitted as the requirement can be requested by the build feature implementation.
The parameter will be set by a compatible agent automatically or be configured there if needed
(see [Requirements](#requirements)). If you omit the parameter it is not fatal, as the plugin does its best to add or
correct it automatically, but this is also not really supported by TeamCity, so it is better if you
add it as expected.

```kotlin
sshTunnel {
    id = "docker_on_host_x"
    name = "Docker on host X"
    connection = c {
        user = "teamcity"
        sshKey = "my_host_x_key_name"
        host = "my.host-x.net"
    }
    remotePart = remoteSocket {
        socket = "/var/run/docker.sock"
    }
    // Omit this for TeamCity 2019.1 and later
    sshTunnelRequirement = "%ssh.executable%"
}
```



Usage
-----

After adding the build feature to a build configuration (or a build configuration template), the plugin opens the
configured SSH tunnels before the build starts, and any step in the build can use the established forwards. If the
establishing of the tunnel fails, the build fails as a whole.

The plugin adds configuration properties to the build that can be used anywhere configuration properties are available,
like custom build scripts, build parameters, environment variables, and so on. All details of the configuration are
exposed as properties, including the actual local port of the forward if no explicit port was configured. The name of
the configured build feature is sanitized and included in the property name so that all build features settings are
accessible individually. Sanitized in this context means that any non-ASCII-alphanumeric character is replaced by an
underscore.

The configuration properties that are available for each forward are:

* `sshTunnel.<sanitized_name>.connection.user`
* `sshTunnel.<sanitized_name>.connection.sshKey`
* `sshTunnel.<sanitized_name>.connection.host`
* `sshTunnel.<sanitized_name>.connection.port`
* `sshTunnel.<sanitized_name>.local.address`
* `sshTunnel.<sanitized_name>.local.port`
* `sshTunnel.<sanitized_name>.local.socket`
* `sshTunnel.<sanitized_name>.remote.address`
* `sshTunnel.<sanitized_name>.remote.port`
* `sshTunnel.<sanitized_name>.remote.socket`

Whereas for `local` and `remote`, either `address` and `port` together or `socket` exist, but not all three at once.

### Example Use Case

You want your build to be able to run docker commands on a remote docker daemon that only listens on the host where it
is running and you have SSH access to an account that is allowed to control the docker daemon on the target machine.

In the configuration of the build feature, you give the name `Docker on host X`, appropriate connection properties,
nothing in the local part to use a random port listening on the localhost interface, and `Socket` with
`/var/run/docker.sock` for the remote part.

If you use a tool that has configuration options for the docker daemon, you can use those. For example, if you use the
commandline `docker` client in a custom script runner, you can do something like
`docker -H %sshTunnel.Docker_on_host_X.local.address%:%sshTunnel.Docker_on_host_X.local.port% ps` to list the running
containers.

Alternatively, you could add a parameter to your build configuration called `env.DOCKER_HOST` with value
`tcp://%sshTunnel.Docker_on_host_X.local.address%:%sshTunnel.Docker_on_host_X.local.port%` and then use any tool
communicating with the docker daemon as if the daemon was local, such as calling `docker ps` in a custom script runner
to list the running containers.




License
-------

```
Copyright 2019 Björn Kautler

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```



[plugins repository page]: https://plugins.jetbrains.com/plugin/12463-ssh-tunnel
