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
import hudson.Launcher;
import hudson.Extension;
import hudson.FilePath;
import hudson.util.FormValidation;
import hudson.model.AbstractProject;
import hudson.model.Run;
import hudson.model.Build;
import hudson.model.TaskListener;
import hudson.model.*;
import hudson.tasks.Builder;
import hudson.tasks.BuildStepDescriptor;
import jenkins.tasks.SimpleBuildStep;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.QueryParameter;

import javax.servlet.ServletException;
import java.io.IOException;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.BufferedWriter;
import java.io.BufferedReader;
import java.io.File;
import java.io.Writer;
import java.io.*;
import java.util.*;
import hudson.EnvVars;
import hudson.Launcher.*;
import hudson.Proc;


public class ZanataCliBuilder extends Builder implements SimpleBuildStep {

    private final String projFile;
    private final boolean syncG2zanata;
    private final boolean syncZ2git;


    // Fields in config.jelly must match the parameter names in the "DataBoundConstructor"
    @DataBoundConstructor
    public ZanataCliBuilder(String projFile, boolean syncG2zanata, boolean syncZ2git) {
        this.projFile = projFile;
        this.syncG2zanata = syncG2zanata;
        this.syncZ2git = syncZ2git;
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

    @Override
    public void perform(Run<?,?> build, FilePath workspace, Launcher launcher, TaskListener listener) {

         String commandG2Z;
         String commandZ2G;

         listener.getLogger().println("Running Zanata Sync, project file: " + projFile);

         if (syncG2zanata) {
            commandG2Z = getDescriptor().getCommandG2Z();

            listener.getLogger().println("Git to Zanata sync is enabled, running command:");
            listener.getLogger().println(commandG2Z + "\n");

            if  (runShellCommandInBuild(commandG2Z, listener, build, workspace)){
                listener.getLogger().println("Git to Zanata sync finished.\n");
            }

         };


         if (syncZ2git) {
            commandZ2G = getDescriptor().getCommandZ2G();

            listener.getLogger().println("Zanata to Git sync is enabled, running command:");
            listener.getLogger().println(commandZ2G + "\n");

            if  (runShellCommandInBuild(commandZ2G, listener, build, workspace)){
                listener.getLogger().println("Zanata to Git sync finished.\n");
            }
         };

         /*
         This is where you 'build' the project.
         Since this is a dummy, we just say 'hello world' and call that a build.

         This also shows how you can consult the global configuration of the builder
         */
    }

    private boolean runShellCommandInBuild(String command, TaskListener listener, Run<?,?> builder, FilePath workspace){

         try {

             EnvVars jenkinsEnvs = builder.getEnvironment(listener);
             Map<String, String> sysEnvs = System.getenv();

             Map<String, String> allEnvs = new HashMap<String, String> ();
             allEnvs.putAll(sysEnvs);
             allEnvs.putAll(jenkinsEnvs);

             listener.getLogger().println("workspace: " + workspace.toURI());

             Process pg = Runtime.getRuntime().exec(new String[]{"bash","-c",command},
                                                    allEnvs.toString().split(", "),
                                                    new File(workspace.toURI()));


             try (BufferedReader in = new BufferedReader(
                                     new InputStreamReader(pg.getInputStream(),"UTF8"));) {
                 String line = null;
                 while ((line = in.readLine()) != null)
                     { System.out.println(line);
                       listener.getLogger().println(line);
                 }
                 in.close();
             } catch (IOException e) {
                 listener.getLogger().println("Can't generate output of command:" + command);
                 e.printStackTrace();
                 return false;
             }
             try (BufferedReader in = new BufferedReader(
                                 new InputStreamReader(pg.getErrorStream(),"UTF8"));) {
                 String line = null;
                 while ((line = in.readLine()) != null)
                     { System.out.println(line);
                       listener.getLogger().println(line);
                 }
                 in.close();
             } catch (IOException e) {
                 listener.getLogger().println("Can't generate error message of command:" + command);
                 e.printStackTrace();
                 return false;
             }
             pg.waitFor();
             listener.getLogger().println("Run command return:  " + Integer.toString(pg.exitValue()));

         } catch (IOException e) {
             listener.getLogger().println("Can't run command:" + command);
             e.printStackTrace();
             return false;
         } catch (InterruptedException e) {
             listener.getLogger().println("Can't run command - InterruptedException.");
             e.printStackTrace();
             return false;
         }
         return true;

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
        /**
         * To persist global configuration information,
         * simply store it in a field and call save().
         *
         * <p>
         * If you don't want fields to be persisted, use {@code transient}.
         */
        private String iniLocation;
        private String iniContents;
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
            return "Zanata Localization Sync";
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
            // To persist global configuration information,
            // set that to properties and call save().
            // useFrench = formData.getBoolean("useFrench");
            // ^Can also use req.bindJSON(this, formData);
            //  (easier when there are many fields; need set* methods for this, like setUseFrench)

            iniLocation = formData.getString("iniLocation");
            iniContents = formData.getString("iniContents");
            commandG2Z = formData.getString("commandG2Z");
            commandZ2G = formData.getString("commandZ2G");

            String iniFullPath;

            if (System.getProperty("JENKINS_HOME") != null) {
                iniFullPath = System.getProperty("JENKINS_HOME") + "/" + iniLocation;
            } else {
                if (System.getProperty("user.home") != null) {
                    iniFullPath = System.getProperty("user.home") + "/" + iniLocation;
                } else {
                    iniFullPath = "/var/lib/jenkins" + "/" + iniLocation;
                }
            }

            File file = new File(iniFullPath);

            try {
                if (!file.exists()) {
                   if (!(file.getParentFile().exists())) {
                       if (!(file.getParentFile().mkdirs())) {
                           throw new IOException("Unable to create directory " + iniFullPath );
                       }
                   }
                   if (!(file.createNewFile())) {
                      throw new IOException("Unable to create file " + iniFullPath );
                   }
                }
            } catch (IOException e) {
                System.out.println("File could not be created or opened: " + iniFullPath);
                e.printStackTrace();
            }

            if (file.exists()) {
                try (Writer fstream = new OutputStreamWriter(new FileOutputStream(iniFullPath), "UTF8");) {
                    fstream.write(iniContents);
                    fstream.flush();
                    fstream.close();
                    System.out.println("File written Succesfully: " + iniFullPath);
                } catch (IOException e) {
                    System.out.println("File write NOT Succesfully: " + iniFullPath);
                    e.printStackTrace();
                }
            }
            save();
            return super.configure(req,formData);
        }

        /**
         * This method returns true if the global configuration says we should speak French.
         *
         * The method name is bit awkward because global.jelly calls this method to determine
         * the initial state of the checkbox by the naming convention.
         */
        public String getIniLocation() {
            return iniLocation;
        }
        public String getIniContents() {
            return iniContents;
        }
        public String getCommandG2Z() {
            return commandG2Z;
        }
        public String getCommandZ2G() {
            return commandZ2G;
        }
    }
}

