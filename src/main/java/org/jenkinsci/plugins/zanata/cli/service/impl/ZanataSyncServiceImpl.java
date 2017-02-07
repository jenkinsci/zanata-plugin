/*
 * Copyright 2015, Red Hat, Inc. and individual contributors
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
package org.jenkinsci.plugins.zanata.cli.service.impl;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;

import org.jenkinsci.plugins.zanata.cli.HasSyncJobDetail;
import org.jenkinsci.plugins.zanata.cli.service.ZanataSyncService;
import org.jenkinsci.plugins.zanata.cli.util.PushPullOptionsUtil;
import org.jenkinsci.plugins.zanata.exception.ZanataSyncException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zanata.client.commands.PushPullOptions;
import org.zanata.client.commands.pull.PullOptions;
import org.zanata.client.commands.pull.PullOptionsImpl;
import org.zanata.client.commands.push.PushOptions;
import org.zanata.client.commands.push.PushOptionsImpl;

import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;

/**
 * @author Patrick Huang <a href="mailto:pahuang@redhat.com">pahuang@redhat.com</a>
 */
public class ZanataSyncServiceImpl implements ZanataSyncService {

    private static final Logger log =
            LoggerFactory.getLogger(ZanataSyncServiceImpl.class);
    private static final long serialVersionUID = 1L;

    private final transient PullOptions pullOptions;
    private final transient PushOptions pushOptions;

    private final PushServiceImpl pushService = new PushServiceImpl();
    private final PullServiceImpl pullService = new PullServiceImpl();
    private final String zanataUrl;
    private final Set<String> projectConfigs;

//    public ZanataSyncServiceImpl(SyncJobDetail jobDetail) {
//        String zanataUrl = jobDetail.getZanataUrl();
//        String username = jobDetail.getZanataUsername();
//        String apiKey = jobDetail.getZanataSecret();
//        String syncToZanataOption = jobDetail.getSyncToZanataOption();
//        String pushToZanataOption = Strings.emptyToNull(syncToZanataOption);
//        projectConfigs = getProjectConfigs(jobDetail.getProjectConfigs());
//
//        String localeId = jobDetail.getLocaleId();
//        this.zanataUrl = zanataUrl;
//        PullOptionsImpl pullOptions = new PullOptionsImpl();
//        PushOptionsImpl pushOptions = new PushOptionsImpl();
//        pullOptions.setInteractiveMode(false);
//        pushOptions.setInteractiveMode(false);
//        pullOptions.setUsername(username);
//        pullOptions.setKey(apiKey);
//        pushOptions.setUsername(username);
//        pushOptions.setKey(apiKey);
//        pushOptions.setPushType(pushToZanataOption);
//        // TODO until https://zanata.atlassian.net/browse/ZNTA-1427 is fixed we can't trust etag cache
//        pullOptions.setUseCache(false);
//
//        this.pushOptions = pushOptions;
//        this.pullOptions = pullOptions;
////        this.pushOptions.setLogHttp(true);
////        this.pullOptions.setLogHttp(true);
//        // if localeId is given, only handle this locale
//        if (!Strings.isNullOrEmpty(localeId)) {
//            pullOptions.setLocales(localeId);
//            pushOptions.setLocales(localeId);
//        }
//        // if project id is given, only handle this project
////        String projectId = jobDetail.getProject();
////        if (!Strings.isNullOrEmpty(projectId)) {
////            pullOptions.setProj(projectId);
////            pushOptions.setProj(projectId);
////        }
//
//    }

    public ZanataSyncServiceImpl(HasSyncJobDetail jobDetail) {
        String zanataUrl = jobDetail.getZanataURL();
        String syncToZanataOption = jobDetail.getSyncOption();
        String pushToZanataOption = Strings.emptyToNull(syncToZanataOption);
        String username = jobDetail.getZanataUsername();
        String apiKey = jobDetail.getZanataSecret();
        projectConfigs = getProjectConfigs(jobDetail.getZanataProjectConfigs());

        String localeId = jobDetail.getZanataLocaleIds();
        this.zanataUrl = zanataUrl;
        PullOptionsImpl pullOptions = new PullOptionsImpl();
        PushOptionsImpl pushOptions = new PushOptionsImpl();
        pullOptions.setInteractiveMode(false);
        pushOptions.setInteractiveMode(false);
        pullOptions.setUsername(username);
        pullOptions.setKey(apiKey);
        pushOptions.setUsername(username);
        pushOptions.setKey(apiKey);
        pushOptions.setPushType(pushToZanataOption);
        // TODO until https://zanata.atlassian.net/browse/ZNTA-1427 is fixed we can't trust etag cache
        pullOptions.setUseCache(false);

        this.pushOptions = pushOptions;
        this.pullOptions = pullOptions;
//        this.pushOptions.setLogHttp(true);
//        this.pullOptions.setLogHttp(true);
        // if localeId is given, only handle this locale
        if (!Strings.isNullOrEmpty(localeId)) {
            pullOptions.setLocales(localeId);
            pushOptions.setLocales(localeId);
        }
        // if project id is given, only handle this project
//        String projectId = jobDetail.getProject();
//        if (!Strings.isNullOrEmpty(projectId)) {
//            pullOptions.setProj(projectId);
//            pushOptions.setProj(projectId);
//        }
    }

    private static Set<String> getProjectConfigs(String projectConfigs) {
        if (Strings.isNullOrEmpty(projectConfigs)) {
            return Collections.emptySet();
        }
        return ImmutableSet
                .copyOf(Splitter.on(",").trimResults().omitEmptyStrings()
                        .split(projectConfigs));
    }

    @Override
    public PullOptions getPullOptions() {
        return pullOptions;
    }

    @Override
    public PushOptions getPushOptions() {
        return pushOptions;
    }

    @Override
    public void pushToZanata(Path repoBase) throws ZanataSyncException {
        String project = getPushOptions().getProj();
        if (projectConfigs.isEmpty()) {
            Set<File> projectConfigs = findProjectConfigsOrThrow(repoBase);
            for (File config : projectConfigs) {
                PushPullOptionsUtil
                        .applyProjectConfig(getPushOptions(), config);
                log.info("{} - {}", getPushOptions());
                pushIfProjectIdMatchesConfig(project, config);
            }
        } else {
            for (String projectConfig : projectConfigs) {
                Path absPath = Paths.get(repoBase.toString(), projectConfig);
                PushPullOptionsUtil.applyProjectConfig(getPushOptions(), absPath.toFile());
                pushIfProjectIdMatchesConfig(project, absPath.toFile());
            }
        }
    }

    private void pushIfProjectIdMatchesConfig(String project, File config) {
        if (Strings.isNullOrEmpty(project) || Objects.equals(getPushOptions().getProj(), project)) {
            overrideURLIfSpecified(getPushOptions(), zanataUrl);
            pushService.pushToZanata(getPushOptions());
        } else if (!Strings.isNullOrEmpty(project)) {
            log.warn(
                    "project id is provided as {}. Skip {} which has project set to {}",
                    config, getPushOptions().getProj());
        }
    }

    private static void overrideURLIfSpecified(PushPullOptions opts,
            String zanataUrl) {
        if (!Strings.isNullOrEmpty(zanataUrl)) {
            try {
                opts.setUrl(new URL(zanataUrl));
            } catch (MalformedURLException e) {
                log.warn("{} is malformed", zanataUrl);
                throw new IllegalArgumentException(zanataUrl + " is malformed");
            }
        }
    }

    private Set<File> findProjectConfigsOrThrow(Path repoBase) {
        Set<File> projectConfigs =
                PushPullOptionsUtil.findProjectConfigs(repoBase.toFile());

        log.info("found {} in {}", projectConfigs, repoBase);
        if (projectConfigs.isEmpty()) {
            throw new ZanataSyncException(
                    "can not find project config (zanata.xml) in the repo");
        }
        return projectConfigs;
    }

    @Override
    public void pullFromZanata(Path repoBase) throws ZanataSyncException {
        String project = getPullOptions().getProj();
        if (projectConfigs.isEmpty()) {
            Set<File> projectConfigs =
                    findProjectConfigsOrThrow(repoBase);
            for (File config : projectConfigs) {
                PushPullOptionsUtil
                        .applyProjectConfig(getPullOptions(), config);
                pullIfProjectIdMatchesConfig(project, config);
            }
        } else {
            for (String projectConfig : projectConfigs) {
                Path absPath = Paths.get(repoBase.toString(), projectConfig);
                PushPullOptionsUtil.applyProjectConfig(getPullOptions(), absPath.toFile());
                pullIfProjectIdMatchesConfig(project, absPath.toFile());
            }
        }
    }

    private void pullIfProjectIdMatchesConfig(String project, File config) {
        if (Strings.isNullOrEmpty(project) || Objects.equals(getPushOptions().getProj(), project)) {
            overrideURLIfSpecified(getPullOptions(), zanataUrl);
            pullService.pullFromZanata(getPullOptions());
        } else if (!Strings.isNullOrEmpty(project)) {
            log.warn(
                    "project id is provided as {}. Skip {} which has project set to {}",
                    config, getPushOptions().getProj());
        }
    }
}
