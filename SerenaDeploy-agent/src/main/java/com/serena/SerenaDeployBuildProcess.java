package com.serena;

import com.urbancode.commons.fileutils.filelister.FileListerBuilder;
import com.urbancode.vfs.client.Client;
import com.urbancode.vfs.common.ClientChangeSet;
import com.urbancode.vfs.common.ClientPathEntry;
import jetbrains.buildServer.agent.AgentRunningBuild;
import jetbrains.buildServer.agent.BuildFinishedStatus;
import jetbrains.buildServer.agent.BuildProgressLogger;
import jetbrains.buildServer.agent.BuildRunnerContext;
import org.codehaus.jettison.json.JSONObject;
import org.jetbrains.annotations.NotNull;

import javax.ws.rs.core.UriBuilder;
import java.io.BufferedReader;
import java.io.File;
import java.io.StringReader;
import java.net.URI;
import java.net.URLDecoder;
import java.util.HashSet;
import java.util.Set;

public class SerenaDeployBuildProcess extends FutureBasedBuildProcess
{
    private final AgentRunningBuild myBuild;
    private final BuildRunnerContext myContext;
    private BuildProgressLogger logger;

    public SerenaDeployBuildProcess(@NotNull final AgentRunningBuild build, @NotNull final BuildRunnerContext context)
    {
        myBuild = build;
        myContext = context;
    }

    private String getParameter(@NotNull final String parameterName)
    {
        final String value = myContext.getRunnerParameters().get(parameterName);
        if (value == null || value.trim().length() == 0) return null;
        String result = value.trim();
        return result;
    }

    private String getConfigParameter(@NotNull final String parameterName)
    {
        final String value = myContext.getConfigParameters().get(parameterName);
        if (value == null || value.trim().length() == 0) return null;
        String result = value.trim();
        return result;
    }

    @NotNull
    public BuildFinishedStatus call() throws Exception
    {
        BuildFinishedStatus toReturn = BuildFinishedStatus.FINISHED_FAILED;

        logger = myBuild.getBuildLogger();
        Boolean publishVersionToSDA = false;
        Boolean deployAfterUpload = false;
        Boolean addPropertiesToVersion = false;
        Boolean addStatusToVersion = false;
        String sraUrl = getParameter("sraUrl");
        String username = getParameter("username");
        String password = getParameter("password");
        String publishVersion = getParameter("publishVersion");
        String publishVersionIf = getParameter("publishVersionIf");
        String componentName = getParameter("componentName");
        String baseDir = getParameter("baseDir");
        String dirOffset = getParameter("dirOffset");
        String versionName = getParameter("versionName");
        String includePatterns = getParameter("includePatterns");
        String excludePatterns = getParameter("excludePatterns");
        String versionProperties = getParameter("versionProperties");
        String deployVersion = getParameter("deployVersion");
        String deployVersionIf = getParameter("deployVersionIf");
        // is publishVersionIf override defined
        if (publishVersionIf == null || publishVersionIf.equals("")) {
            // no, check publishVersion parameter
            if (publishVersion == null) {
                publishVersionToSDA = false;
            } else {
                publishVersionToSDA = true;
            }
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
            if (deployVersion == null) {
                deployAfterUpload = false;
            } else {
                deployAfterUpload = true;
            }
        } else {
            // yes, check if it is set to true or yes
            if (deployVersionIf.equals("false") || deployVersionIf.equals("no")) {
                deployAfterUpload = false;
            }
            if (deployVersionIf.equals("true") || deployVersionIf.equals("yes")) {
                deployAfterUpload = true;
            }
        }
        String deployApplication = getParameter("deployApplication");
        String deployEnvironment = getParameter("deployEnvironment");
        String deployProcess = getParameter("deployProcess");
        String deployProperties = getParameter("deployProperties");
        String addStatus = getParameter("addStatus");
        if (addStatus == null || addStatus.equals(""))
            addStatusToVersion = false;
        else
            addStatusToVersion = true;
        String statusName = getParameter("statusName");
        String jsonVersionProperties = "{";
        String jsonDeployProperties = "{";

        // log general settings
        logger.progressMessage("Serena DA URL: " + sraUrl);
        logger.progressMessage("Username: " + username);
        logger.progressMessage("Publish Version: " + (publishVersionToSDA ? "true" : "false"));
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
            if (jsonVersionProperties.endsWith(",")) jsonVersionProperties = jsonVersionProperties.substring(0, jsonVersionProperties.length() - 1);
        }
        jsonVersionProperties += "}";

        // log deploy settings
        logger.progressMessage("Deploy Version: " + (deployAfterUpload ? "true" : "false"));
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
                if (jsonDeployProperties.endsWith(",")) jsonDeployProperties = jsonDeployProperties.substring(0, jsonDeployProperties.length() - 1);
            }
        }
        jsonDeployProperties += "}";

        // log status settings
        logger.progressMessage("Add Status to Version: " + (addStatusToVersion ? "is true" : "is false"));
        if (addStatusToVersion) {
            logger.progressMessage("Status Name: " + statusName);
        }

        SRAHelper sraHelper = new SRAHelper(sraUrl, username, password);
        String componentId = sraHelper.getComponentId(componentName);
        logger.progressMessage("Using component id: " + componentId);

        //
        // publish version to SDA
        //
        if (publishVersionToSDA) {
            File workDir = new File(baseDir);
            if (!workDir.exists()) throw new Exception("Base artifact directory " + workDir.toString()
                    + " does not exist!");
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
            includesArray = (String[]) includesSet.toArray(includesArray);

            String[] excludesArray = new String[excludesSet.size()];
            excludesArray = (String[]) excludesSet.toArray(excludesArray);

            // create component version
            UriBuilder uriBuilder = UriBuilder.fromPath(sraUrl).path("cli").path("version")
                    .path("createVersion");

            uriBuilder.queryParam("component", componentName);
            uriBuilder.queryParam("name", versionName);
            URI uri = uriBuilder.build();

            logger.targetStarted("Uploading Version");
            logger.progressMessage("Creating version \"" + versionName +
                    "\" on component " + componentName + "...");
            logger.progressMessage("Calling URI \"" + uri.toString() + "\"...");
            String verJson = sraHelper.executeJSONPost(uri);
            JSONObject verObj = new JSONObject(verJson);
            String verId = verObj.getString("id");
            logger.progressMessage("Unique version id is " + verId);

            // Upload Files
            Client client = null;
            String stageId = null;
            String repositoryId = null;
            try {
                ClientPathEntry[] entries = ClientPathEntry
                        .createPathEntriesFromFileSystem(workDir, includesArray, excludesArray,
                                FileListerBuilder.Directories.INCLUDE_ALL, FileListerBuilder.Permissions.BEST_EFFORT,
                                FileListerBuilder.Symlinks.AS_LINK, "SHA-256");

                //logger.progressMessage("Invoke vfs client...");
                client = new Client(sraUrl + "/vfs", null, null);
                stageId = client.createStagingDirectory();
                logger.progressMessage("Created staging directory: " + stageId);

                if (entries.length > 0) {

                    for (ClientPathEntry entry : entries) {
                        File entryFile = new File(workDir, entry.getPath());

                        client.addFileToStagingDirectory(stageId, entry.getPath(), entryFile);
                    }
                    logger.progressMessage("Added " + entries.length + " files to staging directory...");

                    repositoryId = sraHelper.getComponentRepositoryId(componentName);
                    ClientChangeSet changeSet =
                            ClientChangeSet.newChangeSet(repositoryId, username, "Uploaded by TeamCity", entries);

                    logger.progressMessage("Committing change set...");
                    String changeSetId = client.commitStagingDirectory(stageId, changeSet);
                    logger.progressMessage("Created change set: " + changeSetId);

                    logger.progressMessage("Labeling change set with label: " + versionName);
                    client.labelChangeSet(repositoryId, URLDecoder.decode(changeSetId, "UTF-8"), versionName,
                            username, "Associated with version " + versionName);
                    logger.progressMessage("Done labeling change set!");
                } else {
                    logger.progressMessage("Did not find any files to upload!");
                }
            } catch (Throwable e) {
                throw new Exception("Failed to upload files", e);
            } finally {
                if (client != null && stageId != null) {
                    try {
                        //client.deleteStagingDirectory(stageId);
                        logger.progressMessage("Deleted staging directory: " + stageId);
                    } catch (Exception e) {
                        logger.progressMessage("Failed to delete staging directory " + stageId + ": " + e.getMessage());
                    }
                }

            }

            logger.targetFinished("Uploading Version");

            //
            // Add properties to version
            //
            if (addPropertiesToVersion) {
                logger.targetStarted("Adding properties to Version");

                // get property sheet id
                String propSheetId = sraHelper.getComponentVersionPropsheetId(verId);
                logger.progressMessage("Found component version property sheet id: " + propSheetId);

                // put properties
                //String compVerPropsBody = "{\"build.number\":\"" + tcBuildNumber + "\"" + ", \"build.vcs.number\":\"" + tcVcNumber + "\"}";
                String encodedPropSheetId = "components%26" + componentId + "%26versions%26" + verId + "%26propSheetGroup%26propSheets%26"
                        + propSheetId + ".-1/allPropValues";
                uriBuilder = UriBuilder.fromPath(sraUrl).path("property").path("propSheet").path(encodedPropSheetId);
                uri = uriBuilder.build();
                logger.progressMessage("Calling URI \"" + uri.toString() + "\" with body " + jsonVersionProperties);
                sraHelper.executeJSONPut(uri, jsonVersionProperties);

                logger.targetFinished("Adding properties to Version");
            }
            //
            // Add status to version
            //
            if (addStatusToVersion) {
                logger.targetStarted("Adding Status to Version");
                uriBuilder = UriBuilder.fromPath(sraUrl).path("rest").path("deploy").path("version")
                        .path(verId).path("status").path(statusName);
                uri = uriBuilder.build();
                String verStatusBody = "{\"status\":\"" + statusName + "\"}";

                logger.progressMessage("Applying status \"" + statusName +
                        "\" to Version " + versionName);
                logger.progressMessage("Calling URI \"" + uri.toString() + "\"...");
                sraHelper.executeJSONPut(uri, verStatusBody);
                logger.targetFinished("Adding Status to Version");
            }
        }

        //
        // Deploy uploaded version
        //
        if (deployAfterUpload) {
            logger.targetStarted("Deploying Version");
            logger.progressMessage("Starting deployment of " + deployApplication + " to " + deployEnvironment);
            String deployJson = sraHelper.createProcessRequest(componentName, versionName, jsonDeployProperties,
                    deployApplication, deployEnvironment, deployProcess);
            JSONObject deployObj = new JSONObject(deployJson);
            String requestId = deployObj.getString("requestId");
            logger.progressMessage("Deployment request URI is: " + sraUrl + "/app#/application-process-request/" + requestId + "/log");
            logger.targetFinished("Deploying Version");
        }

        return BuildFinishedStatus.FINISHED_SUCCESS;
    }

}
