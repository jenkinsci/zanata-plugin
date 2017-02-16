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
package org.jenkinsci.plugins.zanata;

import org.jenkinsci.plugins.zanata.cli.HasSyncJobDetail;
import com.google.common.base.MoreObjects;


/**
 * DTO for everything a sync job requires. Used by tests.
 * @author Patrick Huang <a href="mailto:pahuang@redhat.com">pahuang@redhat.com</a>
 */
public class SyncJobDetail implements HasSyncJobDetail {


    private String zanataURL;

    private String zanataUsername;
    private String zanataSecret;
    private String syncOption;

    private String zanataLocaleIds;

    private String zanataProjectConfigs;


    @Override
    public String getZanataURL() {
        return zanataURL;
    }

    @Override
    public String getZanataUsername() {
        return zanataUsername;
    }

    @Override
    public String getZanataSecret() {
        return zanataSecret;
    }

    @Override
    public String getZanataCredentialsId() {
        throw new UnsupportedOperationException("should not be used");
    }

    @Override
    public String getSyncOption() {
        return syncOption;
    }

    @Override
    public String getZanataLocaleIds() {
        return zanataLocaleIds;
    }

    @Override
    public String getZanataProjectConfigs() {
        return zanataProjectConfigs;
    }

    @Override
    public String describeSyncJob() {
        return toString();
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("zanataURL", zanataURL)
                .add("zanataUsername", zanataUsername)
                .add("zanataSecret", zanataSecret)
                .add("syncOption", syncOption)
                .add("zanataLocaleIds", zanataLocaleIds)
                .add("zanataProjectConfigs", zanataProjectConfigs)
                .toString();
    }


    public static class Builder {
        private final SyncJobDetail syncJobDetail;

        private Builder(SyncJobDetail syncJobDetail) {
            this.syncJobDetail = syncJobDetail;
        }

        public static Builder builder() {
            return new Builder(new SyncJobDetail());
        }


        public Builder setZanataUrl(String zanataUrl) {
            syncJobDetail.zanataURL = zanataUrl;
            return this;
        }

        public Builder setZanataUsername(String zanataUsername) {
            syncJobDetail.zanataUsername = zanataUsername;
            return this;
        }

        public Builder setZanataSecret(String zanataSecret) {
            syncJobDetail.zanataSecret = zanataSecret;
            return this;
        }

        public Builder setSyncToZanataOption(
                String syncToZanataOption) {
            syncJobDetail.syncOption = syncToZanataOption;
            return this;
        }

        public Builder setLocaleId(String localeId) {
            syncJobDetail.zanataLocaleIds = localeId;
            return this;
        }

        public Builder setProjectConfigs(String projectConfigs) {
            syncJobDetail.zanataProjectConfigs = projectConfigs;
            return this;
        }

        public SyncJobDetail build() {
            return syncJobDetail;
        }
    }
}
