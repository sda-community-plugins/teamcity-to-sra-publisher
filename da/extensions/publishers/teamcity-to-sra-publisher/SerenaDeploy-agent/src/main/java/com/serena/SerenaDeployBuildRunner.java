
package com.serena;


import jetbrains.buildServer.RunBuildException;
import jetbrains.buildServer.agent.*;
import org.jetbrains.annotations.*;

public class SerenaDeployBuildRunner implements AgentBuildRunner, AgentBuildRunnerInfo
{
    @NotNull
    public BuildProcess createBuildProcess(@NotNull final AgentRunningBuild runningBuild, @NotNull final BuildRunnerContext context) throws RunBuildException
    {
        return new SerenaDeployBuildProcess(runningBuild, context);
    }

    @NotNull
    public AgentBuildRunnerInfo getRunnerInfo()
    {
        return this;
    }

    @NotNull
    public String getType()
    {
        return PluginConstants.RunType;
    }

    public boolean canRun(@NotNull final BuildAgentConfiguration agentConfiguration)
    {
        return true;
    }
}
