package org.jenkinsci.plugins.zanata.zanatareposync;

import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;

@Ignore
public class ZanataStepTest {
    @Rule
    public JenkinsRule j = new JenkinsRule();


    @Test
    public void testPipelineDefinition() throws Exception {
        // job setup
        WorkflowJob foo = j.jenkins.createProject(WorkflowJob.class, "foo");

        // TODO we need to set up zanata stub server to test the real pipeline script
        String jenkinsFileContent = Joiner.on("\n").join(ImmutableList.of(
                "node {",
                "  echo 'hi'",
                "}"));
        foo.setDefinition(new CpsFlowDefinition(jenkinsFileContent));

        // get the build going, and wait until workflow pauses
        WorkflowRun b = j.assertBuildStatusSuccess(foo.scheduleBuild2(0).get());

    }

}
