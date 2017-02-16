package org.jenkinsci.plugins.zanata.cli;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.logging.Level;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Proc;
import hudson.Util;
import hudson.model.AbstractProject;
import hudson.model.Descriptor;
import hudson.model.Node;
import hudson.model.Run;
import hudson.model.StreamBuildListener;
import hudson.model.TaskListener;
import hudson.remoting.ChannelClosedException;
import hudson.tasks.Messages;
import hudson.tasks.Publisher;
import hudson.tasks.Shell;
import hudson.util.ArgumentListBuilder;
import hudson.util.DescribableList;

/**
 * TODO not used. But we can use it to invoke zanata cli programmatically.
 * @author Patrick Huang <a href="mailto:pahuang@redhat.com">pahuang@redhat.com</a>
 */
public class RunAsCommand {
    private static final Logger log =
            LoggerFactory.getLogger(RunAsCommand.class);
    private final EnvVars envs;

    public RunAsCommand(EnvVars envs) {
        this.envs = envs;
    }


    public boolean run(FilePath workspace,
            TaskListener listener, Launcher launcher, ArgumentListBuilder args)
            throws InterruptedException, IOException {

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {


            log.debug("Environment variables: {}", envs.entrySet());
            log.debug("Command line: {}", args.toStringWithQuote());
            listener.getLogger().println("Env:" + envs.entrySet());
            listener.getLogger().println("Command to run:" + args.toStringWithQuote());

            StreamBuildListener streamBuildListener = new StreamBuildListener(baos);

            final Proc child = launcher
                    .launch()
                    .cmds(args).envs(envs).stdout(streamBuildListener)
                    .pwd(workspace)
                    .start();

            try {
                while (child.isAlive()) {
                    listener.getLogger().print(baos.toString("UTF-8"));
                    baos.reset();

                    Thread.sleep(5);
                }
            } catch (InterruptedException intEx) {
                child.kill();
                listener.getLogger().println("Aborted by User. Terminated");
                throw new InterruptedException("User Aborted");
            }

            baos.flush();
            listener.getLogger().print(baos.toString("UTF-8"));
            listener.getLogger().flush();
            return child.join() == 0;
        } catch (Exception e) {
            log.error("failed in error", e);
            throw new RuntimeException(e);
        }
    }
}
