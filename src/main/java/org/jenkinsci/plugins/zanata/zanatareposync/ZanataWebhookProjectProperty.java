/*
 * Copyright 2016, Red Hat, Inc. and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jenkinsci.plugins.zanata.zanatareposync;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;

import hudson.Extension;
import hudson.model.Job;
import hudson.model.JobProperty;
import hudson.model.JobPropertyDescriptor;
import hudson.util.Secret;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;

/**
 * @author Patrick Huang <a href="mailto:pahuang@redhat.com">pahuang@redhat.com</a>
 */
public class ZanataWebhookProjectProperty extends JobProperty<Job<?, ?>> {
    private static final Logger log =
            LoggerFactory.getLogger(ZanataWebhookProjectProperty.class);

    private final Secret zanataWebhookSecret;
    private final String URLForWebhook;

    @DataBoundConstructor
    public ZanataWebhookProjectProperty(Secret zanataWebhookSecret, String URLForWebhook) {
        this.zanataWebhookSecret = zanataWebhookSecret;
        this.URLForWebhook = URLForWebhook;
    }

    public Secret getZanataWebhookSecret() {
        return this.zanataWebhookSecret;
    }

    public boolean isAcceptZanataWebhook() {
        return !Strings.isNullOrEmpty(URLForWebhook);
    }

    public String getURLForWebhook() {
        return URLForWebhook;
    }

    @Extension
    public static final class DescriptorImpl extends JobPropertyDescriptor {
        public static final String ZANATA_WEBHOOK_PROJECT_BLOCK_NAME = "zanataWebhookProject";
        public static final String URL_PATH = "zanata-webhook";
        private boolean acceptZanataWebhook;

        public boolean isAcceptZanataWebhook() {
            return acceptZanataWebhook;
        }

        // this is needed to keep the optional block (checkbox and its content) persistable (magic...)
        @Override
        public JobProperty<?> newInstance(StaplerRequest req,
                JSONObject formData) throws FormException {
            ZanataWebhookProjectProperty tpp = req.bindJSON(
                    ZanataWebhookProjectProperty.class,
                    formData.getJSONObject(ZANATA_WEBHOOK_PROJECT_BLOCK_NAME)
            );
            if (tpp != null) {
                acceptZanataWebhook = tpp.isAcceptZanataWebhook();
            }
            return tpp;
        }

        @SuppressWarnings("unused")
        // used in ZanataWebhookProjectProperty/config.jelly
        public String urlForWebhook(String jobName) {
            try {
                return Jenkins.getActiveInstance().getRootUrl() + URL_PATH + "/?job=" + URLEncoder.encode(jobName, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                throw new IllegalStateException("encoding is not supported");
            }
        }

        @Override
        public String getDisplayName() {
            return "Zanata Webhook property";
        }

    }
}
