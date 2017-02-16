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
import java.util.Locale;
import java.util.Map;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import hudson.EnvVars;
import hudson.Extension;
import hudson.Launcher;
import hudson.Proc;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Computer;
import hudson.model.Descriptor;
import hudson.model.Node;
import hudson.model.Run.RunnerAbortedException;
import hudson.tasks.BuildWrapper;
import hudson.tasks.BuildWrapperDescriptor;
import jenkins.model.Jenkins;

/**
 * Installs Zanata CLI selected by the user. Exports Zanata CLI home directory as variable.
 *
 *
 */
public class ZanataCLIInstallWrapper extends BuildWrapper {

    public static class SelectedCLI {
        private final String name;

        @DataBoundConstructor
        public SelectedCLI(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        @Override
        public String toString() {
            return name;
        }

        @CheckForNull
        public ZanataCLIInstall toZanataCLIInstall() {
            return ((ZanataCLIInstall.DescriptorImpl) Jenkins.getActiveInstance().getDescriptor(ZanataCLIInstall.class)).byName(name);
        }

        @Nonnull
        public ZanataCLIInstall toCLIValidated() {
            ZanataCLIInstall tool = toZanataCLIInstall();
            if (tool == null) {
                throw new RuntimeException("Can not find Zanata CLI. Has it been deleted in global configuration?");
            }
            return tool;
        }
    }
    private String zanataCLIInstall;
    private @Nonnull SelectedCLI[] selectedCLIs = new SelectedCLI[0];
    private final boolean convertHomesToUppercase;

    @DataBoundConstructor
    public ZanataCLIInstallWrapper(SelectedCLI[] selectedCLIs, boolean convertHomesToUppercase) {
        this.selectedCLIs = (selectedCLIs != null) ?
                selectedCLIs : new SelectedCLI[0];
        this.convertHomesToUppercase = convertHomesToUppercase;
    }

    public boolean isConvertHomesToUppercase() {
        return convertHomesToUppercase;
    }

    public @Nonnull SelectedCLI[] getSelectedCLIs() {
        return selectedCLIs.clone();
    }

    @DataBoundSetter
    public void setZanataCLIInstall(String zanataCLIInstall) {
        this.zanataCLIInstall = zanataCLIInstall;
    }

    @Override
    public Environment setUp(AbstractBuild build, Launcher launcher,
            BuildListener listener) throws IOException, InterruptedException {

//        final EnvVars buildEnv = build.getEnvironment(listener);
        final Node node = build.getBuiltOn();

        return new Environment() {
            @Override
            public void buildEnvVars(Map<String, String> env) {

                for (SelectedCLI selectedTool : selectedCLIs) {
                    ZanataCLIInstall tool = selectedTool.toZanataCLIInstall();
                    if (tool != null) {
                        logMessage(listener, "version " + tool.getVersion() + " will be installed " + node.getDisplayName());
                    }
                }
            }
        };
    }

    /**
     * The heart of the beast. Installs selected zanata CLIs and exports their
     * HOMEs as environment variables.
     * @return A decorated launcher
     */
    @Override
    public Launcher decorateLauncher(AbstractBuild build, final Launcher launcher,
            BuildListener listener) throws IOException, InterruptedException,
            RunnerAbortedException {

//        EnvVars buildEnv = build.getEnvironment(listener);
        final EnvVars homes = new EnvVars();

        // Each tool can export zero or many directories to the PATH
        final Node node =  Computer.currentComputer().getNode();
        if (node == null) {
            throw new RuntimeException("Cannot install tools on the deleted node");
        }

        for (SelectedCLI selectedCLIName : selectedCLIs) {
            ZanataCLIInstall tool = selectedCLIName.toCLIValidated();
            logMessage(listener, "Starting installation");

            // This installs the tool if necessary
            ZanataCLIInstall installed = tool
                    .forNode(node, listener);


            logMessage(listener, "Tool is installed at "+ installed.getHome());
            String homeDirVarName = (convertHomesToUppercase ? installed.getName().toUpperCase(Locale.ENGLISH) : installed.getName()) +"_HOME";
            logMessage(listener,
                    "Setting " + homeDirVarName + "=" + installed.getHome());
            homes.put(homeDirVarName, installed.getHome());
        }

        return new Launcher.DecoratedLauncher(launcher) {
            @Override
            public Proc launch(ProcStarter starter) throws IOException {
                EnvVars vars;
                try { // Dirty hack, which allows to avoid NPEs in Launcher::envs()
                    vars = toEnvVars(starter.envs());
                } catch (NullPointerException npe) {
                    vars = new EnvVars();
                } catch (InterruptedException x) {
                    throw new IOException(x);
                }

                // Inject paths
//                final String injectedPaths = paths.toListString();
//                if (injectedPaths != null) {
//                    vars.override("PATH+", injectedPaths);
//                }

                // Inject additional variables
                vars.putAll(homes);


                // Override paths to prevent JENKINS-20560
                if (vars.containsKey("PATH")) {
                    final String overallPaths=vars.get("PATH");
                    vars.remove("PATH");
                    vars.put("PATH+", overallPaths);
                }

                return getInner().launch(starter.envs(vars));
            }

            private EnvVars toEnvVars(String[] envs) throws IOException, InterruptedException {
                Computer computer = node.toComputer();
                EnvVars vars = computer != null ? computer.getEnvironment() : new EnvVars();
                for (String line : envs) {
                    vars.addLine(line);
                }
                return vars;
            }
        };
    }

    private static void logMessage(BuildListener listener, String message) {
        listener.getLogger().println("[ZANATA-CLI Install] " + message);
    }

    @Override
    public Descriptor<BuildWrapper> getDescriptor() {
        return DESCRIPTOR;
    }


    @Extension
    public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();

    public static final class DescriptorImpl extends BuildWrapperDescriptor {

        public DescriptorImpl() {
            super(ZanataCLIInstallWrapper.class);
        }

        @Override
        public String getDisplayName() {
            return "Install Zanata CLI";
        }

        @Override
        public boolean isApplicable(AbstractProject<?, ?> item) {
            return true;
        }

        public ZanataCLIInstall[] getInstallations() {
            return Jenkins.getActiveInstance().getDescriptorByType(ZanataCLIInstall.DescriptorImpl.class).getInstallations();
        }
    }
}

