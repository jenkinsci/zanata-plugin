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

import java.io.IOException;
import java.util.Optional;

import org.acegisecurity.context.SecurityContext;
import org.acegisecurity.context.SecurityContextHolder;
import org.apache.commons.io.IOUtils;
import org.jenkinsci.plugins.zanata.webhook.HmacUtil;
import org.jenkinsci.plugins.zanata.webhook.Processor;
import org.jenkinsci.plugins.zanata.webhook.WebhookResult;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;

import hudson.Extension;
import hudson.model.Job;
import hudson.model.UnprotectedRootAction;
import hudson.security.ACL;
import hudson.util.Secret;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;

/**
 * @author Patrick Huang
 *         <a href="mailto:pahuang@redhat.com">pahuang@redhat.com</a>
 */
@Extension
public class ZanataWebhookJobTrigger implements UnprotectedRootAction {
    private static final org.slf4j.Logger log =
            LoggerFactory.getLogger(ZanataWebhookJobTrigger.class);
    private static final String DEFAULT_CHARSET = "UTF-8";
    private Jenkins jenkins = Jenkins.getInstance();
    private StaplerResponse resp;

    @Override
    public String getDisplayName() {
        return null;
    }

    @Override
    public String getIconFileName() {
        return null;
    }

    @Override
    public String getUrlName() {
        return ZanataWebhookProjectProperty.DescriptorImpl.URL_PATH;
    }

    /**
     * Receives the HTTP POST request send by Zanata webhook.
     *
     * @param req
     *            request
     */
    @SuppressWarnings("unused")
    public void doIndex(StaplerRequest req, StaplerResponse rsp)
            throws IOException {
        this.resp = rsp;

        String jobName = req.getParameter("job");
        if (Strings.isNullOrEmpty(jobName)) {
            exitWebHook(new WebhookResult(404, "Parameter 'job' is missing or no value assigned."));
            return;
        }

        // Get the POST stream
        String body = IOUtils.toString(req.getInputStream(), DEFAULT_CHARSET);
        if (body.isEmpty()
                || !req.getRequestURI().contains("/".concat(ZanataWebhookProjectProperty.DescriptorImpl.URL_PATH).concat("/"))) {
            exitWebHook(new WebhookResult(404, "No payload or URI contains invalid entries."));
            return;
        }

        String contentType = req.getContentType();
        if (contentType == null || !contentType.startsWith("application/json")) {
            exitWebHook(new WebhookResult(415, "Only Accept JSON payload."));
            return;
        }

        JSONObject payload = JSONObject.fromObject(body);

        Secret secretDefinedInJob = null;
        SecurityContext saveCtx = SecurityContextHolder.getContext();

        Optional<Job> foundJob;
        try {
            ACL.impersonate(ACL.SYSTEM);
            foundJob = jenkins.getAllItems(Job.class).stream()
                    .filter(job -> job.getName().equals(jobName) && job.isBuildable())
                    .findFirst();
            if (foundJob.isPresent()) {
                final ZanataWebhookProjectProperty property =
                        (ZanataWebhookProjectProperty) foundJob.get()
                                .getProperty(
                                        ZanataWebhookProjectProperty.class);
                if (property != null) {
                    secretDefinedInJob = property.getZanataWebhookSecret();
                }
            }

        } finally {
            SecurityContextHolder.setContext(saveCtx);
        }

        if (!foundJob.isPresent()) {
            String msg = String.format("Job '%s' is not defined in Jenkins or is not buildable",
                    jobName);
            log.warn(msg);
            exitWebHook(new WebhookResult(404, msg));
            return;
        }

        // Get X-Zanata-Webhook for security check
        String webhookSHA = req.getHeader("X-Zanata-Webhook");

        if (noWebhookSecret(secretDefinedInJob, webhookSHA)
                || webhookSHAMatchesSecret(secretDefinedInJob, payload, req.getRequestURI(),
                        webhookSHA)) {
            Processor payloadProcessor = new Processor(Jenkins.getInstance(), foundJob.get());
            WebhookResult result =
                    payloadProcessor.triggerJobs(jobName, req.getRemoteHost(), payload);
            exitWebHook(result);
        } else {
            exitWebHook(new WebhookResult(403, "Incorrect webhook secret"));
        }

    }

    private static boolean noWebhookSecret(Secret secretDefinedInJob,
            String webhookSHA) {
        return isNullOrEmpty(secretDefinedInJob)
                && Strings.isNullOrEmpty(webhookSHA);
    }

    private static boolean isNullOrEmpty(Secret secretDefinedInJob) {
        return secretDefinedInJob == null
                || Strings.isNullOrEmpty(secretDefinedInJob.getPlainText());
    }

    private static boolean webhookSHAMatchesSecret(Secret secretDefinedInJob,
            JSONObject jsonObject, String requestURI, String webhookSHA) {
        if (isNullOrEmpty(secretDefinedInJob)) {
            return true;
        }
        String valueToDigest = jsonObject.toString() + requestURI;
        try {
            String secretText = secretDefinedInJob.getPlainText();
            String expectedHash = HmacUtil
                    .hmacSha1(secretText, HmacUtil
                            .hmacSha1(secretText, valueToDigest));
            return expectedHash.equals(webhookSHA);
        } catch (IllegalArgumentException e) {
            log.error("Unable to generate hmac sha1");
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * Exit the WebHook.
     *
     * @param result
     *            Zanata webhook result
     */
    private void exitWebHook(WebhookResult result) throws IOException {
        if (result.getStatus() != 200) {
            log.warn(result.getMessage());
        }
        JSONObject json = new JSONObject();
        json.put("result", result.getStatus() == 200 ? "OK" : "ERROR");
        json.put("message", result.getMessage());
        resp.setStatus(result.getStatus());
        resp.addHeader("Content-Type", "application/json");
        resp.getWriter().print(json.toString());
    }

}
