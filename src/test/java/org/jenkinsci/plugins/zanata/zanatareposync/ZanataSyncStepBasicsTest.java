package org.jenkinsci.plugins.zanata.zanatareposync;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Before;
import org.junit.Test;
import org.jvnet.hudson.test.WithoutJenkins;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import hudson.model.AbstractProject;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;

/**
 * Use this test class for anything NOT requiring a jenkins instance (JUnit
 * rule).
 *
 * @see ZanataSyncStepTest
 *
 * @author Patrick Huang
 *         <a href="mailto:pahuang@redhat.com">pahuang@redhat.com</a>
 */
public class ZanataSyncStepBasicsTest {
    @Mock
    private AbstractProject mockProject;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    @WithoutJenkins
    public void descriptorBasics() {
        ZanataSyncStep.DescriptorImpl descriptor =
                new ZanataSyncStep.DescriptorImpl();
        assertThat(descriptor.getDisplayName()).isEqualTo("Zanata Sync");
        assertThat(descriptor.isApplicable(mockProject.getClass())).isTrue();

    }

    @Test
    @WithoutJenkins
    public void zanataURLCanBeNullOrEmpty() throws Exception {
        ZanataSyncStep.DescriptorImpl descriptor =
                new ZanataSyncStep.DescriptorImpl();
        assertThat(descriptor.doCheckZanataURL(null).kind).isEqualTo(
                FormValidation.Kind.OK);
        assertThat(descriptor.doCheckZanataURL("").kind).isEqualTo(FormValidation.Kind.OK);
        assertThat(descriptor.doCheckZanataURL("  ").kind).isEqualTo(FormValidation.Kind.OK);
    }

    @Test
    @WithoutJenkins
    public void invalidURLWillGetErrorForZanataURLField() throws Exception {
        ZanataSyncStep.DescriptorImpl descriptor =
                new ZanataSyncStep.DescriptorImpl();
        assertThat(descriptor.doCheckZanataURL("a").kind).isEqualTo(FormValidation.Kind.ERROR);
        assertThat(descriptor.doCheckZanataURL("a.b").kind).isEqualTo(FormValidation.Kind.ERROR);
        assertThat(descriptor.doCheckZanataURL("http//a").kind).isEqualTo(FormValidation.Kind.ERROR);
    }

    @Test
    @WithoutJenkins
    public void validURLWillGetOKForZanataURLField() throws Exception {
        ZanataSyncStep.DescriptorImpl descriptor =
                new ZanataSyncStep.DescriptorImpl();
        assertThat(descriptor.doCheckZanataURL("http://zanata.org").kind).isEqualTo(FormValidation.Kind.OK);
    }

    @Test
    @WithoutJenkins
    public void testPushTypeOptions() {
        ZanataSyncStep.DescriptorImpl descriptor =
                new ZanataSyncStep.DescriptorImpl();
        ListBoxModel options = descriptor.doFillSyncOptionItems(null);
        assertThat(options).hasSize(3);
        assertThat(options.stream().map(option -> option.value))
                .contains("source", "trans", "both");
    }
}
