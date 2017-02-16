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
package org.jenkinsci.plugins.zanata.cli;

import java.io.Serializable;

import org.apache.commons.lang3.StringUtils;
import com.google.common.base.MoreObjects;
import com.google.common.base.Strings;


/**
 * Everything a sync job requires.
 * @author Patrick Huang <a href="mailto:pahuang@redhat.com">pahuang@redhat.com</a>
 */
public interface HasSyncJobDetail extends Serializable {


    String getZanataURL();

    String getSyncOption();

    String getZanataProjectConfigs();

    String getZanataLocaleIds();

    String getZanataUsername();

    String getZanataSecret();

    String getZanataCredentialsId();

    default String describeSyncJob() {
        return MoreObjects.toStringHelper(this)
                .add("zanataURL", getZanataURL())
                .add("credentialsId", getZanataCredentialsId())
                .add("zanataUsername", getZanataUsername())
                .add("zanataSecret", mask(getZanataSecret()))
                .add("syncOption", getSyncOption())
                .add("zanataLocaleIds", getZanataLocaleIds())
                .add("zanataProjectConfigs", getZanataProjectConfigs())
                .toString();
    }

    static String mask(String secret) {
        if (Strings.isNullOrEmpty(secret)) {
            return secret;
        }
        if (secret.length() < 3) {
            return "*";
        }
        if (secret.length() < 5) {
            // when secret is too short, we only show the first and last character
            return secret.charAt(0) + "*" + secret.substring(secret.length() - 1);
        }
        return StringUtils.abbreviateMiddle(secret, "*", 4);
    }
}
