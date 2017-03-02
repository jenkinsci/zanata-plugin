package org.jenkinsci.plugins.zanata.git;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.rules.TemporaryFolder;
import com.google.common.base.Charsets;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.common.io.Files;

/**
 * Use as test rule to set up a temporary git remote on filesystem. CAUTION: Use
 * as test rule NOT class rule. Otherwise some assumption may not hold between
 * tests.
 *
 * @author Patrick Huang <a href="mailto:pahuang@redhat.com">pahuang@redhat.com</a>
 */
public class RemoteGitRepoRule extends TemporaryFolder {
    private File remoteRepo;

    @Override
    protected void before() throws Throwable {
        super.before();
        remoteRepo = newFolder();
        initGitRepo();
    }

    private void initGitRepo() {
        addFile("messages.properties", "greeting=hello, world");
        try {
            Git.init().setDirectory(remoteRepo).call();
        } catch (GitAPIException e) {
            throw new RuntimeException(e);
        }
        commitFiles("init commit");
    }

    public void addFile(String fileName, String content) {
        File file = new File(remoteRepo, fileName);
        try (BufferedWriter writer =
                Files.newWriter(file, Charsets.UTF_8)) {
            writer.write(content);
            writer.newLine();
            writer.flush();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void commitFiles(String message) {
        try {
            Git git = Git.open(remoteRepo);
            git.add().addFilepattern(".").call();
            git.commit().setCommitter("JUnit", "junit@example.com")
                    .setMessage(message).call();
        } catch (GitAPIException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    public List<String> getCommitMessages() {
        try {
            Git git = Git.open(remoteRepo);
            Iterable<RevCommit> commits = git.log().setMaxCount(10).call();
            return ImmutableList.copyOf(commits).stream().map(
                    RevCommit::getFullMessage).collect(Collectors.toList());
        } catch (IOException | GitAPIException e) {
            throw new RuntimeException(e);
        }
    }

    public Path getRemoteRepoPath() {
        return remoteRepo.toPath();
    }
}
