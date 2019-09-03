package com.serena;

import com.urbancode.commons.fileutils.filelister.FileListerBuilder;
import com.urbancode.vfs.client.Client;
import com.urbancode.vfs.common.ClientChangeSet;
import com.urbancode.vfs.common.ClientPathEntry;
import jetbrains.buildServer.RunBuildException;
import jetbrains.buildServer.agent.AgentRunningBuild;
import jetbrains.buildServer.agent.BuildFinishedStatus;
import jetbrains.buildServer.agent.BuildProgressLogger;
import jetbrains.buildServer.agent.BuildRunnerContext;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.jettison.json.JSONObject;
import org.jetbrains.annotations.NotNull;

import javax.ws.rs.core.Application;
import javax.ws.rs.core.UriBuilder;
import java.io.*;
import java.net.URI;
import java.net.URLDecoder;
import java.util.HashSet;
import java.util.Set;

public class SerenaDeployBuildProcess extends FutureBasedBuildProcess {
    private final AgentRunningBuild myBuild;
    private final BuildRunnerContext myContext;
    private BuildProgressLogger logger;

    public SerenaDeployBuildProcess(@NotNull final AgentRunningBuild build, @NotNull final BuildRunnerContext context) {
        myBuild = build;
        logger = build.getBuildLogger();
        myContext = context;
    }

    private String getParameter(@NotNull final String parameterName) {
        final String value = myContext.getRunnerParameters().get(parameterName);
        if (value == null || value.trim().length() == 0) return null;
        String result = value.trim();
        return result;
    }

    private String getConfigParameter(@NotNull final String parameterName) {
        final String value = myContext.getConfigParameters().get(parameterName);
        if (value == null || value.trim().length() == 0) return null;
        String result = value.trim();
        return result;
    }

    @NotNull
    public BuildFinishedStatus call() throws Exception {
        BuildFinishedStatus toReturn = BuildFinishedStatus.FINISHED_FAILED;

        logger = myBuild.getBuildLogger();

        // parse maven POM to get plugin version
        MavenXpp3Reader reader = new MavenXpp3Reader();
        Model model;
        if ((new File("pom.xml")).exists())
            model = reader.read(new FileReader("pom.xml"));
        else {
            model = reader.read(
                    new InputStreamReader(
                            Application.class.getResourceAsStream(
                                    "/META-INF/maven/com.serena/SerenaDeploy-agent/pom.xml"
                            )
                    )
            );
        }
        String pluginVersion = model.getParent().getVersion();

        Boolean publishVersionToSDA = false;
        Boolean deployAfterUpload = false;
        Boolean addPropertiesToVersion = false;
        Boolean addStatusToVersion = false;
        Boolean addToExistingVersion = false;
        Boolean emptyVersion = false;
        Boolean logRESTCalls = false;
        String sraUrl = getParameter("sraUrl");
        String username = getParameter("username");
        String password = getParameter("password");
        String passwordParameter = getParameter("passwordParameter");
        String publishVersion = getParameter("publishVersion");
        String publishVersionIf = getParameter("publishVersionIf");
        String componentName = getParameter("componentName");
        String baseDir = getParameter("baseDir");
        String dirOffset = getParameter("dirOffset");
        String versionName = getParameter("versionName");
        String includePatterns = getParameter("includePatterns");
        String excludePatterns = getParameter("excludePatterns");
        String versionProperties = getParameter("versionProperties");
        // is addFilesToExistingVersion selected
        String addFilesToExistingVersion = getParameter("addToExistingVersion");
        String createEmptyVersion = getParameter("createEmptyVersion");
        addToExistingVersion = addFilesToExistingVersion != null && !addFilesToExistingVersion.equals("");
        String deployVersion = getParameter("deployVersion");
        String deployVersionIf = getParameter("deployVersionIf");
        // is publishVersionIf override defined
        if (publishVersionIf == null || publishVersionIf.equals("")) {
            // no, check publishVersion parameter
            publishVersionToSDA = publishVersion != null;
        } else {
            // yes, check if it is set to true or yes
            if (publishVersionIf.equals("false") || publishVersionIf.equals("no")) {
                publishVersionToSDA = false;
            }
            if (publishVersionIf.equals("true") || publishVersionIf.equals("yes")) {
                publishVersionToSDA = true;
            }
        }
        // is deployVersionIf override defined
        if (deployVersionIf == null || deployVersionIf.equals("")) {
            // no, check deployVersion parameter
            deployAfterUpload = deployVersion != null;
        } else {
            // yes, check if it is set to true or yes
            if (deployVersionIf.equals("false") || deployVersionIf.equals("no")) {
                deployAfterUpload = false;
            }
            if (deployVersionIf.equals("true") || deployVersionIf.equals("yes")) {
                deployAfterUpload = true;
            }
        }
        emptyVersion = createEmptyVersion != null && !createEmptyVersion.equals("");
        String deployApplication = getParameter("deployApplication");
        String deployEnvironment = getParameter("deployEnvironment");
        String deployProcess = getParameter("deployProcess");
        String deployProperties = getParameter("deployProperties");
        String addStatus = getParameter("addStatus");
        addStatusToVersion = addStatus != null && !addStatus.equals("");
        String statusName = getParameter("statusName");
        // is logRESTCalls defined
        String restCallsString = getParameter("logRESTCalls");
        logRESTCalls = restCallsString != null && !restCallsString.equals("");

        String jsonVersionProperties = "{";
        String jsonDeployProperties = "{";

        //
        // Display plugin configuration
        //

        // log general settings
        logger.targetStarted("Retrieving Configuration");
        logger.progressMessage("Micro Focus DA Plugin version: " + pluginVersion);
        logger.progressMessage("Micro Focus DA URL: " + sraUrl);
        logger.progressMessage("Username: " + username);
        logger.progressMessage("Password: " +
                ((password == null || password.equals("")) ? "not set" : "******"));
        logger.progressMessage("Password Parameter: " +
                ((passwordParameter == null || passwordParameter.equals("")) ? "not set" : passwordParameter));
        logger.progressMessage("Publish Version: " + (publishVersionToSDA ? "is true" : "is false"));
        logger.progressMessage("Publish Version if: " +
                ((publishVersionIf == null || publishVersionIf.equals("")) ? "not set" : publishVersionIf));
        logger.progressMessage("Component: " + componentName);

        logger.progressMessage("Base Directory: " + baseDir);
        if (dirOffset != null)
            logger.progressMessage("Directory Offset: " + dirOffset);
        logger.progressMessage("Version Name: " + versionName);
        logger.progressMessage("Include Patterns: " + includePatterns);
        if (excludePatterns != null)
            logger.progressMessage("Exclude Patterns: " + excludePatterns);
        if (versionProperties == null) {
            addPropertiesToVersion = false;
            logger.progressMessage("Version Properties: none defined");
        } else {
            addPropertiesToVersion = true;
            // iterate over properties
            BufferedReader bufReader = new BufferedReader(new StringReader(versionProperties));
            String line = null, propName = null, propVal = null;
            while ((line = bufReader.readLine()) != null) {
                String[] parts = line.split("=");
                logger.progressMessage("Version Property: " + parts[0] + " = " + parts[1]);
                jsonVersionProperties += ("\"" + parts[0] + "\": \"" + parts[1] + "\",");
            }
            // remove last comma if it exists
            if (jsonVersionProperties.endsWith(","))
                jsonVersionProperties = jsonVersionProperties.substring(0, jsonVersionProperties.length() - 1);
        }
        jsonVersionProperties += "}";
        logger.progressMessage("Add Files to Existing Version: " + (addToExistingVersion ? "is true" : "is false"));

        // log deploy settings
        logger.progressMessage("Deploy Version: " + (deployAfterUpload ? "is true" : "is false"));
        logger.progressMessage("Deploy Version if: " +
                ((deployVersionIf == null || deployVersionIf.equals("")) ? "not set" : deployVersionIf));
        if (deployAfterUpload) {
            logger.progressMessage("Deploy Application: " + deployApplication);
            logger.progressMessage("Deploy Environment: " + deployEnvironment);
            logger.progressMessage("Deploy Process: " + deployProcess);
            if (deployProperties == null) {
                logger.progressMessage("Deploy Properties: none defined");
            } else {
                // iterate over properties
                BufferedReader bufReader = new BufferedReader(new StringReader(deployProperties));
                String line = null, propName = null, propVal = null;
                while ((line = bufReader.readLine()) != null) {
                    String[] parts = line.split("=");
                    logger.progressMessage("Deploy Property: " + parts[0] + " = " + parts[1]);
                    jsonDeployProperties += ("\"" + parts[0] + "\": \"" + parts[1] + "\",");
                }
                // remove last comma if it exists
                if (jsonDeployProperties.endsWith(","))
                    jsonDeployProperties = jsonDeployProperties.substring(0, jsonDeployProperties.length() - 1);
            }
        }
        jsonDeployProperties += "}";

        // log status settings
        logger.progressMessage("Add Status to Version: " + (addStatusToVersion ? "is true" : "is false"));
        if (addStatusToVersion) {
            logger.progressMessage("Status Name: " + statusName);
        }
        logger.progressMessage("Create Empty Version: " + (emptyVersion ? "is true" : "is false"));
        logger.progressMessage("Log REST Calls: " + (logRESTCalls ? "is true" : "is false"));

        //
        // Validate the configuration
        //

        logger.progressMessage("Validating configuration");
        if (!(passwordParameter.equals(null) || passwordParameter.equals(""))) password = passwordParameter;
        SRAHelper sraHelper = new SRAHelper(sraUrl, username, password);
        sraHelper.setLogRestCalls(logRESTCalls);
        sraHelper.setBuildProgressLogger(logger);
        try {
            sraHelper.verifyConnection();
        } catch (Exception ex) {
            throw new RunBuildException("Error connecting to Micro Focus DA:", ex);
        }
        logger.progressMessage("Successfully connected to Micro Focus DA");

        if (!sraHelper.getAllComponentNames().contains(componentName)) {
            throw new RunBuildException("The component \"" + componentName + "\" was not found, aborting...");
        }
        String componentId = sraHelper.getComponentId(componentName);
        logger.progressMessage("Found component \"" + componentName + "\". Using component id: " + componentId);

        if (deployAfterUpload) {
            if (!sraHelper.getAllApplicationNames().contains(deployApplication)) {
                throw new RunBuildException("The application \"" + deployApplication + "\" was not found, aborting...");
            } else logger.progressMessage("Found application \"" + deployApplication);
            if (!sraHelper.getApplicationProcessNamesOrAllApplicationProcessNames(deployApplication).contains(deployProcess)) {
                throw new RunBuildException("The application process \"" + deployProcess + "\" was not found, aborting...");
            } else logger.progressMessage("Found application process \"" + deployProcess);
            if (!sraHelper.getApplicationEnvironmentNamesOrAllEnvironmentNames(deployApplication).contains(deployEnvironment)) {
                throw new RunBuildException("The application environment \"" + deployEnvironment + "\" was not found, aborting...");
            } else logger.progressMessage("Found application environment \"" + deployEnvironment);
        }

        if (addStatusToVersion) {
            if (!sraHelper.getAllVersionStatusNames().contains(statusName)) {
                throw new RunBuildException("The version status \"" + statusName + "\" was not found, aborting...");
            } else logger.progressMessage("Found version status \"" + statusName);
        }
        logger.targetFinished("Retrieving Configuration");

        UriBuilder uriBuilder = null;
        URI uri = null;
        String versionId = null;

        //
        // Publish version to DA
        //

        if (publishVersionToSDA) {

            logger.targetStarted("Publishing Version");

            // check if version already exists
            versionId = sraHelper.getComponentVersionId(componentId, versionName);
            if (versionId == null || versionId.trim().length() == 0) { // NO
                if (addToExistingVersion) { // if we are adding to existing version then abort
                    logger.progressMessage("The option \"Add Files to Existing Version\" is selected but " +
                            "the component version \"" + versionName + "\" does not exist!");
                    return toReturn;
                }
            } else { // YES
                logger.progressMessage("Component version \"" + versionName + "\" exists with id: " + versionId);
                if (!addToExistingVersion) { // if we are adding to existing version then abort
                    throw new RunBuildException("Component version \"" + versionName + "\" already exists and the " +
                            "option \"Add Files to Existing Version\" is not selected, aborting...");
                }
            }

            File workDir = new File(baseDir);
            if (!workDir.exists()) {
                throw new RunBuildException("Base artifact directory " + workDir.toString() + " does not exist!");
            }
            if (dirOffset != null && dirOffset.trim().length() > 0) {
                workDir = new File(workDir, dirOffset.trim());
            }

            Set<String> includesSet = new HashSet<String>();
            Set<String> excludesSet = new HashSet<String>();
            for (String pattern : includePatterns.split(",")) {
                if (pattern != null && pattern.trim().length() > 0) {
                    includesSet.add(pattern.trim());
                }
            }

            if (excludePatterns != null) {
                for (String pattern : excludePatterns.split(",")) {
                    if (pattern != null && pattern.trim().length() > 0) {
                        excludesSet.add(pattern.trim());
                    }
                }
            }

            String[] includesArray = new String[includesSet.size()];
            includesArray = includesSet.toArray(includesArray);

            String[] excludesArray = new String[excludesSet.size()];
            excludesArray = excludesSet.toArray(excludesArray);

            // create component version
            if (addToExistingVersion) {
                // no need to create version as it already exists
                logger.progressMessage("Adding files to existing version \"" + versionName + "\"");
            } else {
                try {
                    uriBuilder = UriBuilder.fromPath(sraUrl).path("cli").path("version")
                            .path("createVersion");

                    uriBuilder.queryParam("component", componentName);
                    uriBuilder.queryParam("name", versionName);
                    uri = uriBuilder.build();

                    logger.progressMessage("Creating version \"" + versionName +
                            "\" on component " + componentName + "...");
                    //logger.progressMessage("Calling URI \"" + uri.toString() + "\"");
                    String verJson = sraHelper.executeJSONPost(uri, null);
                    JSONObject verObj = new JSONObject(verJson);
                    versionId = verObj.getString("id");
                    logger.progressMessage("Unique version id is " + versionId);
                } catch (Exception ex) {
                    throw new RunBuildException("Couldn't create component version - does it already exist?: " + ex);
                }
            }

            // upload Files
            if (emptyVersion) {
                // no need to upload files as we are creating an empty version
                logger.progressMessage("Creating empty version; not uploading any files...");
            } else {
                Client client = null;
                String stageId = null;
                String repositoryId = null;
                try {
                    ClientPathEntry[] entries = ClientPathEntry
                            .createPathEntriesFromFileSystem(workDir, includesArray, excludesArray,
                                    FileListerBuilder.Directories.INCLUDE_ALL, FileListerBuilder.Permissions.BEST_EFFORT,
                                    FileListerBuilder.Symlinks.AS_LINK, "SHA-256");

                    logger.progressMessage("Invoke vfs client");
                    client = new Client(sraUrl + "/vfs", null, null);
                    logger.progressMessage("Creating staging directory");
                    stageId = client.createStagingDirectory();
                    logger.progressMessage("Created staging directory: " + stageId);

                    if (entries.length > 0) {

                        for (ClientPathEntry entry : entries) {
                            File entryFile = new File(workDir, entry.getPath());
                            client.addFileToStagingDirectory(stageId, entry.getPath(), entryFile);
                        }
                        logger.progressMessage("Added " + entries.length + " file(s) to staging directory");

                        repositoryId = sraHelper.getComponentRepositoryId(componentName);
                        String commitHashId = null;
                        if (addToExistingVersion) {
                            logger.progressMessage("Retrieving existing change set for: " + repositoryId + "-" + versionName);
                            ClientChangeSet existingChangeSet = null;
                            try {
                                existingChangeSet = client.getChangeSetByLabel(repositoryId, versionName);
                            } catch (Exception ex) {
                                logger.progressMessage("Unable to find change set with label: " + versionName + " in repository"
                                        + repositoryId + " was an empty version previously created?");
                            }
                            if (existingChangeSet != null) {
                                logger.progressMessage("Merging with existing change set with " + existingChangeSet.getEntries().length + " entries");
                                ClientPathEntry[] newEntries = sraHelper.mergePathEntriesWithCurrentChangeSet(entries, existingChangeSet);
                                ClientChangeSet newChangeSet = ClientChangeSet.newChangeSet(repositoryId, username, "Updated by TeamCity", newEntries);
                                logger.progressMessage("Created new change set with " + newChangeSet.getEntries().length + " entries");
                                commitHashId = client.commitStagingDirectory(stageId, newChangeSet);
                            } else {
                                logger.progressMessage("Creating new change set for repository: " + repositoryId);
                                ClientChangeSet changeSet = ClientChangeSet.newChangeSet(repositoryId, username, "Created by TeamCity", entries);
                                logger.progressMessage("Change set has " + changeSet.getEntries().length + " entries");
                                commitHashId = client.commitStagingDirectory(stageId, changeSet);
                            }
                        } else {
                            logger.progressMessage("Creating new change set for repository: " + repositoryId);
                            ClientChangeSet changeSet = ClientChangeSet.newChangeSet(repositoryId, username, "Created by TeamCity", entries);
                            logger.progressMessage("Change set has " + changeSet.getEntries().length + " entries");
                            commitHashId = client.commitStagingDirectory(stageId, changeSet);
                        }
                        logger.progressMessage("Committed staging directory with hash: " + commitHashId);
                        logger.progressMessage("Labeling change set with label: " + versionName);
                        client.labelChangeSet(repositoryId, URLDecoder.decode(commitHashId, "UTF-8"), versionName,
                                username, "Associated with version " + versionName);
                        logger.progressMessage("Done labeling change set");
                    } else {
                        logger.progressMessage("Did not find any files to upload!");
                    }
                } catch (Throwable ex) {
                    throw new RunBuildException("Failed to upload files: ", ex);
                } finally {
                    if (client != null && stageId != null) {
                        try {
                            //client.deleteStagingDirectory(stageId);
                            logger.progressMessage("Deleted staging directory: " + stageId);
                        } catch (Exception ex) {
                            throw new RunBuildException("Failed to delete staging directory " + stageId, ex);
                        }
                    }

                }
            }

            logger.targetFinished("Publishing Version");


            // add properties to version
            if (addPropertiesToVersion) {
                logger.targetStarted("Adding Properties to Version");

                // get property sheet id
                String propSheetId = sraHelper.getComponentVersionPropsheetId(versionId);
                logger.progressMessage("Found component version property sheet id: " + propSheetId);

                // put properties
                //String compVerPropsBody = "{\"build.number\":\"" + tcBuildNumber + "\"" + ", \"build.vcs.number\":\"" + tcVcNumber + "\"}";
                String encodedPropSheetId = "components%26" + componentId + "%26versions%26" + versionId + "%26propSheetGroup%26propSheets%26"
                        + propSheetId + ".-1/allPropValues";
                uriBuilder = UriBuilder.fromPath(sraUrl).path("property").path("propSheet").path(encodedPropSheetId);
                uri = uriBuilder.build();
                //logger.progressMessage("Calling URI \"" + uri.toString() + "\" with body: \"" + jsonVersionProperties + "\"");
                sraHelper.executeJSONPut(uri, jsonVersionProperties);

                logger.targetFinished("Adding Properties to Version");
            }


            // add status to version
            if (addStatusToVersion) {
                logger.targetStarted("Adding Status to Version");
                uriBuilder = UriBuilder.fromPath(sraUrl).path("rest").path("deploy").path("version")
                        .path(versionId).path("status").path(statusName);
                uri = uriBuilder.build();
                String verStatusBody = "{\"status\":\"" + statusName + "\"}";

                logger.progressMessage("Applying status: \"" + statusName +
                        "\" to Version " + versionName);
                //logger.progressMessage("Calling URI: \"" + uri.toString() + "\"...");
                sraHelper.executeJSONPut(uri, verStatusBody);
                logger.targetFinished("Adding Status to Version");
            }
        }


        //
        // Deploy published version
        //

        if (deployAfterUpload) {
            logger.targetStarted("Deploying Version");
            logger.progressMessage("Starting deployment of application \"" + deployApplication + "\" to environment: \""
                    + deployEnvironment + "\"");
            String deployJson = sraHelper.createProcessRequest(componentName, versionName, jsonDeployProperties,
                    deployApplication, deployEnvironment, deployProcess);
            JSONObject deployObj = new JSONObject(deployJson);
            String requestId = deployObj.getString("requestId");
            logger.progressMessage("Deployment request URI is: \"" + sraUrl + "/app#/application-process-request/" + requestId + "/log\"");
            logger.targetFinished("Deploying Version");
        }

        return BuildFinishedStatus.FINISHED_SUCCESS;
    }

}
