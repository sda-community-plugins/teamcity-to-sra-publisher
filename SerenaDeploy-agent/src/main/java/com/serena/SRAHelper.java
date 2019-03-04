package com.serena;

import com.urbancode.commons.util.https.OpenSSLProtocolSocketFactory;
import com.urbancode.vfs.common.ClientChangeSet;
import com.urbancode.vfs.common.ClientPathEntry;
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
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

import javax.ws.rs.core.UriBuilder;
import java.lang.reflect.Array;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jetbrains.buildServer.agent.BuildProgressLogger;


public class SRAHelper
{

    String sraUrl;
    String username;
    String password;

    public SRAHelper(String sraUrl, String username, String password)
    {
        this.sraUrl = sraUrl;
        this.username = username;
        this.password = password;
    }

    public String getSraUrl() {
        return sraUrl;
    }

    public void setSraUrl(String sraUrl) {
        this.sraUrl = sraUrl;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getComponentId(String componentName)
            throws Exception {
        String result = null;
        URI uri = UriBuilder.fromPath(getSraUrl()).path("rest").path("deploy").path("component").queryParam("name", componentName)
                .build();
        String componentContent = executeJSONGet(uri);
        JSONArray components = new JSONArray(componentContent);
        JSONObject component = components.getJSONObject(0);
        result = component.getString("id");
        return result;
    }

    public String getComponentRepositoryId(String componentName)
            throws Exception {
        String result = null;

        URI uri = UriBuilder.fromPath(getSraUrl()).path("rest").path("deploy").path("component").path(componentName)
                .build();

        String componentContent = executeJSONGet(uri);

        JSONArray properties = new JSONObject(componentContent).getJSONArray("properties");
        if (properties != null) {
            for (int i = 0; i < properties.length(); i++) {
                JSONObject propertyJson = properties.getJSONObject(i);
                String propName = propertyJson.getString("name");
                String propValue = propertyJson.getString("value");

                if ("code_station/repository".equalsIgnoreCase(propName)) {
                    result = propValue.trim();
                    break;
                }
            }
        }

        return result;
    }

    public int getNumComponentVersions(String componentId)
            throws Exception {
        URI uri = UriBuilder.fromPath(getSraUrl()).path("rest").path("deploy").path("component").path(componentId).build();

        String componentJson = executeJSONGet(uri);
        JSONObject componentObj = new JSONObject(componentJson);
        int totalRecords = componentObj.getInt("componentVersionCount");
        return totalRecords;
    }

    public String getComponentVersionId(String componentId, String versionName)
            throws Exception {
        String result = null;

        int numVersions = getNumComponentVersions(componentId);

        System.out.println("##teamcity[message text='Found " + String.valueOf(numVersions) + " versions' status='NORMAL']");
        URI uri = UriBuilder.fromPath(getSraUrl()).path("rest").path("deploy").path("component").path(componentId)
                .path("versionsPaged").queryParam("inactive", "true")
                .queryParam("orderField", "dateCreated")
                .queryParam("pageNumber", "1")
                .queryParam("rowsPerPage", String.valueOf(numVersions))
                .queryParam("sortType", "desc")
                .build();

        String versionsJson = executeJSONGet(uri);
        JSONObject versionsObj = new JSONObject(versionsJson);
        int totalRecords = versionsObj.getInt("totalRecords");
        if (totalRecords > 0) {
            JSONArray verArray = versionsObj.getJSONArray("records");
            // see if we can match exact version name
            for (int i = 0; i < verArray.length(); i++) {
                JSONObject verObj = verArray.getJSONObject(i);
                if (verObj.getString("name").equals(versionName)) {
                    result = verObj.getString("id");
                    break;
                }
            }
        }

        return result;
    }

    public String getComponentVersionPropsheetId(String verId)
        throws Exception {
        URI uri = UriBuilder.fromPath(getSraUrl()).path("rest").path("deploy").path("version")
                .path(verId).build();
        String result = null;

        String versionContent = executeJSONGet(uri);

        JSONArray propSheets = new JSONObject(versionContent).getJSONArray("propSheets");
        if (propSheets != null) {
            JSONObject propertyJson = propSheets.getJSONObject(0);
            result = propertyJson.getString("id").trim();
        }
        return result;
    }

    public String createProcessRequest(String componentName, String versionName, String jsonProperties,
                                     String appName, String envName, String procName)
            throws Exception {
        URI uri = UriBuilder.fromPath(getSraUrl()).path("cli").path("applicationProcessRequest")
                .path("request").build();
        String jsonIn =
                "{\"application\":\"" + appName +
                        "\",\"applicationProcess\":\"" + procName +
                        "\",\"environment\":\"" + envName +
                        "\",\"properties\":" + jsonProperties +
                        ",\"versions\":[{\"version\":\"" + versionName +
                        "\",\"component\":\"" + componentName + "\"}]}";

        String jsonOut = executeJSONPut(uri,jsonIn);
        return jsonOut; // return string so we can log it
    }

    public void verifyConnection() throws Exception {
        URI uri = UriBuilder.fromPath(getSraUrl()).path("rest").path("state").build();
        executeJSONGet(uri);
    }

    public void verifyComponentExists(String componentName) throws Exception {
        URI uri = UriBuilder.fromPath(getSraUrl()).path("cli").path("component").path("info").queryParam("component", componentName).build();
        executeJSONGet(uri);
    }

    public void verifyApplicationExists(String applicationName) throws Exception {
        URI uri = UriBuilder.fromPath(getSraUrl()).path("cli").path("application").path("info").queryParam("application", applicationName).build();
        executeJSONGet(uri);
    }

    public void verifyEnvironmentExists(String environmentName, String applicationName) throws Exception {
        URI uri = UriBuilder.fromPath(getSraUrl()).path("cli").path("environment").path("info").queryParam("environment", environmentName).queryParam("application", applicationName).build();
        executeJSONGet(uri);
    }

    public void verifyApplicationProcessExists(String applicationProcess, String applicationName) throws Exception {
        URI uri = UriBuilder.fromPath(getSraUrl()).path("cli").path("applicationProcess").path("info").queryParam("application", applicationName).queryParam("applicationProcess", applicationProcess).build();
        executeJSONGet(uri);
    }

    public String executeJSONGet(URI uri) throws Exception {
        String result = null;
        HttpClient httpClient = new HttpClient();

        if ("https".equalsIgnoreCase(uri.getScheme())) {
            ProtocolSocketFactory socketFactory = new OpenSSLProtocolSocketFactory();
            Protocol https = new Protocol("https", socketFactory, 443);
            Protocol.registerProtocol("https", https);
        }

        GetMethod method = new GetMethod(uri.toString());
        setDirectSsoInteractionHeader(method);
        try {
            HttpClientParams params = httpClient.getParams();
            params.setAuthenticationPreemptive(true);

            UsernamePasswordCredentials clientCredentials = new UsernamePasswordCredentials(getUsername(), getPassword());
            httpClient.getState().setCredentials(AuthScope.ANY, clientCredentials);

            int responseCode = httpClient.executeMethod(method);
            //if (responseCode < 200 || responseCode < 300) {
            if (responseCode == 401) {
                throw new Exception("Error connecting to Serena DA: Invalid user and/or password");
            }
            else if (responseCode != 200) {
                throw new Exception("Error connecting to Serena DA: " + responseCode);
            }
            else {
                result = method.getResponseBodyAsString();
            }
        }
        finally {
            method.releaseConnection();
        }

        return result;
    }

    public String executeJSONPost(URI uri) throws Exception {
        String result = null;
        HttpClient httpClient = new HttpClient();

        if ("https".equalsIgnoreCase(uri.getScheme())) {
            ProtocolSocketFactory socketFactory = new OpenSSLProtocolSocketFactory();
            Protocol https = new Protocol("https", socketFactory, 443);
            Protocol.registerProtocol("https", https);
        }

        PostMethod method = new PostMethod(uri.toString());
        setDirectSsoInteractionHeader(method);
        method.setRequestHeader("charset", "utf-8");
        try {
            HttpClientParams params = httpClient.getParams();
            params.setAuthenticationPreemptive(true);

            UsernamePasswordCredentials clientCredentials = new UsernamePasswordCredentials(getUsername(), getPassword());
            httpClient.getState().setCredentials(AuthScope.ANY, clientCredentials);

            int responseCode = httpClient.executeMethod(method);

            if (responseCode != 200 ) {
                throw new Exception("Serena DA returned error code: " + responseCode);
            }
            else {
                result = method.getResponseBodyAsString();
            }
        }
        catch (Exception e) {
            throw new Exception("Error connecting to Serena DA: " + e.getMessage());
        }
        finally {
            method.releaseConnection();
        }

        return result;
    }

    public String executeJSONPut(URI uri, String putContents) throws Exception {
        String result = null;
        HttpClient httpClient = new HttpClient();

        if ("https".equalsIgnoreCase(uri.getScheme())) {
            ProtocolSocketFactory socketFactory = new OpenSSLProtocolSocketFactory();
            Protocol https = new Protocol("https", socketFactory, 443);
            Protocol.registerProtocol("https", https);
        }

        PutMethod method = new PutMethod(uri.toString());
        setDirectSsoInteractionHeader(method);
        method.setRequestBody(putContents);
        method.setRequestHeader("Content-Type", "application/json");
        method.setRequestHeader("charset", "utf-8");
        try {
            HttpClientParams params = httpClient.getParams();
            params.setAuthenticationPreemptive(true);

            UsernamePasswordCredentials clientCredentials = new UsernamePasswordCredentials(getUsername(), getPassword());
            httpClient.getState().setCredentials(AuthScope.ANY, clientCredentials);

            int responseCode = httpClient.executeMethod(method);

            //if (responseCode < 200 || responseCode < 300) {
            if (responseCode != 200 && responseCode != 204) {
                throw new Exception("Serena DA returned error code: " + responseCode);
            }
            else {
                result = method.getResponseBodyAsString();
            }
        }
        catch (Exception e) {
            throw new Exception("Error connecting to Serena DA: " + e.getMessage());
        }
        finally {
            method.releaseConnection();
        }

        return result;
    }

    private void setDirectSsoInteractionHeader(HttpMethodBase method) {
        method.setRequestHeader("DirectSsoInteraction", "true");
    }

    public ClientPathEntry[] mergePathEntriesWithCurrentChangeSet(ClientPathEntry[] pathEntries,
                                                                           ClientChangeSet changeSet) {
        ClientPathEntry[] result;
        if (changeSet == null) {
            result = pathEntries;
        }
        else {
            List<ClientPathEntry> fullPathEntries = new ArrayList<ClientPathEntry>();
            Set<String> updatedPaths = new HashSet<String>();
            for (ClientPathEntry pathEntry : pathEntries) {
                fullPathEntries.add(pathEntry);
                updatedPaths.add(pathEntry.getPath());
            }

            for (ClientPathEntry pathEntry : changeSet.getEntries()) {
                if (!updatedPaths.contains(pathEntry.getPath())) {
                    fullPathEntries.add(pathEntry);
                }
            }

            result = fullPathEntries.toArray(new ClientPathEntry[fullPathEntries.size()]);
        }

        return result;
    }

    public boolean isNotEmpty(String str) {
        return  (str != null && str.trim().length() > 0);
    }

}
