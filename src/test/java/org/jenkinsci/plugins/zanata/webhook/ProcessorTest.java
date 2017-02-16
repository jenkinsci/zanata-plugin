package org.jenkinsci.plugins.zanata.webhook;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import org.jenkinsci.plugins.zanata.zanatareposync.WithJenkins;
import org.junit.Before;
import org.junit.Test;
import org.jvnet.hudson.test.TestBuilder;
import org.jvnet.hudson.test.WithoutJenkins;
import org.mockito.Mockito;

import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Job;
import hudson.model.Result;
import hudson.util.OneShotEvent;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;

public class ProcessorTest extends WithJenkins {

    private String remoteHost;
    private JSONObject payload;
    private String jobName;

    @Before
    public void setUp() throws Exception {
        remoteHost = "http://zanata";
        payload = new JSONObject();
        jobName = "jobName";
    }

    @Test
    @WithoutJenkins
    public void willNotRunJobIfNoRunningJenkins() {
        Job mockJob = Mockito.mock(Job.class);
        Processor processor = new Processor(null, mockJob);

        WebhookResult webhookResult =
                processor.triggerJobs(jobName, remoteHost, payload);

        assertThat(webhookResult.getMessage())
                .isEqualToIgnoringCase("jenkins is not running");
        Mockito.verifyZeroInteractions(mockJob);
    }

    @Test
    @WithoutJenkins
    public void willNotRunJobIfJenkinsIsTerminating() {
        Job mockJob = Mockito.mock(Job.class);
        Jenkins mockJenkins = Mockito.mock(Jenkins.class);
        Processor processor = new Processor(mockJenkins, mockJob);
        when(mockJenkins.isTerminating()).thenReturn(true);

        WebhookResult webhookResult =
                processor.triggerJobs(jobName, remoteHost, payload);

        assertThat(webhookResult.getMessage())
                .isEqualToIgnoringCase("jenkins is not running");
        Mockito.verifyZeroInteractions(mockJob);
    }

    @Test
    public void willBuildTheJobIfJenkinsIsRunning() throws Exception {
        FreeStyleProject project = j.createFreeStyleProject(jobName);
        final OneShotEvent buildStarted = new OneShotEvent();
        project.getBuildersList().add(new TestBuilder() {
            @Override
            public boolean perform(AbstractBuild<?, ?> build, Launcher launcher,
                    BuildListener listener)
                    throws InterruptedException, IOException {
                listener.getLogger().println("all good mate");
                buildStarted.signal();
                return true;
            }
        });

        Processor processor = new Processor(j.jenkins, project);

        String webhookType = "translationMilestone";
        payload.put("type", webhookType);
        String webhookFromProject = "zanataProject";
        payload.put("project", webhookFromProject);
        WebhookResult webhookResult =
                processor.triggerJobs(jobName, remoteHost, payload);

        // 20 seconds
        int timeout = 1000 * 20;
        buildStarted.block(timeout);
        j.waitUntilNoActivityUpTo(timeout);
        assertThat(webhookResult.getStatus()).isEqualTo(200);
        assertThat(webhookResult.getMessage())
                .isEqualToIgnoringCase("job '" + jobName + "' is triggered");
        FreeStyleBuild lastBuild = project.getLastBuild();
        List<String> logs = lastBuild.getLog(10);
        Optional<String> causeLine = logs.stream()
                .filter(line -> line.startsWith("Started by remote host"))
                .findFirst();
        assertThat(causeLine.isPresent()).isTrue();
        assertThat(causeLine.get())
                .contains(remoteHost)
                .contains(webhookType)
                .contains(webhookFromProject);
        assertThat(lastBuild.getResult()).isEqualTo(Result.SUCCESS);
    }

}
