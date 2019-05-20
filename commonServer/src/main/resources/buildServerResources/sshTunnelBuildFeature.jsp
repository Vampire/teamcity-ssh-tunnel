<%@ page import="java.lang.Integer" %>
<%@ page import="net.kautler.teamcity.ssh_tunnel.common.Constants" %>

<%@ include file="/include.jsp" %>
<%@ taglib prefix="admin" tagdir="/WEB-INF/tags/admin" %>

<%--
  ~ Copyright 2019 Björn Kautler
  ~
  ~ Licensed under the Apache License, Version 2.0 (the "License");
  ~ you may not use this file except in compliance with the License.
  ~ You may obtain a copy of the License at
  ~
  ~     http://www.apache.org/licenses/LICENSE-2.0
  ~
  ~ Unless required by applicable law or agreed to in writing, software
  ~ distributed under the License is distributed on an "AS IS" BASIS,
  ~ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  ~ See the License for the specific language governing permissions and
  ~ limitations under the License.
  --%>

<jsp:useBean id="buildForm" type="jetbrains.buildServer.controllers.admin.projects.BuildTypeForm" scope="request"/>

<script type="text/javascript">
  BS.SshTunnelBuildFeature = {
    onChange: function(part, address, port, socket) {
      var partValue = $j(part).val();
      // this must be done before $j(part).val(null) otherwise the null-setting is effectless
      $j('option[value=""]', $j(part)).remove();
      if (partValue === '${Constants.ADDRESS_PORT_PART_NAME}') {
        $j(address).show();
        $j(port).show();
        $j(socket).hide();
      } else if (partValue === '${Constants.SOCKET_PART_NAME}') {
        $j(address).hide();
        $j(port).hide();
        $j(socket).show();
      } else {
        $j(part).val(null);
        $j(address).hide();
        $j(port).hide();
        $j(socket).hide();
      }
    },
    onLocalChange: function() {
      BS.SshTunnelBuildFeature.onChange('#${Constants.LOCAL_PART_PROPERTY_NAME}', '#${Constants.LOCAL_ADDRESS_PROPERTY_NAME}Row', '#${Constants.LOCAL_PORT_PROPERTY_NAME}Row', '#${Constants.LOCAL_SOCKET_PROPERTY_NAME}Row');
    },
    onRemoteChange: function() {
      BS.SshTunnelBuildFeature.onChange('#${Constants.REMOTE_PART_PROPERTY_NAME}', '#${Constants.REMOTE_ADDRESS_PROPERTY_NAME}Row', '#${Constants.REMOTE_PORT_PROPERTY_NAME}Row', '#${Constants.REMOTE_SOCKET_PROPERTY_NAME}Row');
    },
    onSshKeySelected: function(encrypted) {
      <c:set var="escapedSshKeyPassphrasePropertyName" value="${fn:replace(Constants.SSH_KEY_PASSPHRASE_PROPERTY_NAME, ':', '\\\\\\\\:')}"/>
      if (encrypted) {
        $j('#${escapedSshKeyPassphrasePropertyName}Row').show();
      } else {
        $j('#${escapedSshKeyPassphrasePropertyName}').val('');
        $j('#${escapedSshKeyPassphrasePropertyName}Row').hide();
      }
    }
  };
</script>

<tr>
  <td colspan="2"><em>This build feature opens an SSH tunnel with local forward during a build.</em></td>
</tr>
<tr>
  <th>
    <label for="${Constants.NAME_PROPERTY_NAME}">Name:<l:star/>&nbsp;<bs:helpPopup>
      <jsp:attribute name="linkText"><bs:helpIcon/></jsp:attribute>
      <jsp:attribute name="helpContent">The name for this forward, this is also used in the parameter names</jsp:attribute>
    </bs:helpPopup></label>
  </th>
  <td>
    <%-- part of work-around for missing BuildFeature#getRequirements --%>
    <jsp:useBean id="serverTC" type="jetbrains.buildServer.serverSide.SBuildServer" scope="request"/>
    <c:if test="${Integer.parseInt(serverTC.version.buildNumber) < Constants.MIN_VERSION_SUPPORTING_BUILD_FEATURE_REQUIREMENTS}">
      <props:hiddenProperty name="${Constants.SSH_TUNNEL_REQUIREMENT_PROPERTY_NAME}" value="%${Constants.SSH_EXECUTABLE_CONFIGURATION_PARAMETER_NAME}%"/>
    </c:if>
    <props:textProperty name="${Constants.NAME_PROPERTY_NAME}" className="longField"/>
    <span class="error" id="error_${Constants.NAME_PROPERTY_NAME}"></span>
  </td>
</tr>
<tr class="groupingTitle">
  <td colspan="2">Connection<l:star/>&nbsp;<bs:helpPopup>
    <jsp:attribute name="linkText"><bs:helpIcon/></jsp:attribute>
    <jsp:attribute name="helpContent">The connection properties for the carrier connection</jsp:attribute>
  </bs:helpPopup></td>
</tr>
<tr>
  <th>
    <label for="${Constants.USER_PROPERTY_NAME}">User:<l:star/>&nbsp;<bs:helpPopup>
      <jsp:attribute name="linkText"><bs:helpIcon/></jsp:attribute>
      <jsp:attribute name="helpContent">The user to connect as</jsp:attribute>
    </bs:helpPopup></label>
  </th>
  <td>
    <props:textProperty name="${Constants.USER_PROPERTY_NAME}" className="longField"/>
    <span class="error" id="error_${Constants.USER_PROPERTY_NAME}"></span>
  </td>
</tr>
<tr>
  <th>
    <label for="${Constants.SSH_KEY_PROPERTY_NAME}">Uploaded SSH Key:<l:star/><bs:help file="SSH Keys Management"/></label>
  </th>
  <td>
    <admin:sshKeys projectId="${buildForm.project.externalId}" keySelectionCallback="BS.SshTunnelBuildFeature.onSshKeySelected"/>
    <span class="error" id="error_${Constants.SSH_KEY_PROPERTY_NAME}"></span>
  </td>
</tr>
<tr id="${Constants.SSH_KEY_PASSPHRASE_PROPERTY_NAME}Row" style="display: none">
  <th>
    <label for="${Constants.SSH_KEY_PASSPHRASE_PROPERTY_NAME}">SSH Key Passphrase:<l:star/>&nbsp;<bs:helpPopup>
      <jsp:attribute name="linkText"><bs:helpIcon/></jsp:attribute>
      <jsp:attribute name="helpContent">The passphrase for the selected SSH key</jsp:attribute>
    </bs:helpPopup></label>
  </th>
  <td>
    <props:passwordProperty name="${Constants.SSH_KEY_PASSPHRASE_PROPERTY_NAME}" className="longField"/>
    <span class="error" id="error_${Constants.SSH_KEY_PASSPHRASE_PROPERTY_NAME}"></span>
  </td>
</tr>
<tr>
  <th>
    <label for="${Constants.HOST_PROPERTY_NAME}">Host:<l:star/>&nbsp;<bs:helpPopup>
      <jsp:attribute name="linkText"><bs:helpIcon/></jsp:attribute>
      <jsp:attribute name="helpContent">The host to connect to</jsp:attribute>
    </bs:helpPopup></label>
  </th>
  <td>
    <props:textProperty name="${Constants.HOST_PROPERTY_NAME}" className="longField"/>
    <span class="error" id="error_${Constants.HOST_PROPERTY_NAME}"></span>
  </td>
</tr>
<tr>
  <th>
    <label for="${Constants.PORT_PROPERTY_NAME}">Port:&nbsp;<bs:helpPopup>
      <jsp:attribute name="linkText"><bs:helpIcon/></jsp:attribute>
      <jsp:attribute name="helpContent">The port to connect to (default: 22)</jsp:attribute>
    </bs:helpPopup></label>
  </th>
  <td>
    <props:textProperty name="${Constants.PORT_PROPERTY_NAME}" className="longField"/>
    <span class="error" id="error_${Constants.PORT_PROPERTY_NAME}"></span>
  </td>
</tr>
<tr class="groupingTitle">
  <td colspan="2">Local Part&nbsp;<bs:helpPopup>
    <jsp:attribute name="linkText"><bs:helpIcon/></jsp:attribute>
    <jsp:attribute name="helpContent">Either local address / port or local socket can be specified (default: 127.0.0.1:&lt;random port&gt;)</jsp:attribute>
  </bs:helpPopup></td>
</tr>
<tr>
  <th>
    <label for="${Constants.LOCAL_PART_PROPERTY_NAME}">Local Part:&nbsp;<bs:helpPopup>
      <jsp:attribute name="linkText"><bs:helpIcon/></jsp:attribute>
      <jsp:attribute name="helpContent">Either local address / port or local socket can be specified (default: 127.0.0.1:&lt;random port&gt;)</jsp:attribute>
    </bs:helpPopup></label>
  </th>
  <td>
    <props:selectProperty name="${Constants.LOCAL_PART_PROPERTY_NAME}" className="longField" onchange="BS.SshTunnelBuildFeature.onLocalChange()">
      <props:option value=""/>
      <props:option value="${Constants.ADDRESS_PORT_PART_NAME}">Address and Port</props:option>
      <props:option value="${Constants.SOCKET_PART_NAME}">Socket</props:option>
    </props:selectProperty>
    <span class="error" id="error_${Constants.LOCAL_PART_PROPERTY_NAME}"></span>
  </td>
</tr>
<tr id="${Constants.LOCAL_ADDRESS_PROPERTY_NAME}Row">
  <th>
    <label for="${Constants.LOCAL_ADDRESS_PROPERTY_NAME}">Local Address:&nbsp;<bs:helpPopup>
      <jsp:attribute name="linkText"><bs:helpIcon/></jsp:attribute>
      <jsp:attribute name="helpContent">The local address to bind to (default: 127.0.0.1)</jsp:attribute>
    </bs:helpPopup></label>
  </th>
  <td>
    <props:textProperty name="${Constants.LOCAL_ADDRESS_PROPERTY_NAME}" className="longField"/>
    <span class="error" id="error_${Constants.LOCAL_ADDRESS_PROPERTY_NAME}"></span>
  </td>
</tr>
<tr id="${Constants.LOCAL_PORT_PROPERTY_NAME}Row">
  <th>
    <label for="${Constants.LOCAL_PORT_PROPERTY_NAME}">Local Port:&nbsp;<bs:helpPopup>
      <jsp:attribute name="linkText"><bs:helpIcon/></jsp:attribute>
      <jsp:attribute name="helpContent">The local port to forward (default: &lt;random port&gt;)</jsp:attribute>
    </bs:helpPopup></label>
  </th>
  <td>
    <props:textProperty name="${Constants.LOCAL_PORT_PROPERTY_NAME}" className="longField"/>
    <span class="error" id="error_${Constants.LOCAL_PORT_PROPERTY_NAME}"></span>
  </td>
</tr>
<tr id="${Constants.LOCAL_SOCKET_PROPERTY_NAME}Row">
  <th>
    <label for="${Constants.LOCAL_SOCKET_PROPERTY_NAME}">Local Socket:<l:star/>&nbsp;<bs:helpPopup>
      <jsp:attribute name="linkText"><bs:helpIcon/></jsp:attribute>
      <jsp:attribute name="helpContent">The local socket to forward</jsp:attribute>
    </bs:helpPopup></label>
  </th>
  <td>
    <props:textProperty name="${Constants.LOCAL_SOCKET_PROPERTY_NAME}" className="longField"/>
    <span class="error" id="error_${Constants.LOCAL_SOCKET_PROPERTY_NAME}"></span>
  </td>
</tr>
<tr class="groupingTitle">
  <td colspan="2">Remote Part<l:star/>&nbsp;<bs:helpPopup>
    <jsp:attribute name="linkText"><bs:helpIcon/></jsp:attribute>
    <jsp:attribute name="helpContent">Either remote address / port or remote socket must be specified</jsp:attribute>
  </bs:helpPopup></td>
</tr>
<tr>
  <th>
    <label for="${Constants.REMOTE_PART_PROPERTY_NAME}">Remote Part<l:star/>&nbsp;<bs:helpPopup>
      <jsp:attribute name="linkText"><bs:helpIcon/></jsp:attribute>
      <jsp:attribute name="helpContent">Either remote address / port or remote socket must be specified</jsp:attribute>
    </bs:helpPopup></label>
  </th>
  <td>
    <props:selectProperty name="${Constants.REMOTE_PART_PROPERTY_NAME}" className="longField" onchange="BS.SshTunnelBuildFeature.onRemoteChange()">
      <props:option value=""/>
      <props:option value="${Constants.ADDRESS_PORT_PART_NAME}">Address and Port</props:option>
      <props:option value="${Constants.SOCKET_PART_NAME}">Socket</props:option>
    </props:selectProperty>
    <span class="error" id="error_${Constants.REMOTE_PART_PROPERTY_NAME}"></span>
  </td>
</tr>
<tr id="${Constants.REMOTE_ADDRESS_PROPERTY_NAME}Row">
  <th>
    <label for="${Constants.REMOTE_ADDRESS_PROPERTY_NAME}">Remote Address:&nbsp;<bs:helpPopup>
      <jsp:attribute name="linkText"><bs:helpIcon/></jsp:attribute>
      <jsp:attribute name="helpContent">The remote address to forward to (default: 127.0.0.1)</jsp:attribute>
    </bs:helpPopup></label>
  </th>
  <td>
    <props:textProperty name="${Constants.REMOTE_ADDRESS_PROPERTY_NAME}" className="longField"/>
    <span class="error" id="error_${Constants.REMOTE_ADDRESS_PROPERTY_NAME}"></span>
  </td>
</tr>
<tr id="${Constants.REMOTE_PORT_PROPERTY_NAME}Row">
  <th>
    <label for="${Constants.REMOTE_PORT_PROPERTY_NAME}">Remote Port:<l:star/>&nbsp;<bs:helpPopup>
      <jsp:attribute name="linkText"><bs:helpIcon/></jsp:attribute>
      <jsp:attribute name="helpContent">The remote port to forward to</jsp:attribute>
    </bs:helpPopup></label>
  </th>
  <td>
    <props:textProperty name="${Constants.REMOTE_PORT_PROPERTY_NAME}" className="longField"/>
    <span class="error" id="error_${Constants.REMOTE_PORT_PROPERTY_NAME}"></span>
  </td>
</tr>
<tr id="${Constants.REMOTE_SOCKET_PROPERTY_NAME}Row">
  <th>
    <label for="${Constants.REMOTE_SOCKET_PROPERTY_NAME}">Remote Socket:<l:star/>&nbsp;<bs:helpPopup>
      <jsp:attribute name="linkText"><bs:helpIcon/></jsp:attribute>
      <jsp:attribute name="helpContent">The remote socket to forward to</jsp:attribute>
    </bs:helpPopup></label>
  </th>
  <td>
    <props:textProperty name="${Constants.REMOTE_SOCKET_PROPERTY_NAME}" className="longField"/>
    <span class="error" id="error_${Constants.REMOTE_SOCKET_PROPERTY_NAME}"></span>
  </td>
</tr>

<script type="text/javascript">
  BS.SshTunnelBuildFeature.onLocalChange();
  BS.SshTunnelBuildFeature.onRemoteChange();
  BS.SshTunnelBuildFeature.onSshKeySelected($j('#${Constants.SSH_KEY_PROPERTY_NAME}').find(':selected').hasClass('encrypted'));
</script>
