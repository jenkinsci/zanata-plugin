package org.jenkinsci.plugins.zanata.zanatareposync;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;


import org.junit.Test;
import org.jvnet.hudson.test.WithoutJenkins;
import org.xml.sax.SAXException;

import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.google.common.base.Charsets;
import com.google.common.collect.Lists;

import hudson.FilePath;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.TaskListener;
import hudson.remoting.VirtualChannel;
import hudson.tasks.Builder;
import hudson.tasks.Shell;
import hudson.tools.CommandInstaller;
import hudson.tools.InstallSourceProperty;
import hudson.tools.ToolInstallation;
import hudson.tools.ToolInstaller;
import hudson.tools.ToolProperty;
import hudson.util.StreamTaskListener;
import jenkins.model.Jenkins;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

public class ZanataCLIInstallTest extends WithJenkins {


    @Test
    public void testInstallation() throws IOException, InterruptedException {
        VirtualChannel channel = Jenkins.getActiveInstance().getChannel();

        ZanataCLIInstall installation = createCLI("4.0.0");

        TaskListener listener = new StreamTaskListener(System.out,
                Charsets.UTF_8);;
        installation = installation.forNode(j.jenkins, listener);

        if (!new FilePath(channel, installation.getHome()).exists()) {
            fail(installation.getHome() + " does not exist");
        }
    }

    @Test
    public void testBasicCase() throws Exception {
        j.jenkins.setNumExecutors(0);
        j.createSlave();
        ZanataCLIInstall.DescriptorImpl cliInstallDescriptor = j.jenkins
                .getDescriptorByType(ZanataCLIInstall.DescriptorImpl.class);
        ZanataCLIInstall cli = createCLI("4.0.0");
        cliInstallDescriptor.setInstallations(cli);
        FreeStyleProject project = j.createFreeStyleProject();
        ZanataCLIInstallWrapper.SelectedCLI selectedCLI =
                new ZanataCLIInstallWrapper.SelectedCLI(cli.getName());

        ZanataCLIInstallWrapper wrapper = new ZanataCLIInstallWrapper(
                new ZanataCLIInstallWrapper.SelectedCLI[] { selectedCLI }, false);
        assertThat(wrapper.isConvertHomesToUppercase()).isFalse();
        assertThat(wrapper.getSelectedCLIs()).contains(selectedCLI);

        project.getBuildWrappersList().add(wrapper);
        Builder b = new Shell("echo");
        project.getBuildersList().add(b);
        Future<FreeStyleBuild> build = project.scheduleBuild2(0);
        j.assertBuildStatusSuccess(build);
        j.assertLogContains("[ZANATA-CLI Install] Setting zanata_cli_4_0_0_HOME", build.get());

    }

    @Test
    public void testUI() throws IOException, SAXException {
        HtmlForm form = goToGlobalConfigForm();
        List<?> button = form.getByXPath("//button[text()='Add Zanata CLI']");

        assertThat(button).hasSize(1);
    }

    public static ZanataCLIInstall createCLI(String version) throws IOException {
        List<ToolInstaller> installers = Lists.newArrayList(
                new CommandInstaller(null, "echo hello", "./"));

        List<ToolProperty<ToolInstallation>> properties = Lists.newArrayList();
        properties.add(new InstallSourceProperty(installers));

        return new ZanataCLIInstall("", version, properties);
    }

    @Test
    @WithoutJenkins
    public void canConvertVersionToName() {
        ZanataCLIInstall zanataCLIInstall =
                new ZanataCLIInstall("", "4.0.0-RC1", Lists.newArrayList());
        String name = zanataCLIInstall.getName();
        assertThat(name).isEqualTo("zanata_cli_4_0_0_RC1");
    }

}
