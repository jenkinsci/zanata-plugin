package org.jenkinsci.plugins.zanata.zanatareposync;

import static io.restassured.RestAssured.given;
import static io.restassured.config.EncoderConfig.encoderConfig;
import static io.restassured.config.RestAssuredConfig.newConfig;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.equalToIgnoringCase;

import java.io.IOException;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.TestBuilder;

import com.google.common.base.Charsets;
import com.google.common.net.HttpHeaders;

import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.util.OneShotEvent;
import hudson.util.RunList;
import hudson.util.Secret;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.http.Header;
import io.restassured.specification.RequestSpecification;

public class ZanataWebhookJobTriggerTest {
    private static final Header JSON_CONTENT_TYPE =
            new Header(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON);
    private static final String PAYLOAD =
            "{\"type\":\"milestone\", \"project\":\"zanata-server\"}";

    @ClassRule
    public static JenkinsRule jenkins = new JenkinsRule();
    private RequestSpecification spec;

    @Before
    public void setUp() {
        spec = new RequestSpecBuilder()
                .setBaseUri(jenkins.getInstance().getRootUrl())
                .setBasePath(
                        ZanataWebhookProjectProperty.DescriptorImpl.URL_PATH
                                .concat("/"))
                .setConfig(newConfig().encoderConfig(
                        encoderConfig().defaultContentCharset(Charsets.UTF_8)
                                .appendDefaultContentCharsetToContentTypeIfUndefined(
                                        false)))
                .build();
    }

    @Test
    public void willGetNotFoundIfNoJobName() {
        given().spec(spec)
                .header(JSON_CONTENT_TYPE).body(PAYLOAD).log().all()
                .when().get()
                .then()
                .statusCode(404)
                .body("message", equalTo("Parameter 'job' is missing or no value assigned."));
    }

    @Test
    public void willGetNotFoundIfNoPayload() {
        given().spec(spec)
                .header(JSON_CONTENT_TYPE)
                .param("job", "some")
                .log().all()
                .when().get()
                .then()
                .statusCode(404)
                .body("message", equalTo("No payload or URI contains invalid entries."));
    }

    @Test
    public void willGetMediaUnsupportedIfContentTypeIsNotJSON() {
        given().spec(spec)
                .param("job", "some")
                .body(PAYLOAD).log().all()
                .when().get()
                .then()
                .statusCode(415)
                .body("message", equalTo("Only Accept JSON payload."));
    }

    @Test
    public void willGetNotFoundIfJobNameCanNotBeFound() {
        given().spec(spec)
                .param("job", "NonExistJob")
                .header(JSON_CONTENT_TYPE).body(PAYLOAD)
                .when().get()
                .then()
                .statusCode(404)
                .body("message", equalTo("Job 'NonExistJob' is not defined in Jenkins or is not buildable"));
    }

    @Test
    public void willRunTheJobIfJobNameMatchesAndNoWebhookSecretSet() throws Exception {
        // setup the project on jenkins
        String jobName = "noWebhookSecret";
        FreeStyleProject project =
                jenkins.createFreeStyleProject(jobName);
        project.addProperty(new ZanataWebhookProjectProperty(null, ""));
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

        // before webhook there is no build
        RunList<FreeStyleBuild> builds = project.getBuilds();
        assertThat(builds).isEmpty();

        given().spec(spec)
                .param("job", jobName)
                .header(JSON_CONTENT_TYPE).body(PAYLOAD)
                .when().get()
                .then()
                .statusCode(200)
                .body("message", equalToIgnoringCase("job '" + jobName + "' is triggered"));

        int timeout = 20 * 1000;
        buildStarted.block(timeout);
        jenkins.waitUntilNoActivityUpTo(timeout);

        // after webhook we should have 1 build
        builds = project.getBuilds();
        assertThat(builds).hasSize(1);
    }

    @Test
    public void willRunTheJobIfJobNameAndWebhookSecretBothMatch() throws Exception {
        // setup the project on jenkins
        String jobName = "jobWithWebhookSecret";
        FreeStyleProject project =
                jenkins.createFreeStyleProject(jobName);
        project.addProperty(new ZanataWebhookProjectProperty(Secret.fromString("s3cr3t"), ""));
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

        // before webhook there is no build
        RunList<FreeStyleBuild> builds = project.getBuilds();
        assertThat(builds).isEmpty();

        // header contains expected webhook checksum
        given().spec(spec)
                .param("job", jobName)
                .header("X-Zanata-Webhook", "tSMILvA4juZs1LO5YGgvVh+O9J8=")
                .header(JSON_CONTENT_TYPE).body(PAYLOAD)
                .when().get()
                .then()
                .statusCode(200)
                .body("message", equalToIgnoringCase("job '" + jobName + "' is triggered"));

        int timeout = 20 * 1000;
        buildStarted.block(timeout);
        jenkins.waitUntilNoActivityUpTo(timeout);

        // after webhook we should have 1 build
        builds = project.getBuilds();
        assertThat(builds).hasSize(1);
    }

    @Test
    public void willNotRunTheJobIfJobNameMatchesButWebhookSecretDoesNot() throws Exception {
        // setup the project on jenkins
        String jobName = "jobWithWebhookSecret2";
        FreeStyleProject project =
                jenkins.createFreeStyleProject(jobName);
        project.addProperty(new ZanataWebhookProjectProperty(Secret.fromString("s3cr3t"), ""));

        // before webhook there is no build
        RunList<FreeStyleBuild> builds = project.getBuilds();
        assertThat(builds).isEmpty();

        // header contains a checksum that won't match
        given().spec(spec)
                .param("job", jobName)
                .header("X-Zanata-Webhook", "some_randome_string")
                .header(JSON_CONTENT_TYPE).body(PAYLOAD)
                .when().get()
                .then()
                .statusCode(403)
                .body("message", equalToIgnoringCase("Incorrect webhook secret"));

        int timeout = 20 * 1000;
        jenkins.waitUntilNoActivityUpTo(timeout);

        // after webhook we still have no build
        builds = project.getBuilds();
        assertThat(builds).isEmpty();
    }

}
