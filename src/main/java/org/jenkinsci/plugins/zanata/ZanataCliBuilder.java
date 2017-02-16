/*
The MIT License (MIT)

Copyright (c) 2016 Yu Shao yu.shao.gm@gmail.com

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
*/

package org.jenkinsci.plugins.zanata;
import hudson.AbortException;
import hudson.Launcher;
import hudson.Extension;
import hudson.FilePath;
import hudson.Util;
import hudson.model.Computer;
import hudson.model.Item;
import hudson.model.Job;
import hudson.model.Queue;
import hudson.model.queue.Tasks;
import hudson.util.ArgumentListBuilder;
import hudson.util.FormValidation;
import hudson.model.AbstractProject;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.Builder;
import hudson.tasks.BuildStepDescriptor;
import hudson.util.ListBoxModel;
import jenkins.model.Jenkins;
import jenkins.tasks.SimpleBuildStep;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.zanata.cli.RunAsCommand;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.QueryParameter;

import javax.servlet.ServletException;
import java.io.IOException;

import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.IdCredentials;
import com.cloudbees.plugins.credentials.common.PasswordCredentials;
import com.cloudbees.plugins.credentials.common.StandardUsernameCredentials;
import com.cloudbees.plugins.credentials.common.StandardUsernameListBoxModel;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.domains.URIRequirementBuilder;
import com.google.common.base.Strings;
import hudson.EnvVars;


public class ZanataCliBuilder extends Builder implements SimpleBuildStep {

    // FIXME projFile is not used in script template
    private final String projFile;
    private final String zanataCredentialsId;
    private final boolean syncG2zanata;
    private final boolean syncZ2git;
    private String extraPathEntries;


    // Fields in config.jelly must match the parameter names in the "DataBoundConstructor"
    @DataBoundConstructor
    public ZanataCliBuilder(String projFile, boolean syncG2zanata, boolean syncZ2git, String zanataCredentialsId) {
        this.projFile = projFile;
        this.syncG2zanata = syncG2zanata;
        this.syncZ2git = syncZ2git;
        this.zanataCredentialsId = zanataCredentialsId;
    }

    // TODO add validator for this field
    @DataBoundSetter
    public void setExtraPathEntries(String extraPathEntries) {
        this.extraPathEntries = extraPathEntries;
    }


    /**
     * We'll use this from the {@code config.jelly}.
     */

    public String getProjFile() {
        return projFile;
    }

    public boolean getSyncG2zanata() {
        return syncG2zanata;
    }

    public boolean getSyncZ2git() {
        return syncZ2git;
    }

    public String getZanataCredentialsId() {
        return zanataCredentialsId;
    }

    public String getExtraPathEntries() {
        return extraPathEntries;
    }

    @Override
    public void perform(Run<?,?> build, FilePath workspace, Launcher launcher, TaskListener listener)
            throws IOException, InterruptedException {

         String commandG2Z;
         String commandZ2G;

         listener.getLogger().println("Running Zanata Sync, project file: " + projFile);

        // TODO we should depend on credentials-binding-plugin and use credential as environment variables
        IdCredentials cred = CredentialsProvider.findCredentialById(zanataCredentialsId, IdCredentials.class, build);
        if (cred == null) {
            throw new AbortException("Zanata credential with ID [" + zanataCredentialsId + "] can not be found.");
        }
        CredentialsProvider.track(build, cred);
        StandardUsernameCredentials usernameCredentials = (StandardUsernameCredentials) cred;
        String apiKey =
                ((PasswordCredentials) usernameCredentials).getPassword()
                        .getPlainText();
        String username = usernameCredentials.getUsername();

        EnvVars envs = build.getEnvironment(listener);
        // TODO this is a hack but seems to be the only way to make the script work
        // system path will include usual /usr/bin etc so that 'find' command is available
        listener.getLogger().println("before adding system path:" + envs.get("PATH"));
        envs.override("PATH+", System.getenv("PATH"));
        listener.getLogger().println("after adding system PATH:" + envs.get("PATH"));


        // build.getEnvironment(listener) doesn't seem to get current node's environment.
        // This will work and inherit the global JAVA_HOME env
        if (build.getExecutor() != null) {
            Computer owner = build.getExecutor().getOwner();
            EnvVars envForNode = owner.buildEnvironment(listener);
            envs.putAll(envForNode);
        }

        envs.put("ZANATA_USERNAME", username);
        envs.put("ZANATA_APIKEY", apiKey);


        if (syncG2zanata) {
            commandG2Z = getDescriptor().getCommandG2Z();

            listener.getLogger().println("Git to Zanata sync is enabled, running command:");
            listener.getLogger().println(commandG2Z + "\n");

            if  (runShellCommandInBuild(commandG2Z, listener, launcher, build, workspace, envs)){
                listener.getLogger().println("Git to Zanata sync finished.\n");
            } else {
                throw new RuntimeException("Command failed:" + commandG2Z);
            }

         };


         if (syncZ2git) {
             // FIXME these two commands are coming from global configuration. If you upload the plugin and haven't gone to global config page then hit save, these two value will be null
            commandZ2G = getDescriptor().getCommandZ2G();

            listener.getLogger().println("Zanata to Git sync is enabled, running command:");
            listener.getLogger().println(commandZ2G + "\n");

            if  (runShellCommandInBuild(commandZ2G, listener, launcher, build, workspace,
                    envs)){
                listener.getLogger().println("Zanata to Git sync finished.\n");
            } else {
                throw new RuntimeException("Command failed:" + commandZ2G);
            }
         };

         /*
         This is where you 'build' the project.
         Since this is a dummy, we just say 'hello world' and call that a build.

         This also shows how you can consult the global configuration of the builder
         */
    }

    private boolean runShellCommandInBuild(String command,
            TaskListener listener, Launcher launcher, Run<?, ?> builder,
            FilePath workspace, EnvVars envs){

         try {

             String extraPathEntries = getExtraPathEntries();

             // FIXME to be safe, we should not override PATH. We should change our script template to use environment variable instead of expecting zanata-cli on PATH
             if (!Strings.isNullOrEmpty(extraPathEntries)) {
                 listener.getLogger().println("Current PATH:" + envs.get("PATH"));
                 listener.getLogger().println("Extra PATH:" + extraPathEntries);
                 // Override paths to prevent JENKINS-20560
                 envs.override("PATH+", extraPathEntries);
             }

             listener.getLogger().println("workspace: " + workspace.toURI());


             RunAsCommand runAsCommand = new RunAsCommand(envs);
             // FIXME we are referring to bash directly. Jenkins has an abstraction here (admin can configure and override shell to use). We probably need to use sh. Need to check the Jenkins code to find out.
             boolean result = runAsCommand.run(workspace, listener, launcher,
                     new ArgumentListBuilder()
                             .add("bash").add("-c")
                             .add(command));

             return result;

         } catch (IOException e) {
             listener.getLogger().println("Can't run command:" + command);
             e.printStackTrace();
             return false;
         } catch (InterruptedException e) {
             listener.getLogger().println("Can't run command - InterruptedException.");
             e.printStackTrace();
             return false;
         }

    }

    // Overridden for better type safety.
    // If your plugin doesn't really define any property on Descriptor,
    // you don't have to do this.
    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl)super.getDescriptor();
    }

    /**
     * Descriptor for {@link ZanataCliBuilder}. Used as a singleton.
     * The class is marked as public so that it can be accessed from views.
     *
     * <p>
     * See {@code src/main/resources/hudson/plugins/hello_world/ZanataCliBuilder/*.jelly}
     * for the actual HTML fragment for the configuration screen.
     */
    @Extension // This indicates to Jenkins that this is an implementation of an extension point.
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {
        public static final String defaultGitToZanataScript =
                "find . -type f -not -path \"*/target/*\" -name 'zanata.xml' \\\n" +
                "  -execdir pwd \\; \\\n" +
                "  -execdir ls \\; \\\n" +
                "  -execdir echo 'Running zanata-cli -B stats\\n' \\; \\\n" +
                "  -execdir zanata-cli -B stats --username $ZANATA_USERNAME --key $ZANATA_APIKEY \\; \\\n" +
                "  -execdir echo 'Running zanata-cli -B push\\n' \\; \\\n" +
                "  -execdir zanata-cli -B push --username $ZANATA_USERNAME --key $ZANATA_APIKEY \\; \\\n" +
                "  -execdir echo 'Running zanata-cli -B pull\\n' \\; \\\n" +
                "  -execdir zanata-cli -B pull --username $ZANATA_USERNAME --key $ZANATA_APIKEY \\;";

        public static final String defaultZanataToGitScript =
                "find . -type f -not -path \"*/target/*\" -name 'zanata.xml' \\" +
                "  -execdir echo \"=== Commiting new translation $BUILD_TAG...\\n\" \\; \\\n" +
                "  -execdir pwd \\; \\\n" +
                "  -execdir ls \\; \\\n" +
                "  -execdir echo '=== Git add...\\n' \\; \\\n" +
                "  -execdir git add . \\; \\\n" +
                "  -execdir echo '=== Git commit ...\\n' \\; \\\n" +
                "  -execdir git commit -m \"$BUILD_TAG\" \\; \\\n" +
                "  -execdir echo \"=== Finished commit preparation - $BUILD_TAG.\\n\" \\;";

        private String commandG2Z;
        private String commandZ2G;


        /**
         * In order to load the persisted global configuration, you have to
         * call load() in the constructor.
         */
        public DescriptorImpl() {
            load();
        }


        public FormValidation doCheckProjFile(@QueryParameter String value,
                                              @QueryParameter boolean syncG2zanata,
                                              @QueryParameter boolean syncZ2git)
                throws IOException, ServletException {

            if (value.length() == 0)
                return FormValidation.error("Please set a project name such as zanata.xml");

            System.out.println("Project File is : " + value);
            System.out.println(syncG2zanata);
            System.out.println(syncZ2git);

            save ();
            return FormValidation.ok();
        }

        @SuppressWarnings("unused")
        public ListBoxModel doFillZanataCredentialsIdItems(@AncestorInPath
                Job context,
                @QueryParameter String remote,
                @QueryParameter String credentialsId) {
            if (context == null || !context.hasPermission(Item.CONFIGURE)) {
                // previously it was recommended to just return an empty ListBoxModel
                // now recommended to return a model with just the current value
                return new StandardUsernameListBoxModel().includeCurrentValue(credentialsId);
            }
            // previously it was recommended to use the withXXX methods providing the credentials instances directly
            // now recommended to populate the model using the includeXXX methods which call through to
            // CredentialsProvider.listCredentials and to ensure that the current value is always present using
            // includeCurrentValue
            return new StandardUsernameListBoxModel()
                    .includeEmptyValue()
                    .includeAs(Tasks.getAuthenticationOf((Queue.Task) context), context, StandardUsernameCredentials.class,
                            URIRequirementBuilder.fromUri(remote).build())
                    .includeCurrentValue(credentialsId);
        }

        @SuppressWarnings("unused")
        public FormValidation doCheckZanataCredentialsId(@AncestorInPath AbstractProject context,
                @QueryParameter String url,
                @QueryParameter String value) {
            if (context == null && !Jenkins.getActiveInstance().hasPermission(Jenkins.ADMINISTER) ||
                    context != null && !context.hasPermission(Item.EXTENDED_READ)) {
                return FormValidation.ok();
            }

            value = Util.fixEmptyAndTrim(value);
            if (value == null) {
                return FormValidation.ok();
            }

            url = Util.fixEmptyAndTrim(url);
            if (url == null) {
                // not set, can't check
                return FormValidation.ok();
            }

            for (ListBoxModel.Option o : CredentialsProvider.listCredentials(
                    StandardUsernameCredentials.class,
                    context,
                    Tasks.getAuthenticationOf(context),
                    URIRequirementBuilder.fromUri(url).build(),
                    CredentialsMatchers
                            .instanceOf(StandardUsernamePasswordCredentials.class))) {
                if (StringUtils.equals(value, o.value)) {
                    return FormValidation.ok();
                }
            }
            // no credentials available, can't check
            return FormValidation.warning("Cannot find any credentials with id " + value);
        }


        @Override
        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            // Indicates that this builder can be used with all kinds of project types
            return true;
        }

        /**
         * This human readable name is used in the configuration screen.
         */
        @Override
        public String getDisplayName() {
            return "Zanata Sync via CLI";
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
            commandG2Z = formData.getString("commandG2Z");
            commandZ2G = formData.getString("commandZ2G");

            save();
            return super.configure(req,formData);
        }

        public String getCommandG2Z() {
            return commandG2Z;
        }
        public String getCommandZ2G() {
            return commandZ2G;
        }
    }
}

