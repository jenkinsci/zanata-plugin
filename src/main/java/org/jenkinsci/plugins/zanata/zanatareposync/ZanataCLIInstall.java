/*
 * Copyright 2012, CloudBees Inc., Synopsys Inc. and contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jenkinsci.plugins.zanata.zanatareposync;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import com.google.common.base.Strings;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hudson.Extension;
import hudson.model.Node;
import hudson.model.TaskListener;
import hudson.slaves.NodeSpecific;
import hudson.tools.ToolDescriptor;
import hudson.tools.ToolInstallation;
import hudson.tools.ToolInstaller;
import hudson.tools.ZipExtractionInstaller;
import hudson.util.FormValidation;

/**
 * Add zanata CLI to the node.
 *
 */
@SuppressFBWarnings(value = "SE_NO_SERIALVERSIONID",
        justification = "Actually we do not send the class over the channel. Serial version ID is not required for XStream")
public class ZanataCLIInstall extends ToolInstallation implements
        NodeSpecific<ZanataCLIInstall> {
    private static final String DEFAULT_VERSION = "4.0.0";

    @Nonnull
    private final String version;

    @DataBoundConstructor
    public ZanataCLIInstall(@Nonnull String home, @Nonnull String version,
            @CheckForNull List properties) {
        super("zanata_cli_" + dotToUnderscore(version), home, properties);
        this.version = version;
    }

    private static String dotToUnderscore(String version) {
        if (Strings.isNullOrEmpty(version)) {
            return version;
        }
        return version.replaceAll("\\.|-", "_");
    }

    public String getVersion() {
        return version;
    }

    @Override
    public @Nonnull
    ZanataCLIInstall forNode(Node node, TaskListener log)
            throws IOException, InterruptedException {
        String home = translateFor(node, log);
        return new ZanataCLIInstall(home, getVersion(), getProperties().toList());
    }

    @Extension
    public static class DescriptorImpl extends ToolDescriptor<ZanataCLIInstall> {

        public DescriptorImpl() {
            load();
        }

        @Override
        public String getDisplayName() {
            return "Zanata CLI";
        }

        @Override
        public void setInstallations(ZanataCLIInstall... installations) {
            super.setInstallations(installations);
            save();
        }

        /**
         * Gets a {@link ZanataCLIInstall} by its name.
         * @param name A name of the tool to be retrieved.
         * @return A {@link ZanataCLIInstall} or null if it has no found
         */
        public @CheckForNull
        ZanataCLIInstall byName(String name) {
            for (ZanataCLIInstall tool : getInstallations()) {
                if (tool.getName().equals(name)) {
                    return tool;
                }
            }
            return null;
        }

        @SuppressWarnings("unused")
        public FormValidation doCheckVersion(@QueryParameter String version) {
            if (Strings.isNullOrEmpty(version)) {
                return FormValidation.error("Please enter a version to distinguish this Zanata CLI installation from others");
            }
            if (version.matches("[0-9\\.\\-a-zA-Z_]+")) {
                return FormValidation.ok();
            } else {
                return FormValidation.warning("Please only enter alphanumeric, dot, dash and underscore");
            }
        }

        @Override
        public List<? extends ToolInstaller> getDefaultInstallers() {
            String downloadURL =
                    toDownloadURL(DEFAULT_VERSION);
            return Collections.singletonList(new ZipExtractionInstaller(null,
                    downloadURL, toSubdir(DEFAULT_VERSION)));
        }

        private static String toDownloadURL(String version) {
            return String.format(
                    "https://repo1.maven.org/maven2/org/zanata/zanata-cli/%s/zanata-cli-%s-dist.zip",
                    version, version);
        }
        private static String toSubdir(String version) {
            return "zanata-cli-" + version;
        }
    }

}
