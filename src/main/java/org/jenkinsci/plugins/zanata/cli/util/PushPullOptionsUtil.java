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
package org.jenkinsci.plugins.zanata.cli.util;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.xml.bind.JAXBException;

import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.plugins.interceptors.CacheControlFeature;
import org.jboss.resteasy.plugins.interceptors.encoding.AcceptEncodingGZIPFilter;
import org.jboss.resteasy.plugins.interceptors.encoding.AcceptEncodingGZIPInterceptor;
import org.jboss.resteasy.plugins.interceptors.encoding.ClientContentEncodingAnnotationFeature;
import org.jboss.resteasy.plugins.interceptors.encoding.GZIPDecodingInterceptor;
import org.jboss.resteasy.plugins.interceptors.encoding.GZIPEncodingInterceptor;
import org.jboss.resteasy.plugins.interceptors.encoding.ServerContentEncodingAnnotationFeature;
import org.jboss.resteasy.plugins.providers.ByteArrayProvider;
import org.jboss.resteasy.plugins.providers.DataSourceProvider;
import org.jboss.resteasy.plugins.providers.DefaultTextPlain;
import org.jboss.resteasy.plugins.providers.DocumentProvider;
import org.jboss.resteasy.plugins.providers.FileProvider;
import org.jboss.resteasy.plugins.providers.FileRangeWriter;
import org.jboss.resteasy.plugins.providers.FormUrlEncodedProvider;
import org.jboss.resteasy.plugins.providers.IIOImageProvider;
import org.jboss.resteasy.plugins.providers.InputStreamProvider;
import org.jboss.resteasy.plugins.providers.JaxrsFormProvider;
import org.jboss.resteasy.plugins.providers.ReaderProvider;
import org.jboss.resteasy.plugins.providers.SerializableProvider;
import org.jboss.resteasy.plugins.providers.SourceProvider;
import org.jboss.resteasy.plugins.providers.StreamingOutputProvider;
import org.jboss.resteasy.plugins.providers.StringTextStar;
import org.jboss.resteasy.plugins.providers.jackson.ResteasyJacksonProvider;
import org.jboss.resteasy.plugins.providers.jaxb.CollectionProvider;
import org.jboss.resteasy.plugins.providers.jaxb.JAXBElementProvider;
import org.jboss.resteasy.plugins.providers.jaxb.JAXBXmlRootElementProvider;
import org.jboss.resteasy.plugins.providers.jaxb.JAXBXmlSeeAlsoProvider;
import org.jboss.resteasy.plugins.providers.jaxb.JAXBXmlTypeProvider;
import org.jboss.resteasy.plugins.providers.jaxb.MapProvider;
import org.jboss.resteasy.plugins.providers.jaxb.XmlJAXBContextFinder;
import org.jenkinsci.plugins.zanata.exception.ZanataSyncException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zanata.client.commands.OptionsUtil;
import org.zanata.client.commands.PushPullOptions;
import org.zanata.client.commands.pull.PullCommand;
import org.zanata.client.commands.pull.PullOptions;
import org.zanata.client.commands.push.PushCommand;
import org.zanata.client.commands.push.PushOptions;
import org.zanata.client.config.LocaleList;
import org.zanata.client.config.ZanataConfig;
import org.zanata.rest.client.RestClientFactory;
import org.zanata.rest.dto.VersionInfo;

/**
 * @author Patrick Huang <a href="mailto:pahuang@redhat.com">pahuang@redhat.com</a>
 */
public final class PushPullOptionsUtil {
    private static final Logger log =
            LoggerFactory.getLogger(PushPullOptionsUtil.class);
    // TODO make this configurable?
    public static final int MAX_DEPTH = 10;

    // FIXME this is a quick hack to work around http://stackoverflow.com/questions/41253028/how-to-make-jenkins-plugin-aware-of-spi
    private static final Consumer<ResteasyClientBuilder>
            resteasyClientBuilderConsumer = builder -> {
        builder.register(ResteasyJacksonProvider.class)
                .register(JAXBXmlSeeAlsoProvider.class)
                .register(JAXBXmlRootElementProvider.class)
                .register(JAXBElementProvider.class)
                .register(JAXBXmlTypeProvider.class)
                .register(CollectionProvider.class)
                .register(MapProvider.class)
                .register(XmlJAXBContextFinder.class)
                .register(DataSourceProvider.class)
                .register(DocumentProvider.class)
                .register(DefaultTextPlain.class)
                .register(StringTextStar.class)
                .register(SourceProvider.class)
                .register(InputStreamProvider.class)
                .register(ReaderProvider.class)
                .register(ByteArrayProvider.class)
                .register(FormUrlEncodedProvider.class)
                .register(JaxrsFormProvider.class)
                .register(FileProvider.class)
                .register(FileRangeWriter.class)
                .register(StreamingOutputProvider.class)
                .register(IIOImageProvider.class)
                .register(SerializableProvider.class)
                .register(CacheControlFeature.class)
                .register(AcceptEncodingGZIPInterceptor.class)
                .register(AcceptEncodingGZIPFilter.class)
                .register(ClientContentEncodingAnnotationFeature.class)
                .register(GZIPDecodingInterceptor.class)
                .register(GZIPEncodingInterceptor.class)
                .register(ServerContentEncodingAnnotationFeature.class);
    };

    /**
     * You typically call this after clone the source repo and before doing a
     * push to or pull from Zanata
     *
     * @param options
     *         push or pull options
     * @param <O>
     *         PushOptions or PullOptions
     * @return the options after applying project config (zanata.xml) and modify
     * the source and trans dir (because current working directory is not the
     * same as what push and pull commands will be executed within
     */
    public static <O extends PushPullOptions> O applyProjectConfig(O options,
            File projectConfig) {
        options.setProjectConfig(projectConfig);
        // unset previous values so that we can reload them from project config
        options.setSrcDir(null);
        options.setTransDir(null);
        options.setProj(null);
        options.setProjectVersion(null);
        options.setProjectType(null);

        try {
            // here we must take it step by step due to an issue http://stackoverflow.com/questions/41253028/how-to-make-jenkins-plugin-aware-of-spi
            Optional<ZanataConfig> zanataConfig =
                    OptionsUtil.applyProjectConfigToProjectOptions(options);
            if (OptionsUtil
                    .shouldFetchLocalesFromServer(zanataConfig, options)) {
                log.debug("fetching locales from server");
                LocaleList localeMappings = OptionsUtil
                        .fetchLocalesFromServer(options,
                                makeRestClientFactory(options));
                options.setLocaleMapList(localeMappings);
            }
        } catch (JAXBException e) {
            throw new ZanataSyncException("Failed applying project config", e);
        }

        File baseDir = projectConfig.getParentFile();
        // we need to adjust src-dir and trans-dir to be relative to zanata base dir
        options.setSrcDir(
                new File(baseDir, options.getSrcDir() != null ?
                        options.getSrcDir().getPath() : "."));
        options.setTransDir(
                new File(baseDir, options.getTransDir() != null ?
                        options.getTransDir().getPath() : "."));
        // disable commandhook
        if (!options.getCommandHooks().isEmpty()) {
            throw new ZanataSyncException(
                    "Commandhook in zanata.xml is not supported", null);
        }

        return options;
    }

    private static <O extends PushPullOptions> RestClientFactory makeRestClientFactory(
            O options) {
        // FIXME the version info is not resolved properly
        return new RestClientFactory(getUri(options), options.getUsername(),
                        options.getKey(),
                        new VersionInfo("unknown", "unknown", "unknown"),
                        options.getLogHttp(), options.isDisableSSLCert(),
                        resteasyClientBuilderConsumer);
    }

    private static <O extends PushPullOptions> URI getUri(O options) {
        try {
            return options.getUrl().toURI();
        } catch (URISyntaxException e) {
            throw new ZanataSyncException("fail reading zanata base URI");
        }
    }

    /**
     * @param repoBase
     *         base path of a source repo.
     * @return absolute paths for all the project configs found under repoBase
     */
    public static Set<File> findProjectConfigs(File repoBase) {
        try {
            Stream<Path> pathStream = Files.find(repoBase.toPath(), MAX_DEPTH,
                    (path, basicFileAttributes) ->
                            basicFileAttributes.isRegularFile() &&
                                    path.toFile().getName()
                                            .equals("zanata.xml"));
            return pathStream.map(Path::toFile).collect(Collectors.toSet());
        } catch (IOException e) {
            throw new ZanataSyncException("Failed finding project config", e);
        }
    }

    public static PushCommand makePushCommand(PushOptions pushOptions) {
        RestClientFactory factory =
                makeRestClientFactory(pushOptions);
        return new PushCommand(pushOptions, factory.getCopyTransClient(),
                factory.getAsyncProcessClient(), factory);
    }

    public static PullCommand makePullCommand(PullOptions pullOptions) {
        RestClientFactory factory =
                makeRestClientFactory(pullOptions);
        return new PullCommand(pullOptions, factory);
    }
}
