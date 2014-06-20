<%@ taglib prefix="props" tagdir="/WEB-INF/tags/props" %>
<%@ taglib prefix="l" tagdir="/WEB-INF/tags/layout" %>
<%@ taglib prefix="bs" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="forms" tagdir="/WEB-INF/tags/forms" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<jsp:useBean id="propertiesBean" scope="request" type="jetbrains.buildServer.controllers.BasePropertiesBean"/>

<l:settingsGroup title="Runner Parameters">
    <tr>
        <th>
            <label for="sraUrl">Serena RA URL: </label>
        </th>
        <td>
            <props:textProperty name="sraUrl" />
            <span class="error" id="error_sraUrl"></span>
            <span class="smallNote">The URL for Serena RA, i.e. http://localhost:8080/serena_ra</span>
        </td>
    </tr>
    <tr>
        <th>
            <label for="username">Username: </label>
        </th>
        <td>
            <props:textProperty name="username" />
            <span class="error" id="error_username"></span>
            <span class="smallNote">The user name used to login to the Serena RA server.</span>
        </td>
    </tr>
    <tr>
        <th>
            <label for="password">Password: </label>
        </th>
        <td>
            <props:passwordProperty name="password" />
            <span class="error" id="error_password"></span>
            <span class="smallNote">The password used to login to the Serena RA server.</span>
        </td>
    </tr>
    <tr>
        <th>
            <label for="componentName">Component: </label>
        </th>
        <td>
            <props:textProperty name="componentName" />
            <span class="error" id="error_componentName"></span>
            <span class="smallNote">The name of the component in the Serena RA server which will receive the new version.</span>
        </td>
    </tr>
    <tr>
        <th>
            <label for="baseDir">Base Artifact Directory: </label>
        </th>
        <td>
            <props:textProperty name="baseDir" />
            <span class="error" id="error_baseDir"></span>
            <span class="smallNote">Specify the base directory where the artifacts are located. If empty the default value is %teamcity.build.workingDir% which is the base of the build workspace.</span>
        </td>
    </tr>
    <tr>
        <th>
            <label for="dirOffset">Directory Offset: </label>
        </th>
        <td>
            <props:textProperty name="dirOffset" />
            <span class="error" id="error_dirOffset"></span>
            <span class="smallNote">Specify a sub-directory in the workspace where to search for files to publish. Absolute directories will not work.</span>
        </td>
    </tr>
    <tr>
        <th>
            <label for="versionName">Version Name: </label>
        </th>
        <td>
            <props:textProperty name="versionName" />
            <span class="error" id="error_versionName"></span>
            <span class="smallNote">The name of the new version that will be created in the Serena RA server. You can reference TeamCity parameters, e.g. %build.number%</span>
        </td>
    </tr>
    <tr>
        <th>
            <label for="includePatterns">Includes: </label>
        </th>
        <td>
            <props:textProperty name="includePatterns" />
            <span class="error" id="error_includePatterns"></span>
            <span class="smallNote">A comma separated list of file filters to select the files to publish. If empty the default is **/* which includes everything in the workspace.</span>
        </td>
    </tr>
    <tr>
        <th>
            <label for="excludePatterns">Excludes: </label>
        </th>
        <td>
            <props:textProperty name="excludePatterns" />
            <span class="error" id="error_excludePatterns"></span>
            <span class="smallNote">A comma separated list of file filters to exclude from publishing. Leave blank to include everything.</span>
        </td>
    </tr>
    <tr>
        <th>
            <label for="deployVersion">Deploy Version:</label>
        </th>
        <td>
            <props:checkboxProperty name="deployVersion" />
            <span class="error" id="error_deployVersion"></span>
            <span class="smallNote">Check here if you want to trigger a deployment of this version in Serena RA once it's uploaded.</span>
        </td>
    </tr>
    <tr>
        <th>
            <label for="deployApplication">Deploy Application: </label>
        </th>
        <td>
            <props:textProperty name="deployApplication" />
            <span class="error" id="error_deployApplication"></span>
            <span class="smallNote">The name of the application in Serena RA which will be used to deploy the new version.</span>
        </td>
    </tr>
    <tr>
        <th>
            <label for="deployEnvironment">Deploy Environment: </label>
        </th>
        <td>
            <props:textProperty name="deployEnvironment" />
            <span class="error" id="error_deployEnvironment"></span>
            <span class="smallNote">The name of the environment in Serena RA to deploy to.</span>
        </td>
    </tr>
    <tr>
        <th>
            <label for="deployProcess">Deploy Process: </label>
        </th>
        <td>
            <props:textProperty name="deployProcess" />
            <span class="error" id="error_deployProcess"></span>
            <span class="smallNote">The name of the application process in Serena RA which will be used to deploy the new version.</span>
        </td>
    </tr>
</l:settingsGroup>
