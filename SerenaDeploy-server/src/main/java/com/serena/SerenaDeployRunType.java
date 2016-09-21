package com.serena;

import com.serena.PluginConstants;
import jetbrains.buildServer.serverSide.*;
import org.hsqldb.lib.StringUtil;
import org.jetbrains.annotations.*;

import java.util.*;

public class SerenaDeployRunType extends RunType
{
    public SerenaDeployRunType(@NotNull final RunTypeRegistry runTypeRegistry)
    {
        runTypeRegistry.registerRunType(this);
    }

    @NotNull
    @Override
    public String getType()
    {
        return PluginConstants.RunType;
    }

    @Override
    public String getDisplayName()
    {
        return PluginConstants.DisplayName;
    }

    @Override
    public String getDescription()
    {
        return PluginConstants.Description;
    }

    @Override
    public String describeParameters(@NotNull final Map<String, String> parameters)
    {
        StringBuilder sb = new StringBuilder();
        //sb.append("sraUrl:");
        //sb.append(parameters.get("sraUrl"));
        //sb.append(" ");
        //sb.append("Component:");
        //sb.append(parameters.get("componentName"));

        return sb.toString();
    }

    @Override
    public PropertiesProcessor getRunnerPropertiesProcessor()
    {
        return new PropertiesProcessor()
        {
            public Collection<InvalidProperty> process(Map<String, String> properties)
            {
                ArrayList<InvalidProperty> toReturn = new ArrayList<InvalidProperty>();
                if (!properties.containsKey("sraUrl") || StringUtil.isEmpty(properties.get("sraUrl")))
                    toReturn.add(new InvalidProperty("sraUrl", "Please enter the Serena DA URL"));
                if (!properties.containsKey("username") || StringUtil.isEmpty(properties.get("username")))
                    toReturn.add(new InvalidProperty("username", "Please enter a username for connecting to Serena DA"));

                if (!StringUtil.isEmpty(properties.get("publishVersion"))) {
                    if (!properties.containsKey("componentName") || StringUtil.isEmpty(properties.get("componentName")))
                        toReturn.add(new InvalidProperty("componentName", "Please enter a component name"));

                    if (!properties.containsKey("versionName") || StringUtil.isEmpty(properties.get("versionName")))
                        toReturn.add(new InvalidProperty("versionName", "Please enter a version name"));

                    if (StringUtil.isEmpty(properties.get("baseDir")))
                        properties.put("baseDir", "%teamcity.build.workingDir%");
                    if (StringUtil.isEmpty(properties.get("includePatterns")))
                        properties.put("includePatterns", "**/*");
                }

                if (!StringUtil.isEmpty(properties.get("deployVersion"))) {
                    if (!properties.containsKey("deployApplication") || StringUtil.isEmpty(properties.get("deployApplication")))
                        toReturn.add(new InvalidProperty("deployApplication", "Please enter an Application"));
                    if (!properties.containsKey("deployEnvironment") || StringUtil.isEmpty(properties.get("deployEnvironment")))
                        toReturn.add(new InvalidProperty("deployEnvironment", "Please enter an Application Environment"));
                    if (!properties.containsKey("deployProcess") || StringUtil.isEmpty(properties.get("deployProcess")))
                        toReturn.add(new InvalidProperty("deployProcess", "Please enter an Application Process"));
                }

                return toReturn;
            }
        };
    }

    @Override
    public String getEditRunnerParamsJspFilePath()
    {
        return "editRunnerRunParameters.jsp";
    }

    @Override
    public String getViewRunnerParamsJspFilePath()
    {
        return "viewRunnerRunParameters.jsp";
    }

    @Override
    public Map<String, String> getDefaultRunnerProperties()
    {
        return null;
    }
}
