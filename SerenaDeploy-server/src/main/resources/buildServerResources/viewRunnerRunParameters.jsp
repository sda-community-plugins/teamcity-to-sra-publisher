<%@ taglib prefix="props" tagdir="/WEB-INF/tags/props" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<jsp:useBean id="propertiesBean" scope="request" type="jetbrains.buildServer.controllers.BasePropertiesBean"/>

<div class="parameter">
    Serena DA URL: <strong><props:displayValue name="sraUrl" /></strong>
</div>

<div class="parameter">
    Username: <strong><props:displayValue name="username" /></strong>
</div>

<div class="parameter">
    Password: <strong><props:displayValue name="password" /></strong>
</div>

<div class="parameter">
    Publish Version if: <strong><props:displayValue name="publishVersionIf" /></strong>
</div>

<c:choose>
    <c:when test="${not empty propertiesBean.properties['publishVersion']}">
        <div class="parameter">
            Component: <strong><props:displayValue name="componentName" /></strong>
        </div>

        <div class="parameter">
            Base Artifact Directory: <strong><props:displayValue name="baseDir" /></strong>
        </div>

        <div class="parameter">
            Directory Offset: <strong><props:displayValue name="dirOffset" /></strong>
        </div>

        <div class="parameter">
            Version Name: <strong><props:displayValue name="versionName" /></strong>
        </div>

        <div class="parameter">
            Includes: <strong><props:displayValue name="includePatterns" /></strong>
        </div>

        <div class="parameter">
            Excludes: <strong><props:displayValue name="excludePatterns" /></strong>
        </div>

        <div class="parameter">
            Version Properties: <strong><props:displayValue name="versionProperties" /></strong>
        </div>
    </c:when>
</c:choose>

<c:choose>
    <c:when test="${not empty propertiesBean.properties['addStatus']}">
        <div class="parameter">
            Add Status to Version: <strong><props:displayValue name="addStatus" /></strong>
        </div>

        <div class="parameter">
            Version Status Name: <strong><props:displayValue name="statusName" /></strong>
        </div>
    </c:when>
</c:choose>

<div class="parameter">
    Deploy Version if: <strong><props:displayValue name="deployVersionIf" /></strong>
</div>

<c:choose>
    <c:when test="${not empty propertiesBean.properties['deployVersion']}">
        <div class="parameter">
            Deploy Application: <strong><props:displayValue name="deployApplication" /></strong>
        </div>

        <div class="parameter">
            Deploy Environment: <strong><props:displayValue name="deployEnvironment" /></strong>
        </div>

        <div class="parameter">
            Deploy Process: <strong><props:displayValue name="deployProcess" /></strong>
        </div>

        <div class="parameter">
            Deploy Properties: <strong><props:displayValue name="deployProperties" /></strong>
        </div>
    </c:when>
</c:choose>
