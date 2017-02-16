package org.jenkinsci.plugins.zanata.zanatareposync;

import java.io.IOException;

import org.junit.Rule;
import org.jvnet.hudson.test.JenkinsRule;
import org.xml.sax.SAXException;

import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.domains.Domain;
import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

/**
 * @author Patrick Huang
 *         <a href="mailto:pahuang@redhat.com">pahuang@redhat.com</a>
 */
public abstract class WithJenkins {
    @Rule
    public JenkinsRule j = new JenkinsRule();

    /**
     *
     * @return HTMLUnit form for global configuration form
     * @throws IOException
     * @throws SAXException
     */
    public final HtmlForm goToGlobalConfigForm()
            throws IOException, SAXException {
        JenkinsRule.WebClient client = configureWebClient();
        HtmlPage p = client.goTo("configure");
        return p.getFormByName("config");
    }

    /**
     *
     * @return HTMLUnit webclient
     */
    public final JenkinsRule.WebClient configureWebClient() {
        JenkinsRule.WebClient client = j.createWebClient();
        client.setJavaScriptEnabled(true);
        return client;
    }

    /**
     * Add a standard username password credential to the Jenkins instance.
     *
     * @param username
     *            username
     * @param password
     *            password
     * @param credentialId
     *            unique credential id
     * @param description
     *            optional description
     * @throws IOException
     */
    protected void addUsernamePasswordCredential(String username,
            String password, String credentialId, String description)
            throws IOException {
        UsernamePasswordCredentialsImpl zanataCred =
                new UsernamePasswordCredentialsImpl(CredentialsScope.GLOBAL,
                        credentialId, description, username, password);
        CredentialsProvider.lookupStores(j.jenkins).iterator().next()
                .addCredentials(Domain.global(), zanataCred);
    }
}
