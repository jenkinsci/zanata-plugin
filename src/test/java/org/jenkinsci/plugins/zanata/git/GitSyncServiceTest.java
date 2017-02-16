package org.jenkinsci.plugins.zanata.git;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.jenkinsci.plugins.gitclient.Git;
import org.jenkinsci.plugins.zanata.SyncJobDetail;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Charsets;
import com.google.common.collect.Lists;

import hudson.EnvVars;
import hudson.model.TaskListener;

public class GitSyncServiceTest {
    private static final Logger log =
            LoggerFactory.getLogger(GitSyncServiceTest.class);
    @Rule
    public RemoteGitRepoRule gitRepoRule = new RemoteGitRepoRule();

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private GitSyncService syncService;
    @Mock
    private TaskListener taskListener;
    private String zanataUsername = "pahuang";

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        // set up a repo on local file system and use it as a remote to test

        syncService = new GitSyncService(createJobDetail(),
                Git.with(taskListener, new EnvVars()));
    }

    private SyncJobDetail createJobDetail() {
        return SyncJobDetail.Builder.builder().setZanataUsername(zanataUsername)
                .build();

    }

    @Test
    public void testCommit() {
        Path gitRepo = gitRepoRule.getRemoteRepoPath();
        gitRepoRule.addFile("newFile", "this should be committed");
        syncService.syncTranslationToRepo(gitRepo);

        assertThat(gitRepoRule.getCommitMessages())
                .contains("Zanata Sync job triggered by " + zanataUsername);
    }

    @Test
    public void willSkipIfItIsNotGitRepo() throws Exception {
        File folder = temporaryFolder.newFolder();
        Files.write(Paths.get(folder.getAbsolutePath(), "newFile"),
                Lists.newArrayList("text"), Charsets.UTF_8);
        syncService.syncTranslationToRepo(folder.toPath());

        // no git exception thrown
    }
}
