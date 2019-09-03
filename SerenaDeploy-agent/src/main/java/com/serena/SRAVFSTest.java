package com.serena;

import com.urbancode.commons.fileutils.filelister.FileListerBuilder;
import com.urbancode.vfs.client.Client;
import com.urbancode.vfs.common.ClientChangeSet;
import com.urbancode.vfs.common.ClientPathEntry;
import org.codehaus.jettison.json.JSONObject;

import javax.ws.rs.core.UriBuilder;
import java.io.File;
import java.net.URI;
import java.net.URLDecoder;
import java.util.HashSet;
import java.util.Set;

public class SRAVFSTest {
    public static void main(String[] args) {
        System.out.println("VFS Testing");
        boolean addToExistingVersion = true;
        String componentName = "vfs-test";
        String versionName = "1.1";
        String includePatterns = "*.*";
        String excludePatterns = "";
        String baseDir = "C:\\Temp\\components\\vfs-test\\1.0";
        String componentId = null;
        String versionId = null;
        Client client = null;
        SRAHelper helper = new SRAHelper("http://localhost:8080/da", "admin", "admin");
        try {
            helper.verifyConnection();
        } catch (Exception ex) {
            System.out.println(ex.getLocalizedMessage());
        }
        try {
            componentId = helper.getComponentId("vfs-test");
            System.out.println("componentId=" + componentId);
            versionId = helper.getComponentVersionId(componentId, versionName);
            System.out.println("versionId=" + versionId);
            client = new Client("http://localhost:8080/da/vfs", null, null);
        } catch (Exception ex) {
            System.out.println(ex.getLocalizedMessage());
        }

        // create component version
        if (addToExistingVersion) {
            System.out.println("Adding files to existing version \"" + versionName + "\"");
        } else {
            try {
                UriBuilder uriBuilder = UriBuilder.fromPath("http://localhost:8080/da").path("cli").path("version")
                        .path("createVersion");

                uriBuilder.queryParam("component", componentName);
                uriBuilder.queryParam("name", versionName);
                URI uri = uriBuilder.build();

                System.out.println("Creating version \"" + versionName +
                        "\" on component " + componentName + "...");
                System.out.println("Calling URI \"" + uri.toString() + "\"...");
                String verJson = helper.executeJSONPost(uri, null);
                JSONObject verObj = new JSONObject(verJson);
                versionId = verObj.getString("id");
                System.out.println("Unique version id is " + versionId);
            } catch (Exception ex) {
                System.out.println(ex.getLocalizedMessage());
            }
        }

        File workDir = new File(baseDir);
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
        System.out.println("includesArray=" + includesArray.toString());

        String[] excludesArray = new String[excludesSet.size()];
        excludesArray = excludesSet.toArray(excludesArray);
        System.out.println("excludesArray=" + excludesArray.toString());
        String stageId = null;
        String repositoryId = null;
        try {
            ClientPathEntry[] entries = ClientPathEntry
                    .createPathEntriesFromFileSystem(workDir, includesArray, excludesArray,
                            FileListerBuilder.Directories.INCLUDE_ALL, FileListerBuilder.Permissions.BEST_EFFORT,
                            FileListerBuilder.Symlinks.AS_LINK, "SHA-256");

            stageId = client.createStagingDirectory();
            System.out.println("Created staging directory: " + stageId);
            if (entries.length > 0) {
                for (ClientPathEntry entry : entries) {
                    File entryFile = new File(workDir, entry.getPath());
                    client.addFileToStagingDirectory(stageId, entry.getPath(), entryFile);
                }
                System.out.println("Added " + entries.length + " files to staging directory...");

                repositoryId = helper.getComponentRepositoryId(componentName);
                System.out.println("repositoryId=" + repositoryId);
                if (addToExistingVersion) {
                    System.out.println("Retrieving existing change set for: " + repositoryId + "-" + versionName);
                    ClientChangeSet existingChangeSet = client.getChangeSetByLabel(repositoryId, versionName);
                    System.out.println("Merging with existing change set: " + existingChangeSet.toJSON().toString());
                    ClientPathEntry[] newEntries = helper.mergePathEntriesWithCurrentChangeSet(entries, existingChangeSet);
                    ClientChangeSet newChangeSet = ClientChangeSet.newChangeSet(repositoryId, "admin", "Updated by TeamCity", newEntries);
                    System.out.println("Created new change set: " + newChangeSet.toString());
                    String commitHashId = client.commitStagingDirectory(stageId, newChangeSet);
                    System.out.println("Committed staging directory: " + commitHashId);
                    System.out.println("Labeling change set with label: " + versionName);
                    client.labelChangeSet(repositoryId, URLDecoder.decode(commitHashId, "UTF-8"), versionName,
                            "admin", "Associated with version " + versionName);
                    System.out.println("Done labeling change set!");
                } else {
                    System.out.println("in here");
                    System.out.println("Creating new change set for: " + repositoryId);
                    ClientChangeSet changeSet = ClientChangeSet.newChangeSet(repositoryId, "admin", "Created by TeamCity", entries);
                    System.out.println("Created new change set: " + changeSet.toJSON());
                    String commitHashId = client.commitStagingDirectory(stageId, changeSet);
                    System.out.println("Created change set: " + commitHashId);
                    client.labelChangeSet(repositoryId, URLDecoder.decode(commitHashId, "UTF-8"), versionName,
                            "admin", "Associated with version " + versionName);
                    System.out.println("Done labeling change set!");
                }
            } else {
                System.out.println("Did not find any files to upload!");
            }
        } catch (Throwable e) {
            System.out.println("Failed to upload files: " + e.toString());
        } finally {
            if (client != null && stageId != null) {
                try {
                    //client.deleteStagingDirectory(stageId);
                    System.out.println("Deleted staging directory: " + stageId);
                } catch (Exception e) {
                    System.out.println("Failed to delete staging directory " + stageId + ": " + e.getMessage());
                }
            }

        }
    }
}
