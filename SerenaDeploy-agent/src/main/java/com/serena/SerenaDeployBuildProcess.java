package com.serena;

import jetbrains.buildServer.agent.*;
import jetbrains.buildServer.util.StringUtil;
import org.apache.commons.httpclient.HttpMethodBase;
import org.jetbrains.annotations.*;

import java.io.File;
import java.net.URI;
import java.net.URLDecoder;
import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.core.UriBuilder;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethodBase;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.commons.httpclient.params.HttpClientParams;
import org.apache.commons.httpclient.protocol.Protocol;
import org.apache.commons.httpclient.protocol.ProtocolSocketFactory;

import com.urbancode.commons.util.https.OpenSSLProtocolSocketFactory;

import com.urbancode.commons.fileutils.filelister.FileListerBuilder;
import com.urbancode.vfs.client.Client;
import com.urbancode.vfs.common.ClientChangeSet;
import com.urbancode.vfs.common.ClientPathEntry;

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

    @NotNull
    public BuildFinishedStatus call() throws Exception
    {
        BuildFinishedStatus toReturn = BuildFinishedStatus.FINISHED_FAILED;

        logger = myBuild.getBuildLogger();
        Boolean deployAfterUpload;
        String sraUrl = getParameter("sraUrl");
        String username = getParameter("username");
        String password = getParameter("password");
        String componentName = getParameter("componentName");
        String baseDir = getParameter("baseDir");
        String dirOffset = getParameter("dirOffset");
        String versionName = getParameter("versionName");
        String includePatterns = getParameter("includePatterns");
        String excludePatterns = getParameter("excludePatterns");
        String deployVersion = getParameter("deployVersion");
        if (deployVersion == null)
            deployAfterUpload = false;
        else
            deployAfterUpload = true;
        String deployApplication = getParameter("deployApplication");
        String deployEnvironment = getParameter("deployEnvironment");
        String deployProcess = getParameter("deployProcess");

        logger.progressMessage("Serena RA URL: " + sraUrl);
        logger.progressMessage("Username: " + username);
        logger.progressMessage("Component: " + componentName);
        logger.progressMessage("Base Directory: " + baseDir);
        if (dirOffset != null)
            logger.progressMessage("Directory Offset: " + dirOffset);
        logger.progressMessage("Version Name: " + versionName);
        logger.progressMessage("Include Patterns: " + includePatterns);
        if (excludePatterns != null)
            logger.progressMessage("Exclude Patterns: " + excludePatterns);

        logger.progressMessage("Deploy Version: " + (deployAfterUpload ? "selected" : "not selected"));
        if (deployAfterUpload) {
            logger.progressMessage("Deploy Application: " + deployApplication);
            logger.progressMessage("Deploy Environment: " + deployEnvironment);
            logger.progressMessage("Deploy Process: " + deployProcess);
        }

        SRAHelper sraHelper = new SRAHelper(sraUrl, username, password);

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

        //
        // create component version
        //
        UriBuilder uriBuilder = UriBuilder.fromPath(sraUrl).path("cli").path("version")
                .path("createVersion");

        uriBuilder.queryParam("component", componentName);
        uriBuilder.queryParam("name", versionName);
        URI uri = uriBuilder.build();

        logger.progressMessage("Creating new version \"" + versionName +
                "\" on component " + componentName + "...");
        logger.progressMessage("Calling URI \"" + uri.toString() + "\"...");
        sraHelper.executeJSONPost(uri);
        logger.progressMessage("Successfully created new component version.");

        //
        // Upload Files
        //
        logger.progressMessage("Uploading files into version.");
        Client client = null;
        String stageId = null;
        try {
            ClientPathEntry[] entries = ClientPathEntry
                    .createPathEntriesFromFileSystem(workDir, includesArray, excludesArray,
                            FileListerBuilder.Directories.INCLUDE_ALL, FileListerBuilder.Permissions.BEST_EFFORT,
                            FileListerBuilder.Symlinks.AS_LINK, "SHA-256");

            logger.progressMessage("Invoke vfs client...");
            client = new Client(sraUrl + "/vfs", null, null);
            stageId = client.createStagingDirectory();
            logger.progressMessage("Created staging directory: " + stageId);

            if (entries.length > 0) {

                for (ClientPathEntry entry : entries) {
                    File entryFile = new File(workDir, entry.getPath());
                    logger.progressMessage("Adding " + entry.getPath() + " to staging directory...");
                    client.addFileToStagingDirectory(stageId, entry.getPath(), entryFile);
                }

                String repositoryId = sraHelper.getComponentRepositoryId(componentName);
                ClientChangeSet changeSet =
                        ClientChangeSet.newChangeSet(repositoryId, username, "Uploaded by TeamCity", entries);

                logger.progressMessage("Committing change set...");
                String changeSetId = client.commitStagingDirectory(stageId, changeSet);
                logger.progressMessage("Created change set: " + changeSetId);

                logger.progressMessage("Labeling change set with label: " + versionName);
                client.labelChangeSet(repositoryId, URLDecoder.decode(changeSetId, "UTF-8"), versionName,
                        username, "Associated with version " + versionName);
                logger.progressMessage("Done labeling change set!");
            }
            else {
                logger.progressMessage("Did not find any files to upload!");
            }
        }
        catch (Throwable e) {
            throw new Exception("Failed to upload files", e);
        }
        finally {
            if (client != null && stageId != null) {
                try {
                    client.deleteStagingDirectory(stageId);
                    logger.progressMessage("Deleted staging directory: " + stageId);
                }
                catch (Exception e) {
                    logger.progressMessage("Failed to delete staging directory " + stageId + ": " + e.getMessage());
                }
            }

        }

        //
        // Deploy uploaded version
        //
        if (deployAfterUpload) {
            logger.progressMessage("Starting deployment of " + deployApplication + " to " + deployEnvironment);
            sraHelper.createProcessRequest(componentName, versionName,
                    deployApplication, deployEnvironment, deployProcess);
        }

        return BuildFinishedStatus.FINISHED_SUCCESS;
    }

}
