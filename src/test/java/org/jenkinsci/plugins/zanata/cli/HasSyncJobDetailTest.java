package org.jenkinsci.plugins.zanata.cli;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

public class HasSyncJobDetailTest {

    @Test
    public void maskWillReturnAsIsForNullOrEmptyString() {
        assertThat(HasSyncJobDetail.mask("")).isEmpty();
        assertThat(HasSyncJobDetail.mask(null)).isNull();
    }

    @Test
    public void testMask() {
        assertThat(HasSyncJobDetail.mask("a")).isEqualTo("*");
        assertThat(HasSyncJobDetail.mask("ab")).isEqualTo("*");
        assertThat(HasSyncJobDetail.mask("abc")).isEqualTo("a*c");
        assertThat(HasSyncJobDetail.mask("abcd")).isEqualTo("a*d");
        assertThat(HasSyncJobDetail.mask("abcde")).isEqualTo("ab*e");
        assertThat(HasSyncJobDetail.mask("abcdef")).isEqualTo("ab*f");
        assertThat(HasSyncJobDetail.mask("abcdefg")).isEqualTo("ab*g");
        assertThat(HasSyncJobDetail.mask("abcdefgh")).isEqualTo("ab*h");
    }

}
