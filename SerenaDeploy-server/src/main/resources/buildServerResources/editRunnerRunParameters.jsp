<%@ taglib prefix="props" tagdir="/WEB-INF/tags/props" %>
<%@ taglib prefix="l" tagdir="/WEB-INF/tags/layout" %>
<%@ taglib prefix="bs" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="forms" tagdir="/WEB-INF/tags/forms" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<jsp:useBean id="propertiesBean" scope="request" type="jetbrains.buildServer.controllers.BasePropertiesBean"/>

    <tr class="groupingTitle">
        <td colspan="2">Deployment Server Settings:</td>
    </tr>

    <tr>
        <th>
            <label for="sraUrl">Serena DA URL: </label>
            <span class="mandatoryAsterix" title="Mandatory field">*</span>
        </th>
        <td>
            <props:textProperty name="sraUrl" className="longField" />
            <span class="error" id="error_sraUrl"></span>
            <span class="smallNote">The URL for Serena DA, i.e. http://localhost:8080/serena_ra</span>
        </td>
    </tr>
    <tr>
        <th>
            <label for="username">Username: </label>
            <span class="mandatoryAsterix" title="Mandatory field">*</span>
        </th>
        <td>
            <props:textProperty name="username" />
            <span class="error" id="error_username"></span>
            <span class="smallNote">The user name used to login to the Serena DA server.</span>
        </td>
    </tr>
    <tr>
        <th>
            <label for="password">Password: </label>
            <span class="mandatoryAsterix" title="Mandatory field">*</span>
        </th>
        <td>
            <props:passwordProperty name="password" />
            <span class="error" id="error_password"></span>
            <span class="smallNote">The password used to login to the Serena DA server.</span>
        </td>
    </tr>

    <!--tr>
        <th class="noBorder"></th>
        <td class="noBorder"><button id="validateConnection" type="button" class="btn btn-default">Validate</button></td>
    </tr-->

    <tr class="groupingTitle">
        <td colspan="2">Version Upload Settings:</td>
    </tr>

    <tr>
        <tr>
            <th>
                <label for="publishVersion">Publish Version:</label>
            </th>
            <td>
                <props:checkboxProperty name="publishVersion"/>
                <span class="error" id="error_publishVersion"></span>
                <span class="smallNote">Check here if you want to upload the outputs of this build into Serena DA.</span>
            </td>
        </tr>
    </tr>
    <tr>
        <th>
            <label for="publishVersionIf">Publish Version if:</label>
        </th>
        <td>
            <props:textProperty name="publishVersionIf" className="longField"/>
            <span class="error" id="error_publishVersionIf"></span>
            <span class="smallNote">Use a property to determine if the outputs of the build should be uploaded to Serena DA. If set this overrides the "Publish Version" checkbox.</span>
        </td>
    </tr>

    <tr>
        <th>
            <label for="componentName">Component: </label>
            <span class="mandatoryAsterix" title="Mandatory field">*</span>
        </th>
        <td>
            <props:textProperty name="componentName" />
            <span class="error" id="error_componentName"></span>
            <span class="smallNote">The name of the component in the Serena DA server which will receive the new version.</span>
        </td>
    </tr>
    <tr>
        <th>
            <label for="baseDir">Base Artifact Directory: </label>
            <span class="mandatoryAsterix" title="Mandatory field">*</span>
        </th>
        <td>
            <props:textProperty name="baseDir" className="longField"/>
            <span class="error" id="error_baseDir"></span>
            <span class="smallNote">Specify the base directory where the artifacts are located. If empty the default value is %teamcity.build.workingDir% which is the base of the build workspace.</span>
        </td>
    </tr>
    <tr>
        <th>
            <label for="dirOffset">Directory Offset: </label>
        </th>
        <td>
            <props:textProperty name="dirOffset" className="longField"/>
            <span class="error" id="error_dirOffset"></span>
            <span class="smallNote">Specify a sub-directory in the workspace where to search for files to publish. Absolute directories will not work.</span>
        </td>
    </tr>
    <tr>
        <th>
            <label for="versionName">Version Name: </label>
        </th>
        <td>
            <props:textProperty name="versionName" className="longField"/>
            <span class="error" id="error_versionName"></span>
            <span class="smallNote">The name of the new version that will be created in the Serena DA server. You can reference TeamCity parameters, e.g. %build.number%</span>
        </td>
    </tr>
    <tr>
        <th>
            <label for="includePatterns">Includes: </label>
        </th>
        <td>
            <props:textProperty name="includePatterns" className="longField"/>
            <span class="error" id="error_includePatterns"></span>
            <span class="smallNote">A comma separated list of file filters to select the files to publish. If empty the default includes everything in the workspace.</span>
        </td>
    </tr>
    <tr>
        <th>
            <label for="excludePatterns">Excludes: </label>
        </th>
        <td>
            <props:textProperty name="excludePatterns" className="longField"/>
            <span class="error" id="error_excludePatterns"></span>
            <span class="smallNote">A comma separated list of file filters to exclude from publishing. Leave blank to include everything.</span>
        </td>
    </tr>
    <tr class="advancedSetting">
        <th>
            <label for="versionProperties">Version Properties: </label>
        </th>
        <td>
            <props:multilineProperty name="versionProperties" linkTitle="Enter properties below:"
                                     cols="60" rows="5" expanded="${true}"/>
            <span class="error" id="error_versionProperties"></span>
            <span class="smallNote">Newline separated list of quoted properties and values i.e. prop1=%teamcity.project.id% to create on the version.</span>
        </td>
    </tr>

    <tr class="groupingTitle advancedSetting">
        <td colspan="2">Version Status Settings:</td>
    </tr>

    <tr class="advancedSetting">
        <th>
            <label for="addStatus">Add Status to Version:</label>
        </th>
        <td>
            <props:checkboxProperty name="addStatus"/>
            <span class="error" id="error_addStatus"></span>
            <span class="smallNote">Check here if you want to add a status to the version in Serena DA once it's uploaded.</span>
        </td>
    </tr>

    <tr class="advancedSetting">
        <th>
            <label for="statusName">Version Status Name:</label>
        </th>
        <td>
            <props:textProperty name="statusName" className="longField"/>
            <span class="error" id="error_statusName"></span>
            <span class="smallNote">The name of the status to apply to the version.</span>
        </td>
    </tr>

    <tr class="groupingTitle">
        <td colspan="2">Version Deployment Settings:</td>
    </tr>

    <tr>
        <th>
            <label for="deployVersion">Deploy Version:</label>
        </th>
        <td>
            <props:checkboxProperty name="deployVersion"/>
            <span class="error" id="error_deployVersion"></span>
            <span class="smallNote">Check here if you want to trigger a deployment of this version in Serena DA once it's uploaded.</span>
        </td>
    </tr>

    <tr class="advancedSetting">
        <th>
            <label for="deployVersionIf">Deploy Version if:</label>
        </th>
        <td>
            <props:textProperty name="deployVersionIf" className="longField"/>
            <span class="error" id="error_deployVersionIf"></span>
            <span class="smallNote">Use a property to determine if a deployment of this version should be triggered in Serena DA once it's uploaded. If set this overrides the "Deploy Version" checkbox.</span>
        </td>
    </tr>
    <tr>
        <th>
            <label for="deployApplication">Deploy Application: </label>
        </th>
        <td>
            <props:textProperty name="deployApplication" className="longField"/>
            <span class="error" id="error_deployApplication"></span>
            <span class="smallNote">The name of the application in Serena DA which will be used to deploy the new version.</span>
        </td>
    </tr>
    <tr>
        <th>
            <label for="deployEnvironment">Deploy Environment: </label>
        </th>
        <td>
            <props:textProperty name="deployEnvironment" className="longField"/>
            <span class="error" id="error_deployEnvironment"></span>
            <span class="smallNote">The name of the environment in Serena DA to deploy to.</span>
        </td>
    </tr>
    <tr>
        <th>
            <label for="deployProcess">Deploy Process: </label>
        </th>
        <td>
            <props:textProperty name="deployProcess" className="longField"/>
            <span class="error" id="error_deployProcess"></span>
            <span class="smallNote">The name of the application process in Serena DA which will be used to deploy the new version.</span>
        </td>
    </tr>
    <tr class="advancedSetting">
        <th>
            <label for="deployProperties">Deploy Properties: </label>
        </th>
        <td>
            <props:multilineProperty name="deployProperties" linkTitle="Enter properties below:"
                                     cols="60" rows="5" expanded="${true}"/>
            <span class="error" id="error_deployProperties"></span>
            <span class="smallNote">Newline separated list of quoted properties and values i.e. prop1=%teamcity.project.id% to pass to Serena DA Deployment Process.</span>
        </td>
    </tr>

<script type="text/javascript">
    (function($) {
        var validate = function() {
            alert("Not yet implemented");
        };

        $("#validateConnection").click(validate);
    })(jQuery);
</script>
