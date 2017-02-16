package org.jenkinsci.plugins.zanata.zanatareposync;

import static org.assertj.core.api.Assertions.assertThat;

import org.jenkinsci.plugins.workflow.structs.DescribableHelper;
import org.junit.Test;

import hudson.model.FreeStyleProject;
import hudson.util.Secret;

public class ZanataWebhookProjectPropertyTest extends WithJenkins {

    @Test
    public void testZanataWebhookProperty() throws Exception {
        FreeStyleProject project = j.createFreeStyleProject();

        // by default no zanata webhook
        j.configRoundtrip(project);
        assertThat(project.getProperty(ZanataWebhookProjectProperty.class))
                .isNull();

        // enable zanata webhook for the project
        String url = "http://jenkins/zanata-webhook/?job=" + project.getName();
        String zanataWebhookSecret = "s3cr3t";
        project.addProperty(
                new ZanataWebhookProjectProperty(Secret.fromString(zanataWebhookSecret), url));
        j.configRoundtrip(project);
        ZanataWebhookProjectProperty prop =
                project.getProperty(ZanataWebhookProjectProperty.class);
        assertThat(prop).isNotNull();
        assertThat(zanataWebhookSecret)
                .isEqualTo(prop.getZanataWebhookSecret().getPlainText());
        assertThat(url).isEqualTo(prop.getURLForWebhook());
        prop = DescribableHelper.instantiate(ZanataWebhookProjectProperty.class,
                DescribableHelper.uninstantiate(prop));
        assertThat(url).isEqualTo(prop.getURLForWebhook());
        assertThat(zanataWebhookSecret)
                .isEqualTo(prop.getZanataWebhookSecret().getPlainText());
        assertThat(prop.getDescriptor().getDisplayName())
                .isEqualTo("Zanata Webhook property");

    }

}
