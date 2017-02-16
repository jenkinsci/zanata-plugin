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
package org.jenkinsci.plugins.zanata.webhook;

import org.acegisecurity.context.SecurityContext;
import org.acegisecurity.context.SecurityContextHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.MoreObjects;

import hudson.model.BuildableItem;
import hudson.model.Cause;
import hudson.model.Job;
import hudson.security.ACL;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;

/**
 * @author Patrick Huang
 *         <a href="mailto:pahuang@redhat.com">pahuang@redhat.com</a>
 */
public class Processor {
    private static final Logger log = LoggerFactory.getLogger(Processor.class);
    private final Jenkins jenkins;
    private final Job job;

    public Processor(Jenkins instance, Job job) {
        this.jenkins = instance;
        this.job = job;
    }

    public WebhookResult triggerJobs(String jobName, String remoteHost,
            JSONObject payload) {

        if (jenkins == null || jenkins.isTerminating()) {
            return new WebhookResult(200, "Jenkins is not running");
        }
        if (!(job instanceof BuildableItem)) {
            return new WebhookResult(200, jobName + " is not buildable");

        }
        BuildableItem buildableItem = (BuildableItem) job;

        String webhookType = payload.getString("type");
        String zanataProject = MoreObjects.firstNonNull(payload.getString("project"), "unknown");
        Cause cause = new Cause.RemoteCause(remoteHost,
                String.format("webhook [%s] from project %s", webhookType,
                        zanataProject));

        SecurityContext saveCtx = null;
        try {
            saveCtx = SecurityContextHolder.getContext();
            ACL.impersonate(ACL.SYSTEM);
            buildableItem.scheduleBuild(0, cause);
            return new WebhookResult(200,
                    String.format("Job '%s' is triggered", jobName));
        } catch (Exception e) {
            log.warn("error handling webhook for job: {}", jobName, e);
            return new WebhookResult(500, "Error triggering job " + jobName
                    + " due to " + e.getMessage());
        } finally {
            SecurityContextHolder.setContext(saveCtx);
        }
    }
}
